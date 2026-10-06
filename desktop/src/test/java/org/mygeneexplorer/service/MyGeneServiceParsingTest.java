package org.mygeneexplorer.service;

import org.junit.jupiter.api.Test;
import org.mygeneexplorer.model.ClinvarVariant;
import org.mygeneexplorer.model.GeneInfo;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MyGeneServiceParsingTest {

    private final MyGeneService service = new MyGeneService();

    @Test
    void parseGeneReturnsFirstHit() throws IOException {
        String json = """
                {"total": 1, "hits": [{
                  "_id": "672", "symbol": "BRCA1", "name": "BRCA1 DNA repair associated",
                  "entrezgene": 672, "summary": "Tumor suppressor.",
                  "type_of_gene": "protein-coding", "map_location": "17q21.31",
                  "alias": ["RNF53", "BRCC1", "RNF53", ""]
                }]}
                """;

        Optional<GeneInfo> gene = service.parseGene(json);

        assertEquals(Optional.of(new GeneInfo(
                "672", "BRCA1", "BRCA1 DNA repair associated", 672L, "Tumor suppressor.",
                "protein-coding", "17q21.31", List.of("RNF53", "BRCC1"))), gene);
    }

    @Test
    void parseGeneAcceptsASingleAlias() throws IOException {
        String json = "{\"hits\": [{\"symbol\": \"TP53\", \"alias\": \"P53\"}]}";

        assertEquals(List.of("P53"), service.parseGene(json).orElseThrow().aliases());
    }

    @Test
    void parseGeneHandlesMissingOptionalFields() throws IOException {
        String json = """
                {"total": 1, "hits": [{"_id": "ENSG01", "symbol": "XYZ", "name": "Test",
                  "summary": null}]}
                """;

        GeneInfo gene = service.parseGene(json).orElseThrow();

        assertNull(gene.entrezGene());
        assertNull(gene.summary());
        assertNull(gene.typeOfGene());
        assertEquals(List.of(), gene.aliases());
    }

    @Test
    void parseGeneReturnsEmptyWhenNoHit() throws IOException {
        assertTrue(service.parseGene("{\"total\": 0, \"hits\": []}").isEmpty());
        assertTrue(service.parseGene("{}").isEmpty());
    }

    @Test
    void parseClinvarVariantsMergesRcvWithoutDuplicates() throws IOException {
        String json = """
                {"total": 14415, "hits": [{
                  "_id": "chr17:g.41199721C>T",
                  "clinvar": {"variant_id": 125845, "rcv": [
                    {"clinical_significance": "Pathogenic", "origin": "germline"},
                    {"clinical_significance": "Pathogenic", "origin": "germline"},
                    {"clinical_significance": "Pathogenic", "origin": "unknown"}
                  ]}
                }]}
                """;

        List<ClinvarVariant> variants = service.parseClinvarVariants(json);

        assertEquals(List.of(new ClinvarVariant(
                "chr17:g.41199721C>T", 125845L,
                List.of("Pathogenic"), List.of("germline", "unknown"))), variants);
        assertEquals(14415, service.parseTotal(json));
    }

    @Test
    void parseClinvarVariantsAcceptsSingleRcvAndOriginArray() throws IOException {
        String json = """
                {"total": 1, "hits": [{
                  "_id": "chr17:g.41199724G>A",
                  "clinvar": {"variant_id": 865740, "rcv": {
                    "clinical_significance": "not provided", "origin": ["germline", "somatic"]
                  }}
                }]}
                """;

        ClinvarVariant variant = service.parseClinvarVariants(json).getFirst();

        assertEquals(List.of("not provided"), variant.clinicalSignificances());
        assertEquals(List.of("germline", "somatic"), variant.origins());
    }

    @Test
    void parseClinvarVariantsKeepsOneRowPerClinvarRecord() throws IOException {
        String json = """
                {"total": 1, "hits": [{
                  "_id": "chr1:g.1A>G",
                  "clinvar": [
                    {"variant_id": 1, "rcv": {"clinical_significance": "Benign"}},
                    {"variant_id": 2, "rcv": {"clinical_significance": " "}}
                  ]
                }]}
                """;

        List<ClinvarVariant> variants = service.parseClinvarVariants(json);

        assertEquals(List.of(1L, 2L), variants.stream().map(ClinvarVariant::variantId).toList());
        assertEquals(List.of(), variants.get(0).origins());
        assertEquals(List.of(), variants.get(1).clinicalSignificances());
    }

    @Test
    void parseClinvarVariantsIgnoresHitsWithoutClinvar() throws IOException {
        String json = "{\"total\": 1, \"hits\": [{\"_id\": \"chr1:g.1A>G\", \"clinvar\": null}]}";

        assertTrue(service.parseClinvarVariants(json).isEmpty());
    }

    @Test
    void parseTotalDefaultsToZero() throws IOException {
        assertEquals(0, service.parseTotal("{}"));
    }

    @Test
    void parseRejectsInvalidJson() {
        assertThrows(IOException.class, () -> service.parseGene("not JSON"));
    }
}
