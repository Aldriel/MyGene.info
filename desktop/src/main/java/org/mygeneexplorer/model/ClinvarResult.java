package org.mygeneexplorer.model;

import java.util.List;

/**
 * ClinVar variants fetched for a gene.
 *
 * @param total    total number of variants available on the server, which may exceed the
 *                 number of variants fetched
 * @param variants variants fetched
 */
public record ClinvarResult(long total, List<ClinvarVariant> variants) {

    /** Defensive copy: the list stays immutable. */
    public ClinvarResult {
        variants = List.copyOf(variants);
    }
}
