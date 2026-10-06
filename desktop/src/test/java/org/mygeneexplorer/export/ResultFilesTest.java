package org.mygeneexplorer.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResultFilesTest {

    private static final SearchResult RESULT = new SearchResult(
            "brca1",
            new GeneInfo("672", "BRCA1", "BRCA1 DNA repair associated", 672L, "Tumor suppressor.",
                    "protein-coding", "17q21.31", List.of("RNF53", "BRCC1")),
            new ClinvarResult(14415, List.of(
                    new ClinvarVariant("chr17:g.41199721C>T", 125845L,
                            List.of("Pathogenic"), List.of("germline", "unknown")),
                    new ClinvarVariant("chr17:g.1A>G", null, List.of(), List.of()))),
            Instant.parse("2026-10-06T18:30:00Z"));

    @Test
    void savedResultsReopenIdentically(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("BRCA1.json");

        ResultFiles.save(RESULT, file, "1.0.0");

        assertEquals(RESULT, ResultFiles.load(file));
    }

    @Test
    void documentIsSelfDescribing() throws IOException {
        JsonNode root = new ObjectMapper().readTree(ResultFiles.toJson(RESULT, "1.2.3"));

        assertEquals(ResultFiles.FORMAT, root.get("format").asText());
        assertEquals(ResultFiles.FORMAT_VERSION, root.get("formatVersion").asInt());
        assertEquals("1.2.3", root.at("/generator/version").asText());
        assertEquals("https://mygene.info", root.at("/sources/gene").asText());
        assertEquals("2026-10-06T18:30:00Z", root.get("retrievedAt").asText());
        assertEquals("https://www.ncbi.nlm.nih.gov/clinvar/variation/125845/",
                root.at("/clinvar/variants/0/clinvarUrl").asText());
        assertEquals(14415, root.at("/clinvar/total").asLong());
    }

    @Test
    void toleratesMissingOptionalFields() throws ResultFileException {
        String json = """
                {"format": "mygene-explorer/search-result", "formatVersion": 1,
                 "query": "TP53", "retrievedAt": "2026-01-01T00:00:00Z",
                 "gene": {"symbol": "TP53", "aliases": [null]},
                 "clinvar": {"variants": [{"hgvs": "x", "origins": ["germline", null]}]}}
                """;

        SearchResult result = ResultFiles.fromJson(json, "tp53.json");

        assertNull(result.gene().entrezGene());
        assertEquals(List.of(), result.gene().aliases());
        assertEquals(1, result.clinvar().total());
        assertEquals(List.of("germline"), result.clinvar().variants().getFirst().origins());
    }

    private static ResultFileException loadFails(String json) {
        return assertThrows(ResultFileException.class,
                () -> ResultFiles.fromJson(json, "file.json"));
    }

    @Test
    void rejectsInvalidJson() {
        ResultFileException error = loadFails("{ not json");

        assertEquals(Reason.INVALID_FORMAT, error.reason());
        assertEquals("file.json", error.fileName());
        assertEquals("file.json is not a valid MyGene Explorer result file.", error.getMessage());
    }

    @Test
    void rejectsOtherJsonDocuments() {
        assertEquals(Reason.INVALID_FORMAT, loadFails("{\"name\": \"package\"}").reason());
        assertEquals(Reason.INVALID_FORMAT, loadFails("").reason());
    }

    @Test
    void rejectsDocumentsWithoutRequiredFields() {
        String json = """
                {"format": "mygene-explorer/search-result", "formatVersion": 1,
                 "query": "TP53", "retrievedAt": "2026-01-01T00:00:00Z", "gene": {}}
                """;

        assertEquals(Reason.INVALID_FORMAT, loadFails(json).reason());
    }

    @Test
    void rejectsInvalidDates() {
        String json = """
                {"format": "mygene-explorer/search-result", "formatVersion": 1,
                 "query": "TP53", "retrievedAt": "yesterday", "gene": {"symbol": "TP53"}}
                """;

        assertEquals(Reason.INVALID_FORMAT, loadFails(json).reason());
    }

    @Test
    void rejectsNewerFormatVersions() {
        String json = "{\"format\": \"mygene-explorer/search-result\", \"formatVersion\": 99}";

        assertEquals(Reason.UNSUPPORTED_VERSION, loadFails(json).reason());
    }

    @Test
    void reportsUnreadableFiles(@TempDir Path dir) {
        ResultFileException error = assertThrows(ResultFileException.class,
                () -> ResultFiles.load(dir.resolve("missing.json")));

        assertEquals(Reason.UNREADABLE, error.reason());
        assertEquals("missing.json", error.fileName());
    }

    @Test
    void writesUtf8(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("accents.json");
        SearchResult result = new SearchResult("x", new GeneInfo(null, "X", "Gène é", null, null,
                null, null, null), new ClinvarResult(0, List.of()), Instant.EPOCH);

        ResultFiles.save(result, file, "1.0.0");

        assertEquals("Gène é", ResultFiles.load(file).gene().name());
        assertEquals(true, Files.readString(file, StandardCharsets.UTF_8).contains("Gène é"));
    }
}
