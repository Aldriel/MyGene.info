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

/**
 * Interface texts in English and French.
 *
 * Placeholders are written `{name}`; numbers passed as parameters are formatted for the
 * language. Both dictionaries must define the same keys with the same placeholders (checked by
 * the tests).
 */

const en = {
  'app.subtitle':
    'Search human genes with MyGene.info and their ClinVar variants with MyVariant.info',
  'app.disclaimer': 'For research and exploration only. Not intended for clinical decision-making.',

  'header.language': 'Language',
  'header.textSize': 'Text size',
  'textSize.small': 'Small',
  'textSize.normal': 'Normal',
  'textSize.large': 'Large',
  'textSize.extraLarge': 'Extra large',

  'search.label': 'Gene symbol',
  'search.placeholder': 'Gene symbol (e.g. BRCA1, TP53, CFTR)',
  'search.button': 'Search',
  'search.searching': 'Searching…',
  'search.maxVariants': 'Max. variants',
  'search.recent': 'Recent searches:',
  'search.clearRecent': 'Clear history',

  'welcome.title': 'Explore a human gene',
  'welcome.text':
    'Enter an official gene symbol to view its summary, its ClinVar variants and the distribution of their clinical significance. Results can be filtered, shared with a link, and exported to CSV, JSON or PDF.',
  'welcome.examples': 'Try:',
  'welcome.open': 'Open a results file (.json)…',

  loading: 'Querying MyGene.info and MyVariant.info…',

  'gene.type': 'Type',
  'gene.location': 'Location',
  'gene.aliases': 'Aliases',
  'gene.summary': 'Summary',
  'gene.ncbiLink': 'NCBI Gene {id}',
  'gene.showMore': 'Show full summary',
  'gene.showLess': 'Collapse summary',

  'tab.variants': 'Variants',
  'tab.summary': 'Summary',

  'table.title': 'ClinVar variants',
  'table.variantId': 'Variant ID',
  'table.hgvs': 'HGVS',
  'table.significance': 'Clinical significance',
  'table.origin': 'Origin',
  'table.empty': 'No ClinVar variant found for this gene.',
  'table.sortBy': 'Sort by {column}',

  'filter.label': 'Filter variants',
  'filter.prompt': 'Filter by ID, HGVS, significance or origin',
  'filter.category': 'Clinical significance category',
  'filter.allCategories': 'All significances',
  'filter.clear': 'Clear filter',
  'filter.count': 'Showing {shown} of {loaded} loaded · {total} in ClinVar',
  'filter.noMatch': 'No variant matches the filter.',

  'significance.pathogenic': 'Pathogenic',
  'significance.likely-pathogenic': 'Likely pathogenic',
  'significance.conflicting': 'Conflicting interpretations',
  'significance.uncertain': 'Uncertain significance',
  'significance.likely-benign': 'Likely benign',
  'significance.benign': 'Benign',
  'significance.other': 'Other / not provided',

  'summary.title': 'Clinical significance distribution',
  'summary.empty': 'No variant to summarize.',
  'summary.caption':
    'Based on the {loaded} variants loaded out of {total} in ClinVar. Each variant is counted once, in its most severe category.',
  'summary.pathogenicShare': 'Pathogenic or likely pathogenic: {count} ({percent})',
  'summary.legendCount': '{count} ({percent})',
  'summary.chart': 'Pie chart of the clinical significance distribution',

  'actions.export': 'Export',
  'actions.exportCsv': 'CSV — spreadsheet',
  'actions.exportJson': 'JSON — reopenable results',
  'actions.exportPdf': 'PDF — printable report',
  'actions.copyLink': 'Copy link',
  'actions.open': 'Open results…',
  'actions.dismiss': 'Dismiss',

  'status.downloaded': '{file} downloaded.',
  'status.preparingPdf': 'Preparing the PDF report…',
  'status.linkCopied': 'Link copied to the clipboard.',
  'status.loaded': 'Results opened from {file} (retrieved {date}).',
  'status.retrievedAt': 'Data retrieved {date}',
  'status.sources': 'Data: MyGene.info · MyVariant.info · ClinVar',

  'export.gene': 'Gene',
  'export.clinvarUrl': 'ClinVar URL',

  'pdf.title': 'ClinVar variant report',
  'pdf.entrez': 'NCBI Gene ID',
  'pdf.retrieved': 'Data retrieved',
  'pdf.variants': 'ClinVar variants',
  'pdf.generated': 'Generated {date} by {app} {version}',
  'pdf.page': 'Page {page} of {pages}',

  'brand.title': 'Maxime Ethier - Biocomputing Consultant',
  'brand.website': 'https://www.maximeethier.com/en',

  'footer.license': 'Apache License 2.0',
  'footer.licenseTitle': 'Read the license under which this application is distributed',
  'footer.availability': 'Available for bioinformatics contracts or employment.',
  'footer.kofi': 'Support me on Ko-fi',
  'footer.kofiTitle': 'Support the development of this application with a donation on Ko-fi',
  'footer.version': 'Version {version}',

  'error.title': 'Error',
  'error.symbol.empty': 'Please enter a gene symbol.',
  'error.symbol.tooLong': 'Gene symbols are at most {max} characters long.',
  'error.symbol.malformed':
    '"{symbol}" is not a valid gene symbol. Use letters, digits, hyphens, dots or underscores only (e.g. BRCA1, HLA-A, C9orf72).',
  'error.geneNotFound': 'No human gene found for symbol "{symbol}".',
  'error.api.timeout': '{host} did not respond in time. Please try again.',
  'error.api.unreachable': 'Unable to reach {host}. Please check your internet connection.',
  'error.api.invalidResponse': 'Received an invalid response from {host}.',
  'error.api.tooManyRequests':
    'Too many requests sent to {host}. Please wait a moment and try again.',
  'error.api.unavailable':
    '{host} is temporarily unavailable (HTTP {status}). Please try again later.',
  'error.api.badRequest': '{host} rejected the request (HTTP 400). Please check the gene symbol.',
  'error.api.unexpectedStatus': 'Unexpected response from {host} (HTTP {status}).',
  'error.file.unreadable': 'Unable to read {file}.',
  'error.file.invalid': '{file} is not a valid MyGene Explorer results file.',
  'error.file.unsupported':
    '{file} was created by a newer version of MyGene Explorer. Please update the application.',
  'error.export': 'The export failed. Please try again.',
  'error.clipboard': 'Unable to copy the link. Copy it from the address bar instead.',
  'error.unexpected': 'An unexpected error occurred.',
}

const fr = {
  'app.subtitle':
    'Recherchez des gènes humains avec MyGene.info et leurs variants ClinVar avec MyVariant.info',
  'app.disclaimer':
    'Destiné à la recherche et à l’exploration uniquement. Ne pas utiliser pour une décision clinique.',

  'header.language': 'Langue',
  'header.textSize': 'Taille du texte',
  'textSize.small': 'Petite',
  'textSize.normal': 'Normale',
  'textSize.large': 'Grande',
  'textSize.extraLarge': 'Très grande',

  'search.label': 'Symbole du gène',
  'search.placeholder': 'Symbole du gène (ex. BRCA1, TP53, CFTR)',
  'search.button': 'Rechercher',
  'search.searching': 'Recherche…',
  'search.maxVariants': 'Variants max.',
  'search.recent': 'Recherches récentes :',
  'search.clearRecent': 'Effacer l’historique',

  'welcome.title': 'Explorez un gène humain',
  'welcome.text':
    'Saisissez le symbole officiel d’un gène pour afficher sa fiche, ses variants ClinVar et la répartition de leur signification clinique. Les résultats peuvent être filtrés, partagés par un lien et exportés en CSV, JSON ou PDF.',
  'welcome.examples': 'Essayez :',
  'welcome.open': 'Ouvrir un fichier de résultats (.json)…',

  loading: 'Interrogation de MyGene.info et MyVariant.info…',

  'gene.type': 'Type',
  'gene.location': 'Localisation',
  'gene.aliases': 'Alias',
  'gene.summary': 'Résumé',
  'gene.ncbiLink': 'NCBI Gene {id}',
  'gene.showMore': 'Afficher le résumé complet',
  'gene.showLess': 'Réduire le résumé',

  'tab.variants': 'Variants',
  'tab.summary': 'Résumé',

  'table.title': 'Variants ClinVar',
  'table.variantId': 'ID du variant',
  'table.hgvs': 'HGVS',
  'table.significance': 'Signification clinique',
  'table.origin': 'Origine',
  'table.empty': 'Aucun variant ClinVar trouvé pour ce gène.',
  'table.sortBy': 'Trier par {column}',

  'filter.label': 'Filtrer les variants',
  'filter.prompt': 'Filtrer par ID, HGVS, signification ou origine',
  'filter.category': 'Catégorie de signification clinique',
  'filter.allCategories': 'Toutes les significations',
  'filter.clear': 'Effacer le filtre',
  'filter.count': '{shown} affichés sur {loaded} chargés · {total} dans ClinVar',
  'filter.noMatch': 'Aucun variant ne correspond au filtre.',

  'significance.pathogenic': 'Pathogène',
  'significance.likely-pathogenic': 'Probablement pathogène',
  'significance.conflicting': 'Interprétations contradictoires',
  'significance.uncertain': 'Signification incertaine',
  'significance.likely-benign': 'Probablement bénin',
  'significance.benign': 'Bénin',
  'significance.other': 'Autre / non fourni',

  'summary.title': 'Répartition des significations cliniques',
  'summary.empty': 'Aucun variant à résumer.',
  'summary.caption':
    'Basé sur les {loaded} variants chargés sur {total} dans ClinVar. Chaque variant est compté une fois, dans sa catégorie la plus sévère.',
  'summary.pathogenicShare': 'Pathogènes ou probablement pathogènes : {count} ({percent})',
  'summary.legendCount': '{count} ({percent})',
  'summary.chart': 'Graphique circulaire de la répartition des significations cliniques',

  'actions.export': 'Exporter',
  'actions.exportCsv': 'CSV — tableur',
  'actions.exportJson': 'JSON — résultats réouvrables',
  'actions.exportPdf': 'PDF — rapport imprimable',
  'actions.copyLink': 'Copier le lien',
  'actions.open': 'Ouvrir des résultats…',
  'actions.dismiss': 'Fermer',

  'status.downloaded': '{file} téléchargé.',
  'status.preparingPdf': 'Préparation du rapport PDF…',
  'status.linkCopied': 'Lien copié dans le presse-papiers.',
  'status.loaded': 'Résultats ouverts depuis {file} (récupérés le {date}).',
  'status.retrievedAt': 'Données récupérées le {date}',
  'status.sources': 'Données : MyGene.info · MyVariant.info · ClinVar',

  'export.gene': 'Gène',
  'export.clinvarUrl': 'URL ClinVar',

  'pdf.title': 'Rapport des variants ClinVar',
  'pdf.entrez': 'Identifiant NCBI Gene',
  'pdf.retrieved': 'Données récupérées le',
  'pdf.variants': 'Variants ClinVar',
  'pdf.generated': 'Généré le {date} par {app} {version}',
  'pdf.page': 'Page {page} sur {pages}',

  'brand.title': 'Maxime Ethier - Consultant en Bio-informatique',
  'brand.website': 'https://www.maximeethier.com',

  'footer.license': 'Licence Apache 2.0',
  'footer.licenseTitle': 'Lire la licence sous laquelle cette application est distribuée',
  'footer.availability': 'Disponible pour des contrats ou un emploi en bio-informatique.',
  'footer.kofi': 'Soutenez-moi sur Ko-fi',
  'footer.kofiTitle': 'Soutenir le développement de cette application par un don sur Ko-fi',
  'footer.version': 'Version {version}',

  'error.title': 'Erreur',
  'error.symbol.empty': 'Veuillez saisir un symbole de gène.',
  'error.symbol.tooLong': 'Les symboles de gènes comptent au plus {max} caractères.',
  'error.symbol.malformed':
    '« {symbol} » n’est pas un symbole de gène valide. Utilisez uniquement des lettres, des chiffres, des traits d’union, des points ou des traits de soulignement (ex. BRCA1, HLA-A, C9orf72).',
  'error.geneNotFound': 'Aucun gène humain trouvé pour le symbole « {symbol} ».',
  'error.api.timeout': '{host} n’a pas répondu à temps. Veuillez réessayer.',
  'error.api.unreachable':
    'Impossible de joindre {host}. Veuillez vérifier votre connexion Internet.',
  'error.api.invalidResponse': 'Réponse invalide reçue de {host}.',
  'error.api.tooManyRequests':
    'Trop de requêtes envoyées à {host}. Veuillez patienter un instant, puis réessayer.',
  'error.api.unavailable':
    '{host} est temporairement indisponible (HTTP {status}). Veuillez réessayer plus tard.',
  'error.api.badRequest':
    '{host} a refusé la requête (HTTP 400). Veuillez vérifier le symbole du gène.',
  'error.api.unexpectedStatus': 'Réponse inattendue de {host} (HTTP {status}).',
  'error.file.unreadable': 'Impossible de lire {file}.',
  'error.file.invalid': '{file} n’est pas un fichier de résultats MyGene Explorer valide.',
  'error.file.unsupported':
    '{file} a été créé par une version plus récente de MyGene Explorer. Veuillez mettre l’application à jour.',
  'error.export': 'L’exportation a échoué. Veuillez réessayer.',
  'error.clipboard': 'Impossible de copier le lien. Copiez-le plutôt depuis la barre d’adresse.',
  'error.unexpected': 'Une erreur inattendue s’est produite.',
}

export const MESSAGES = { en, fr }

/** Languages of the interface, each named in itself. */
export const LANGUAGES = [
  { code: 'en', name: 'English' },
  { code: 'fr', name: 'Français' },
]
