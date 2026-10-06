package org.mygeneexplorer.model;

import java.time.Instant;
import java.util.Objects;

/**
 * Complete result of a search: the gene, its ClinVar variants and when they were retrieved.
 *
 * @param query       symbol searched by the user
 * @param gene        gene found on MyGene.info
 * @param clinvar     ClinVar variants found on MyVariant.info
 * @param retrievedAt moment the data was retrieved from the APIs
 */
public record SearchResult(String query, GeneInfo gene, ClinvarResult clinvar,
                           Instant retrievedAt) {

    public SearchResult {
        Objects.requireNonNull(query, "query");
        Objects.requireNonNull(gene, "gene");
        Objects.requireNonNull(clinvar, "clinvar");
        Objects.requireNonNull(retrievedAt, "retrievedAt");
    }
}
