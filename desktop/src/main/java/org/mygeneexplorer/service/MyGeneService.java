package org.mygeneexplorer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Client des API publiques BioThings.
 *
 * <ul>
 *   <li>MyGene.info : informations sur les gènes (symbole, nom, résumé).</li>
 *   <li>MyVariant.info : variants ClinVar. MyGene.info n'expose pas ClinVar,
 *       ces données proviennent donc de son service jumeau.</li>
 * </ul>
 *
 * <p>Les méthodes {@code create...Task} renvoient des {@link Task} à exécuter dans un
 * thread d'arrière-plan afin de ne pas bloquer l'interface JavaFX. Les méthodes
 * {@code parse...} n'ont pas d'effet de bord et peuvent être appelées depuis
 * {@code setOnSucceeded}, qui s'exécute sur le thread JavaFX.
 */
public class MyGeneService {

    private static final String MYGENE_URL = "https://mygene.info/v3";
    private static final String MYVARIANT_URL = "https://myvariant.info/v1";

    private static final String GENE_FIELDS = "symbol,name,entrezgene,summary";
    private static final String CLINVAR_FIELDS = String.join(",",
            "clinvar.variant_id",
            "clinvar.rcv.clinical_significance",
            "clinvar.rcv.origin");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Crée un service avec un {@link HttpClient} configuré par défaut. */
    public MyGeneService() {
        this(HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build());
    }

    /**
     * Crée un service avec un {@link HttpClient} fourni (utile pour les tests).
     *
     * @param httpClient client HTTP à utiliser pour toutes les requêtes
     */
    public MyGeneService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    // ---------------------------------------------------------------------
    // Requêtes (exécutées en arrière-plan)
    // ---------------------------------------------------------------------

    /**
     * Crée une tâche qui recherche un gène humain par symbole sur MyGene.info.
     *
     * @param symbol symbole du gène, par exemple {@code "BRCA1"}
     * @return une tâche dont la valeur est la réponse JSON brute
     * @throws IllegalArgumentException si le symbole est vide
     */
    public Task<String> createGeneQueryTask(String symbol) {
        String url = MYGENE_URL + "/query?" + queryString(Map.of(
                "q", "symbol:" + normalize(symbol),
                "species", "human",
                "fields", GENE_FIELDS,
                "size", "1"));
        return createGetTask(url);
    }

    /**
     * Crée une tâche qui récupère les variants ClinVar d'un gène sur MyVariant.info.
     *
     * @param symbol symbole du gène, par exemple {@code "BRCA1"}
     * @param size   nombre maximal de variants à récupérer
     * @return une tâche dont la valeur est la réponse JSON brute
     * @throws IllegalArgumentException si le symbole est vide
     */
    public Task<String> createClinvarQueryTask(String symbol, int size) {
        String url = MYVARIANT_URL + "/query?" + queryString(Map.of(
                "q", "clinvar.gene.symbol:" + normalize(symbol),
                "fields", CLINVAR_FIELDS,
                "size", String.valueOf(size)));
        return createGetTask(url);
    }

    // ---------------------------------------------------------------------
    // Analyse des réponses JSON
    // ---------------------------------------------------------------------

    /**
     * Extrait le premier gène d'une réponse MyGene.info.
     *
     * @param json réponse renvoyée par {@link #createGeneQueryTask(String)}
     * @return le gène trouvé, ou {@link Optional#empty()} si aucun gène ne correspond
     * @throws IOException si le JSON est invalide
     */
    public Optional<GeneInfo> parseGene(String json) throws IOException {
        JsonNode hit = mapper.readTree(json).path("hits").path(0);
        if (hit.isMissingNode()) {
            return Optional.empty();
        }

        return Optional.of(new GeneInfo(
                textOrNull(hit.get("_id")),
                textOrNull(hit.get("symbol")),
                textOrNull(hit.get("name")),
                longOrNull(hit.get("entrezgene")),
                textOrNull(hit.get("summary"))));
    }

    /**
     * Extrait la liste des variants ClinVar d'une réponse MyVariant.info.
     *
     * <p>Selon le variant, les champs {@code clinvar}, {@code rcv} et {@code origin}
     * sont soit une valeur unique, soit un tableau : les deux formes sont gérées.
     * Les significations cliniques et origines des différentes soumissions (RCV)
     * d'un même variant sont regroupées sans doublons.
     *
     * @param json réponse renvoyée par {@link #createClinvarQueryTask(String, int)}
     * @return la liste des variants, vide si aucun n'a été trouvé
     * @throws IOException si le JSON est invalide
     */
    public List<ClinvarVariant> parseClinvarVariants(String json) throws IOException {
        List<ClinvarVariant> variants = new ArrayList<>();

        for (JsonNode hit : mapper.readTree(json).path("hits")) {
            String hgvs = textOrNull(hit.get("_id"));

            for (JsonNode clinvar : asList(hit.get("clinvar"))) {
                Set<String> significances = new LinkedHashSet<>();
                Set<String> origins = new LinkedHashSet<>();

                for (JsonNode rcv : asList(clinvar.get("rcv"))) {
                    addText(significances, rcv.get("clinical_significance"));
                    for (JsonNode origin : asList(rcv.get("origin"))) {
                        addText(origins, origin);
                    }
                }

                variants.add(new ClinvarVariant(
                        hgvs,
                        longOrNull(clinvar.get("variant_id")),
                        List.copyOf(significances),
                        List.copyOf(origins)));
            }
        }

        return variants;
    }

    /**
     * Lit le nombre total de résultats disponibles côté serveur, qui peut dépasser
     * le nombre de variants effectivement récupérés.
     *
     * @param json réponse d'une requête MyGene.info ou MyVariant.info
     * @return le nombre total de résultats, ou 0 s'il est absent
     * @throws IOException si le JSON est invalide
     */
    public long parseTotal(String json) throws IOException {
        return mapper.readTree(json).path("total").asLong(0);
    }

    // ---------------------------------------------------------------------
    // Utilitaires internes
    // ---------------------------------------------------------------------

    /**
     * Crée une tâche JavaFX qui exécute un GET et renvoie le corps de la réponse.
     * L'annulation de la tâche annule aussi la requête HTTP en cours.
     */
    private Task<String> createGetTask(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .GET()
                .build();
        String host = request.uri().getHost();

        return new Task<>() {
            // Écrit par le thread de la tâche, lu par le thread JavaFX dans cancelled().
            private volatile CompletableFuture<HttpResponse<String>> future;

            @Override
            protected String call() throws Exception {
                updateMessage("Requête vers " + host + "…");
                future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString());

                HttpResponse<String> response;
                try {
                    response = future.get();
                } catch (ExecutionException e) {
                    // Remonte l'erreur réseau d'origine plutôt que l'enveloppe.
                    Throwable cause = e.getCause();
                    throw cause instanceof Exception ex ? ex : e;
                }

                int status = response.statusCode();
                if (status < 200 || status >= 300) {
                    throw new IOException("Erreur HTTP " + status + " (" + host + ")");
                }

                updateMessage("Réponse reçue");
                return response.body();
            }

            @Override
            protected void cancelled() {
                if (future != null) {
                    future.cancel(true);
                }
            }
        };
    }

    /** Valide et normalise un symbole de gène (sans espaces, en majuscules). */
    private static String normalize(String symbol) {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("Le symbole du gène est obligatoire.");
        }
        return symbol.trim().toUpperCase();
    }

    /** Construit une chaîne de requête URL encodée en UTF-8. */
    private static String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** Renvoie le nœud sous forme de liste, qu'il soit absent, unique ou un tableau. */
    private static List<JsonNode> asList(JsonNode node) {
        if (node == null || node.isNull() || node.isMissingNode()) {
            return List.of();
        }
        if (!node.isArray()) {
            return List.of(node);
        }

        List<JsonNode> list = new ArrayList<>();
        node.forEach(list::add);
        return list;
    }

    /** Ajoute le texte du nœud à l'ensemble s'il n'est pas vide. */
    private static void addText(Set<String> target, JsonNode node) {
        String text = textOrNull(node);
        if (text != null && !text.isBlank()) {
            target.add(text);
        }
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    private static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }
}
