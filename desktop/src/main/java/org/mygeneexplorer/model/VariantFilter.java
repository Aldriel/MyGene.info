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
