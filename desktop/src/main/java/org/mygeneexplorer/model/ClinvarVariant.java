package org.mygeneexplorer.model;

import java.util.List;

/**
 * Variant ClinVar associé à un gène.
 *
 * <p>Un même variant peut avoir plusieurs soumissions ClinVar (RCV), d'où les listes
 * de significations cliniques et d'origines.
 *
 * @param hgvs                  notation HGVS génomique, par exemple {@code chr17:g.41199721C>T}
 * @param variantId             identifiant de variation ClinVar, ou {@code null} s'il est absent
 * @param clinicalSignificances significations cliniques distinctes (ex. « Pathogenic »)
 * @param origins               origines distinctes (ex. « germline »)
 */
public record ClinvarVariant(
        String hgvs,
        Long variantId,
        List<String> clinicalSignificances,
        List<String> origins
) {

    private static final String CLINVAR_VARIATION_URL =
            "https://www.ncbi.nlm.nih.gov/clinvar/variation/";

    /** Copie défensive : les listes du record restent immuables. */
    public ClinvarVariant {
        clinicalSignificances = List.copyOf(clinicalSignificances);
        origins = List.copyOf(origins);
    }

    /**
     * @return l'URL de la fiche ClinVar du variant, ou {@code null} sans identifiant
     */
    public String clinvarUrl() {
        return variantId == null ? null : CLINVAR_VARIATION_URL + variantId + "/";
    }
}
