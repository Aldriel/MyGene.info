package org.mygeneexplorer.model;

import java.util.List;

/**
 * ClinVar variant associated with a gene.
 *
 * <p>A variant may have several ClinVar submissions (RCV), hence the lists of clinical
 * significances and origins.
 *
 * @param hgvs                  genomic HGVS notation, e.g. {@code chr17:g.41199721C>T}
 * @param variantId             ClinVar variation identifier, or {@code null} if absent
 * @param clinicalSignificances distinct clinical significances (e.g. "Pathogenic")
 * @param origins               distinct origins (e.g. "germline")
 */
public record ClinvarVariant(
        String hgvs,
        Long variantId,
        List<String> clinicalSignificances,
        List<String> origins
) {

    private static final String CLINVAR_VARIATION_URL =
            "https://www.ncbi.nlm.nih.gov/clinvar/variation/";

    /** Defensive copy: the lists of the record stay immutable. */
    public ClinvarVariant {
        clinicalSignificances = List.copyOf(clinicalSignificances);
        origins = List.copyOf(origins);
    }

    /**
     * @return the URL of the ClinVar record of the variant, or {@code null} without identifier
     */
    public String clinvarUrl() {
        return variantId == null ? null : CLINVAR_VARIATION_URL + variantId + "/";
    }
}
