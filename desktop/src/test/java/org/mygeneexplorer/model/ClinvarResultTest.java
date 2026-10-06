package org.mygeneexplorer.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ClinvarResultTest {

    @Test
    void variantsAreDefensivelyCopied() {
        List<ClinvarVariant> variants = new ArrayList<>();
        ClinvarResult result = new ClinvarResult(0, variants);

        variants.add(new ClinvarVariant("hgvs", 1L, List.of(), List.of()));

        assertEquals(List.of(), result.variants());
        assertThrows(UnsupportedOperationException.class, () -> result.variants().add(null));
    }
}
