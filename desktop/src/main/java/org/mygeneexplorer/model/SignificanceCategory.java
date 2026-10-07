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

import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Broad category of a ClinVar clinical significance, ordered from most to least severe.
 */
public enum SignificanceCategory {
    PATHOGENIC("pathogenic", "#dc2626"),
    LIKELY_PATHOGENIC("likely-pathogenic", "#f97316"),
    CONFLICTING("conflicting", "#9333ea"),
    UNCERTAIN("uncertain", "#f59e0b"),
    LIKELY_BENIGN("likely-benign", "#34d399"),
    BENIGN("benign", "#059669"),
    OTHER("other", "#94a3b8");

    /** Order matters: "likely pathogenic" must be tested before "pathogenic". */
    private static final List<Map.Entry<String, SignificanceCategory>> KEYWORDS = List.of(
            Map.entry("conflicting", CONFLICTING),
            Map.entry("likely pathogenic", LIKELY_PATHOGENIC),
            Map.entry("pathogenic", PATHOGENIC),
            Map.entry("likely benign", LIKELY_BENIGN),
            Map.entry("benign", BENIGN),
            Map.entry("uncertain", UNCERTAIN));

    private final String id;
    private final String color;

    SignificanceCategory(String id, String color) {
        this.id = id;
        this.color = color;
    }

    /** @return identifier used for CSS style classes, e.g. {@code likely-pathogenic} */
    public String id() {
        return id;
    }

    /** @return key of the localized display name */
    public String messageKey() {
        return "significance." + id;
    }

    /** @return color used for charts, as a CSS hexadecimal value */
    public String color() {
        return color;
    }

    /**
     * Classifies a ClinVar clinical significance, e.g. "Likely pathogenic".
     *
     * @param significance clinical significance as reported by ClinVar, possibly {@code null}
     * @return the matching category, or {@link #OTHER}
     */
    public static SignificanceCategory classify(String significance) {
        if (significance == null) {
            return OTHER;
        }
        String lower = significance.toLowerCase(Locale.ROOT);
        return KEYWORDS.stream()
                .filter(entry -> lower.contains(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(OTHER);
    }

    /**
     * @return the categories of all the significances of the variant, {@link #OTHER} if none
     */
    public static Set<SignificanceCategory> categoriesOf(ClinvarVariant variant) {
        Set<SignificanceCategory> categories = EnumSet.noneOf(SignificanceCategory.class);
        variant.clinicalSignificances().forEach(s -> categories.add(classify(s)));
        if (categories.isEmpty()) {
            categories.add(OTHER);
        }
        return categories;
    }

    /** @return the most severe category of the variant */
    public static SignificanceCategory primaryOf(ClinvarVariant variant) {
        return categoriesOf(variant).iterator().next();
    }

    /**
     * Counts variants by their most severe category.
     *
     * @return a count for every category, zero included, in severity order
     */
    public static Map<SignificanceCategory, Long> countByPrimary(
            Collection<ClinvarVariant> variants) {
        Map<SignificanceCategory, Long> counts = new EnumMap<>(SignificanceCategory.class);
        for (SignificanceCategory category : values()) {
            counts.put(category, 0L);
        }
        variants.forEach(v -> counts.merge(primaryOf(v), 1L, Long::sum));
        return counts;
    }
}
