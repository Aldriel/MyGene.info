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
