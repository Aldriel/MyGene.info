package org.mygeneexplorer.model;

/**
 * Fiche résumée d'un gène renvoyée par MyGene.info.
 *
 * @param id         identifiant MyGene.info (généralement l'identifiant Entrez)
 * @param symbol     symbole officiel, par exemple {@code BRCA1}
 * @param name       nom complet du gène
 * @param entrezGene identifiant NCBI Entrez Gene, ou {@code null} s'il est absent
 * @param summary    résumé fonctionnel, ou {@code null} s'il est absent
 */
public record GeneInfo(String id, String symbol, String name, Long entrezGene, String summary) {
}
