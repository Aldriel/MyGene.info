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
