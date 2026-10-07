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
