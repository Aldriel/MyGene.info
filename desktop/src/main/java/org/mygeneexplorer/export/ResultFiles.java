package org.mygeneexplorer.export;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.mygeneexplorer.export.ResultFileException.Reason;
import org.mygeneexplorer.model.ClinvarResult;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;
import org.mygeneexplorer.model.SearchResult;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Saves and reopens complete search results as self-describing JSON documents.
 *
 * <p>The document records its format and version, the application that produced it, the data
 * sources and the retrieval date, so a saved result remains traceable.
 */
public final class ResultFiles {

    /** Identifier of the document format. */
    public static final String FORMAT = "mygene-explorer/search-result";

    /** Current version of the document format. */
    public static final int FORMAT_VERSION = 1;

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    /** Not instantiable: static utility class. */
    private ResultFiles() {
    }

    /**
     * Writes a search result to a file, encoded in UTF-8.
     *
     * @param result     result to save
     * @param file       destination file
     * @param appVersion version of the application, recorded in the document
     * @throws IOException if the file cannot be written
     */
    public static void save(SearchResult result, Path file, String appVersion)
            throws IOException {
        Files.writeString(file, toJson(result, appVersion), StandardCharsets.UTF_8);
    }

    /**
     * Reads a search result previously written by {@link #save}.
     *
     * @throws ResultFileException if the file cannot be read or is not a valid result file
     */
    public static SearchResult load(Path file) throws ResultFileException {
        String name = String.valueOf(file.getFileName());
        String json;
        try {
            json = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResultFileException(Reason.UNREADABLE, name, e);
        }
        return fromJson(json, name);
    }

    /** Serializes a search result. */
    public static String toJson(SearchResult result, String appVersion) {
        ObjectNode root = MAPPER.createObjectNode();
        root.put("format", FORMAT);
        root.put("formatVersion", FORMAT_VERSION);
        root.putObject("generator")
                .put("application", "MyGene Explorer")
                .put("version", appVersion);
        root.putObject("sources")
                .put("gene", "https://mygene.info")
                .put("variants", "https://myvariant.info")
                .put("clinicalData", "https://www.ncbi.nlm.nih.gov/clinvar/");
        root.put("query", result.query());
        root.put("retrievedAt", result.retrievedAt().toString());

        GeneInfo gene = result.gene();
        ObjectNode geneNode = root.putObject("gene")
                .put("id", gene.id())
                .put("symbol", gene.symbol())
                .put("name", gene.name())
                .put("entrezGene", gene.entrezGene())
                .put("typeOfGene", gene.typeOfGene())
                .put("mapLocation", gene.mapLocation())
                .put("summary", gene.summary());
        ArrayNode aliases = geneNode.putArray("aliases");
        gene.aliases().forEach(aliases::add);

        ObjectNode clinvar = root.putObject("clinvar");
        clinvar.put("total", result.clinvar().total());
        ArrayNode variants = clinvar.putArray("variants");
        for (ClinvarVariant variant : result.clinvar().variants()) {
            ObjectNode node = variants.addObject()
                    .put("variantId", variant.variantId())
                    .put("hgvs", variant.hgvs());
            ArrayNode significances = node.putArray("clinicalSignificances");
            variant.clinicalSignificances().forEach(significances::add);
            ArrayNode origins = node.putArray("origins");
            variant.origins().forEach(origins::add);
            node.put("clinvarUrl", variant.clinvarUrl());
        }

        try {
            return MAPPER.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize a search result", e);
        }
    }

    /**
     * Parses a search result.
     *
     * @param json     document content
     * @param fileName name used in error messages
     * @throws ResultFileException if the document is not a valid result file
     */
    public static SearchResult fromJson(String json, String fileName) throws ResultFileException {
        try {
            JsonNode root = MAPPER.readTree(json);
            if (root == null || !FORMAT.equals(root.path("format").asText(null))) {
                throw invalid(fileName, null);
            }
            if (root.path("formatVersion").asInt(0) > FORMAT_VERSION) {
                throw new ResultFileException(Reason.UNSUPPORTED_VERSION, fileName, null);
            }

            JsonNode geneNode = root.path("gene");
            String symbol = text(geneNode, "symbol");
            String query = text(root, "query");
            String retrievedAt = text(root, "retrievedAt");
            if (symbol == null || query == null || retrievedAt == null) {
                throw invalid(fileName, null);
            }

            GeneInfo gene = new GeneInfo(
                    text(geneNode, "id"),
                    symbol,
                    text(geneNode, "name"),
                    geneNode.hasNonNull("entrezGene") ? geneNode.get("entrezGene").asLong() : null,
                    text(geneNode, "summary"),
                    text(geneNode, "typeOfGene"),
                    text(geneNode, "mapLocation"),
                    strings(geneNode.path("aliases")));

            List<ClinvarVariant> variants = new ArrayList<>();
            for (JsonNode node : root.path("clinvar").path("variants")) {
                variants.add(new ClinvarVariant(
                        text(node, "hgvs"),
                        node.hasNonNull("variantId") ? node.get("variantId").asLong() : null,
                        strings(node.path("clinicalSignificances")),
                        strings(node.path("origins"))));
            }
            long total = root.path("clinvar").path("total").asLong(variants.size());

            return new SearchResult(query, gene, new ClinvarResult(total, variants),
                    Instant.parse(retrievedAt));
        } catch (JsonProcessingException | DateTimeParseException e) {
            throw invalid(fileName, e);
        }
    }

    /**
     * Creates the exception reporting a file that is not a valid result file.
     *
     * @param fileName name of the file
     * @param cause    underlying exception, or {@code null}
     * @return the exception
     */
    private static ResultFileException invalid(String fileName, Throwable cause) {
        return new ResultFileException(Reason.INVALID_FORMAT, fileName, cause);
    }

    /**
     * Reads a text field.
     *
     * @param node  JSON object
     * @param field name of the field
     * @return the text, or {@code null} if the field is missing or null
     */
    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }

    /**
     * Reads an array of texts, skipping null entries.
     *
     * @param array JSON array, or a missing node
     * @return the texts, in order
     */
    private static List<String> strings(JsonNode array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> {
            if (!value.isNull()) {
                values.add(value.asText());
            }
        });
        return values;
    }
}
