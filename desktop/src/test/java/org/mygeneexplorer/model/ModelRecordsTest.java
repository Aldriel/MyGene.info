package org.mygeneexplorer.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelRecordsTest {

    private static GeneInfo gene(Long entrez, List<String> aliases) {
        return new GeneInfo("672", "BRCA1", "name", entrez, null, null, null, aliases);
    }

    @Test
    void geneAliasesAreNeverNullAndImmutable() {
        List<String> aliases = new ArrayList<>(List.of("RNF53"));
        GeneInfo gene = gene(672L, aliases);
        aliases.add("BRCC1");

        assertEquals(List.of("RNF53"), gene.aliases());
        assertEquals(List.of(), gene(672L, null).aliases());
        assertThrows(UnsupportedOperationException.class, () -> gene.aliases().add("X"));
    }

    @Test
    void entrezIdFallsBackToMyGeneId() {
        assertEquals("672", gene(672L, null).entrezIdOrId());
        assertEquals("672", gene(null, null).entrezIdOrId());
        assertEquals("ENSG1", new GeneInfo("ENSG1", "X", null, null, null, null, null, null)
                .entrezIdOrId());
    }

    @Test
    void searchResultRequiresAllFields() {
        GeneInfo gene = gene(672L, null);
        ClinvarResult clinvar = new ClinvarResult(0, List.of());
        Instant now = Instant.now();

        assertThrows(NullPointerException.class, () -> new SearchResult(null, gene, clinvar, now));
        assertThrows(NullPointerException.class, () -> new SearchResult("q", null, clinvar, now));
        assertThrows(NullPointerException.class, () -> new SearchResult("q", gene, null, now));
        assertThrows(NullPointerException.class, () -> new SearchResult("q", gene, clinvar, null));
    }
}
