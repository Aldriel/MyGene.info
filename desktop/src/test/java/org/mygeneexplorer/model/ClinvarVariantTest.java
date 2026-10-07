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
