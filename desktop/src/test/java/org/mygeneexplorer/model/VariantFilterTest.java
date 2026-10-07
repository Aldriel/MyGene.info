/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mygeneexplorer.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VariantFilterTest {

    private static final ClinvarVariant VARIANT = new ClinvarVariant(
            "chr17:g.41199721C>T", 125845L,
            List.of("Pathogenic", "Uncertain significance"), List.of("germline"));

    @Test
    void noneMatchesEverythingAndIsInactive() {
        assertTrue(VariantFilter.NONE.test(VARIANT));
        assertFalse(VariantFilter.NONE.isActive());
    }

    @ParameterizedTest(name = "matches \"{0}\"")
    @ValueSource(strings = {"125845", "1258", "chr17:g.4119", "C>T", "PATHOGENIC", "uncertain",
            "Germ", "  germline  "})
    void matchesTextInAnyField(String text) {
        assertTrue(new VariantFilter(text, null).test(VARIANT));
    }

    @Test
    void rejectsTextFoundNowhere() {
        assertFalse(new VariantFilter("somatic", null).test(VARIANT));
    }

    @Test
    void handlesMissingIdentifierAndHgvs() {
        ClinvarVariant bare = new ClinvarVariant(null, null, List.of(), List.of());

        assertFalse(new VariantFilter("1", null).test(bare));
        assertTrue(new VariantFilter("", SignificanceCategory.OTHER).test(bare));
    }

    @Test
    void matchesAnyCategoryOfTheVariant() {
        assertTrue(new VariantFilter(null, SignificanceCategory.PATHOGENIC).test(VARIANT));
        assertTrue(new VariantFilter(null, SignificanceCategory.UNCERTAIN).test(VARIANT));
        assertFalse(new VariantFilter(null, SignificanceCategory.BENIGN).test(VARIANT));
    }

    @Test
    void combinesTextAndCategory() {
        assertTrue(new VariantFilter("germline", SignificanceCategory.PATHOGENIC).test(VARIANT));
        assertFalse(new VariantFilter("germline", SignificanceCategory.BENIGN).test(VARIANT));
        assertFalse(new VariantFilter("somatic", SignificanceCategory.PATHOGENIC).test(VARIANT));
    }

    @Test
    void normalizesTheText() {
        VariantFilter filter = new VariantFilter("  BRCA  ", null);

        assertEquals("brca", filter.text());
        assertTrue(filter.isActive());
        assertTrue(new VariantFilter(null, SignificanceCategory.BENIGN).isActive());
    }
}
