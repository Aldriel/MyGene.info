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

import java.util.Locale;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Filter applied to the variant table.
 *
 * @param text     case-insensitive text searched in the identifier, HGVS notation, clinical
 *                 significances and origins; blank to match everything
 * @param category category the variant must belong to, or {@code null} for any category
 */
public record VariantFilter(String text, SignificanceCategory category)
        implements Predicate<ClinvarVariant> {

    /** Filter matching every variant. */
    public static final VariantFilter NONE = new VariantFilter("", null);

    /** Normalizes the text: {@code null} becomes empty, case and surrounding spaces are ignored. */
    public VariantFilter {
        text = text == null ? "" : text.strip().toLowerCase(Locale.ROOT);
    }

    /** @return whether the filter excludes anything */
    public boolean isActive() {
        return !text.isEmpty() || category != null;
    }

    /**
     * Tests whether a variant passes the filter.
     *
     * @param variant variant to test
     * @return {@code true} if it belongs to the category and contains the text
     */
    @Override
    public boolean test(ClinvarVariant variant) {
        if (category != null && !SignificanceCategory.categoriesOf(variant).contains(category)) {
            return false;
        }
        if (text.isEmpty()) {
            return true;
        }
        String id = variant.variantId() == null ? null : variant.variantId().toString();
        Stream<String> values = Stream.concat(
                Stream.of(id, variant.hgvs()),
                Stream.concat(variant.clinicalSignificances().stream(), variant.origins().stream()));
        return values.anyMatch(v -> v != null && v.toLowerCase(Locale.ROOT).contains(text));
    }
}
