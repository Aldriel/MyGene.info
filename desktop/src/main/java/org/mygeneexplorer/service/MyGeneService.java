package org.mygeneexplorer.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.concurrent.Task;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.service.ApiException.Reason;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Client for the public BioThings APIs.
 *
 * <ul>
 *   <li>MyGene.info: gene information (symbol, name, summary).</li>
 *   <li>MyVariant.info: ClinVar variants. MyGene.info does not expose ClinVar, so this data
 *       comes from its sibling service.</li>
 * </ul>
 *
 * <p>The {@code find...} methods are blocking; the {@code create...Task} methods wrap them in a
 * {@link Task} to run on a background thread so the JavaFX UI is never blocked. Every network
 * or server failure is reported as an {@link ApiException} whose message can be shown to the
 * user, and an invalid symbol as an {@link InvalidSymbolException}.
 */
public class MyGeneService {

    private static final URI MYGENE_URL = URI.create("https://mygene.info/v3");
    private static final URI MYVARIANT_URL = URI.create("https://myvariant.info/v1");

    private static final String GENE_FIELDS =
            "symbol,name,entrezgene,summary,type_of_gene,map_location,alias";

    /** Maximum number of variants MyVariant.info returns in a single query. */
    public static final int MAX_CLINVAR_SIZE = 1000;
    private static final String CLINVAR_FIELDS = String.join(",",
            "clinvar.variant_id",
            "clinvar.rcv.clinical_significance",
            "clinvar.rcv.origin");

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);

    private final HttpClient httpClient;
    private final URI mygeneUrl;
    private final URI myvariantUrl;
    private final Duration requestTimeout;
    private final ObjectMapper mapper = new ObjectMapper();

    /** Creates a service targeting the public APIs with a default {@link HttpClient}. */
    public MyGeneService() {
        this(HttpClient.newBuilder()
                        .connectTimeout(CONNECT_TIMEOUT)
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                MYGENE_URL, MYVARIANT_URL, REQUEST_TIMEOUT);
    }

    /**
     * Creates a service with custom endpoints, used by tests to target a local server.
     *
     * @param httpClient     HTTP client used for every request
     * @param mygeneUrl      base URL of MyGene.info
     * @param myvariantUrl   base URL of MyVariant.info
     * @param requestTimeout maximum time to wait for a response
     */
    MyGeneService(HttpClient httpClient, URI mygeneUrl, URI myvariantUrl,
                  Duration requestTimeout) {
        this.httpClient = httpClient;
        this.mygeneUrl = mygeneUrl;
        this.myvariantUrl = myvariantUrl;
        this.requestTimeout = requestTimeout;
    }

    // ---------------------------------------------------------------------
    // Background tasks
    // ---------------------------------------------------------------------

    /**
     * Creates a task that looks up a human gene by symbol.
     *
     * @param symbol gene symbol, e.g. {@code "BRCA1"}
     * @return a task whose value is the gene, or empty if none matches
     * @throws InvalidSymbolException if the symbol is malformed (thrown immediately)
     */
    public Task<Optional<GeneInfo>> createGeneTask(String symbol) {
        String normalized = GeneSymbols.normalize(symbol);
        return task(() -> findGene(normalized));
    }

    /**
     * Creates a task that fetches the ClinVar variants of a gene.
     *
     * @param symbol gene symbol, e.g. {@code "BRCA1"}
     * @param size   maximum number of variants to fetch
     * @return a task whose value is the variants found
     * @throws InvalidSymbolException if the symbol is malformed (thrown immediately)
     */
    public Task<ClinvarResult> createClinvarTask(String symbol, int size) {
        String normalized = GeneSymbols.normalize(symbol);
        checkSize(size);
        return task(() -> findClinvarVariants(normalized, size));
    }

    // ---------------------------------------------------------------------
    // Blocking requests
    // ---------------------------------------------------------------------

    /**
     * Looks up a human gene by symbol on MyGene.info.
     *
     * @param symbol gene symbol, e.g. {@code "BRCA1"}
     * @return the gene, or {@link Optional#empty()} if none matches
     * @throws InvalidSymbolException if the symbol is malformed
     * @throws ApiException           if the request fails
     * @throws InterruptedException   if the thread is interrupted (cancellation)
     */
    public Optional<GeneInfo> findGene(String symbol) throws IOException, InterruptedException {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "symbol:" + GeneSymbols.normalize(symbol));
        params.put("species", "human");
        params.put("fields", GENE_FIELDS);
        params.put("size", "1");
        return parseGene(get(mygeneUrl, params));
    }

    /**
     * Fetches the ClinVar variants of a gene on MyVariant.info.
     *
     * @param symbol gene symbol, e.g. {@code "BRCA1"}
     * @param size   maximum number of variants to fetch, between 1 and {@link #MAX_CLINVAR_SIZE}
     * @return the variants found, with the total available on the server
     * @throws InvalidSymbolException   if the symbol is malformed
     * @throws IllegalArgumentException if the size is out of range
     * @throws ApiException             if the request fails
     * @throws InterruptedException     if the thread is interrupted (cancellation)
     */
    public ClinvarResult findClinvarVariants(String symbol, int size)
            throws IOException, InterruptedException {
        checkSize(size);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("q", "clinvar.gene.symbol:" + GeneSymbols.normalize(symbol));
        params.put("fields", CLINVAR_FIELDS);
        params.put("size", String.valueOf(size));
        JsonNode root = get(myvariantUrl, params);
        return new ClinvarResult(parseTotal(root), parseClinvarVariants(root));
    }

    // ---------------------------------------------------------------------
    // JSON parsing
    // ---------------------------------------------------------------------

    /**
     * Extracts the first gene of a MyGene.info response.
     *
     * @param json body of a MyGene.info query response
     * @return the gene, or {@link Optional#empty()} if none matches
     * @throws IOException if the JSON is invalid
     */
    public Optional<GeneInfo> parseGene(String json) throws IOException {
        return parseGene(mapper.readTree(json));
    }

    /**
     * Extracts the first gene of a parsed MyGene.info response.
     *
     * @param root parsed response
     * @return the gene, or {@link Optional#empty()} if none matches
     */
    private static Optional<GeneInfo> parseGene(JsonNode root) {
        JsonNode hit = root.path("hits").path(0);
        if (hit.isMissingNode()) {
            return Optional.empty();
        }

        Set<String> aliases = new LinkedHashSet<>();
        asList(hit.get("alias")).forEach(alias -> addText(aliases, alias));

        return Optional.of(new GeneInfo(
                textOrNull(hit.get("_id")),
                textOrNull(hit.get("symbol")),
                textOrNull(hit.get("name")),
                longOrNull(hit.get("entrezgene")),
                textOrNull(hit.get("summary")),
                textOrNull(hit.get("type_of_gene")),
                textOrNull(hit.get("map_location")),
                List.copyOf(aliases)));
    }

    /**
     * Extracts the ClinVar variants of a MyVariant.info response.
     *
     * <p>Depending on the variant, the {@code clinvar}, {@code rcv} and {@code origin} fields
     * are either a single value or an array: both forms are handled. The clinical
     * significances and origins of the submissions (RCV) of a variant are merged without
     * duplicates.
     *
     * @param json body of a MyVariant.info query response
     * @return the variants, empty if none was found
     * @throws IOException if the JSON is invalid
     */
    public List<ClinvarVariant> parseClinvarVariants(String json) throws IOException {
        return parseClinvarVariants(mapper.readTree(json));
    }

    /**
     * Extracts the ClinVar variants of a parsed MyVariant.info response.
     *
     * @param root parsed response
     * @return the variants, empty if none was found
     */
    private static List<ClinvarVariant> parseClinvarVariants(JsonNode root) {
        List<ClinvarVariant> variants = new ArrayList<>();

        for (JsonNode hit : root.path("hits")) {
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
     * Reads the total number of results available on the server, which may exceed the number
     * of results actually returned.
     *
     * @param json body of a MyGene.info or MyVariant.info query response
     * @return the total number of results, or 0 if absent
     * @throws IOException if the JSON is invalid
     */
    public long parseTotal(String json) throws IOException {
        return parseTotal(mapper.readTree(json));
    }

    /**
     * Reads the total number of results of a parsed response.
     *
     * @param root parsed response
     * @return the total number of results, or 0 if absent
     */
    private static long parseTotal(JsonNode root) {
        return root.path("total").asLong(0);
    }

    // ---------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------

    /**
     * Sends a GET request to {@code baseUrl/query} and returns the parsed JSON body,
     * translating every failure into an {@link ApiException}. Interrupting the calling thread
     * (e.g. when the task is cancelled) aborts the request.
     *
     * @param baseUrl base URL of the API
     * @param params  query parameters, not yet encoded
     * @return the parsed response body
     * @throws ApiException         if the call fails or the response is not valid JSON
     * @throws InterruptedException if the calling thread is interrupted
     */
    private JsonNode get(URI baseUrl, Map<String, String> params)
            throws ApiException, InterruptedException {
        URI uri = URI.create(baseUrl + "/query?" + queryString(params));
        String host = hostOf(uri);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(requestTimeout)
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (HttpTimeoutException e) {
            throw new ApiException(Reason.TIMEOUT, host, 0, e);
        } catch (IOException e) {
            throw new ApiException(Reason.UNREACHABLE, host, 0, e);
        }

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw ApiException.forStatus(status, host);
        }

        try {
            return mapper.readTree(response.body());
        } catch (JsonProcessingException e) {
            throw new ApiException(Reason.INVALID_RESPONSE, host, status, e);
        }
    }

    /**
     * Checks the number of variants requested.
     *
     * @param size requested number of variants
     * @throws IllegalArgumentException if it is outside [1, {@value #MAX_CLINVAR_SIZE}]
     */
    private static void checkSize(int size) {
        if (size < 1 || size > MAX_CLINVAR_SIZE) {
            throw new IllegalArgumentException(
                    "size must be between 1 and " + MAX_CLINVAR_SIZE + ": " + size);
        }
    }

    /**
     * Wraps blocking work in a JavaFX task.
     *
     * @param work work to run in the background
     * @param <T>  type of the result
     * @return the task, not started
     */
    private static <T> Task<T> task(Callable<T> work) {
        return new Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
    }

    /**
     * Returns the host of a URI for error messages, with its port when not the default.
     *
     * @param uri request URI
     * @return e.g. {@code mygene.info} or {@code localhost:8080}
     */
    private static String hostOf(URI uri) {
        return uri.getPort() == -1 ? uri.getHost() : uri.getHost() + ":" + uri.getPort();
    }

    /**
     * Builds a UTF-8 URL-encoded query string.
     *
     * @param params query parameters, not yet encoded
     * @return e.g. {@code q=symbol%3ABRCA1&species=human}
     */
    private static String queryString(Map<String, String> params) {
        return params.entrySet().stream()
                .map(e -> encode(e.getKey()) + "=" + encode(e.getValue()))
                .collect(Collectors.joining("&"));
    }

    /**
     * URL-encodes a value in UTF-8.
     *
     * @param value raw value
     * @return the encoded value
     */
    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Returns the node as a list, whether it is missing, a single value or an array.
     *
     * @param node JSON node, may be {@code null}
     * @return the elements, empty if the node is missing or null
     */
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

    /**
     * Adds the text of the node to the set if it is not blank.
     *
     * @param target set receiving the text
     * @param node   JSON node, may be {@code null}
     */
    private static void addText(Set<String> target, JsonNode node) {
        String text = textOrNull(node);
        if (text != null && !text.isBlank()) {
            target.add(text);
        }
    }

    /**
     * Reads the text of a node.
     *
     * @param node JSON node, may be {@code null}
     * @return its text, or {@code null} if the node is missing or null
     */
    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asText();
    }

    /**
     * Reads the number of a node.
     *
     * @param node JSON node, may be {@code null}
     * @return its value, or {@code null} if the node is missing or null
     */
    private static Long longOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asLong();
    }
}
