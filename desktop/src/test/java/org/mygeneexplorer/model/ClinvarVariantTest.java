package org.mygeneexplorer.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinvarVariantTest {

    @Test
    void clinvarUrlPointsToTheNcbiRecord() {
        ClinvarVariant variant = new ClinvarVariant("hgvs", 125845L, List.of(), List.of());

        assertEquals("https://www.ncbi.nlm.nih.gov/clinvar/variation/125845/",
                variant.clinvarUrl());
    }

    @Test
    void clinvarUrlIsNullWithoutIdentifier() {
        assertNull(new ClinvarVariant("hgvs", null, List.of(), List.of()).clinvarUrl());
    }

    @Test
    void listsAreDefensivelyCopied() {
        List<String> significances = new ArrayList<>(List.of("Pathogenic"));
        ClinvarVariant variant = new ClinvarVariant("hgvs", 1L, significances, List.of());

        significances.add("Benign");

        assertEquals(List.of("Pathogenic"), variant.clinicalSignificances());
        assertThrows(UnsupportedOperationException.class,
                () -> variant.origins().add("germline"));
    }
}
