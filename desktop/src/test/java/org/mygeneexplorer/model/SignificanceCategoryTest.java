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
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mygeneexplorer.model.SignificanceCategory.BENIGN;
import static org.mygeneexplorer.model.SignificanceCategory.CONFLICTING;
import static org.mygeneexplorer.model.SignificanceCategory.LIKELY_BENIGN;
import static org.mygeneexplorer.model.SignificanceCategory.LIKELY_PATHOGENIC;
import static org.mygeneexplorer.model.SignificanceCategory.OTHER;
import static org.mygeneexplorer.model.SignificanceCategory.PATHOGENIC;
import static org.mygeneexplorer.model.SignificanceCategory.UNCERTAIN;

class SignificanceCategoryTest {

    private static ClinvarVariant variant(String... significances) {
        return new ClinvarVariant("hgvs", 1L, List.of(significances), List.of());
    }

    @ParameterizedTest(name = "\"{0}\" is {1}")
    @CsvSource(delimiter = '|', value = {
            "Pathogenic|PATHOGENIC",
            "pathogenic|PATHOGENIC",
            "Likely pathogenic|LIKELY_PATHOGENIC",
            "Pathogenic/Likely pathogenic|LIKELY_PATHOGENIC",
            "Conflicting interpretations of pathogenicity|CONFLICTING",
            "Uncertain significance|UNCERTAIN",
            "Likely benign|LIKELY_BENIGN",
            "Benign|BENIGN",
            "Benign/Likely benign|LIKELY_BENIGN",
            "not provided|OTHER",
            "risk factor|OTHER",
    })
    void classifiesClinvarSignificances(String significance, SignificanceCategory expected) {
        assertEquals(expected, SignificanceCategory.classify(significance));
    }

    @Test
    void classifiesNullAsOther() {
        assertEquals(OTHER, SignificanceCategory.classify(null));
    }

    @Test
    void categoriesOfAVariantWithoutSignificanceIsOther() {
        assertEquals(Set.of(OTHER), SignificanceCategory.categoriesOf(variant()));
    }

    @Test
    void primaryCategoryIsTheMostSevere() {
        assertEquals(PATHOGENIC,
                SignificanceCategory.primaryOf(variant("Benign", "Pathogenic", "not provided")));
        assertEquals(UNCERTAIN,
                SignificanceCategory.primaryOf(variant("Likely benign", "Uncertain significance")));
    }

    @Test
    void countByPrimaryIncludesEveryCategoryInSeverityOrder() {
        Map<SignificanceCategory, Long> counts = SignificanceCategory.countByPrimary(List.of(
                variant("Pathogenic"),
                variant("Pathogenic", "Benign"),
                variant("Likely benign"),
                variant()));

        assertEquals(List.of(PATHOGENIC, LIKELY_PATHOGENIC, CONFLICTING, UNCERTAIN,
                LIKELY_BENIGN, BENIGN, OTHER), List.copyOf(counts.keySet()));
        assertEquals(2L, counts.get(PATHOGENIC));
        assertEquals(1L, counts.get(LIKELY_BENIGN));
        assertEquals(1L, counts.get(OTHER));
        assertEquals(0L, counts.get(BENIGN));
    }

    @Test
    void exposesStyleAndMessageIdentifiers() {
        assertEquals("likely-pathogenic", LIKELY_PATHOGENIC.id());
        assertEquals("significance.likely-pathogenic", LIKELY_PATHOGENIC.messageKey());
        assertEquals("#dc2626", PATHOGENIC.color());
    }
}
