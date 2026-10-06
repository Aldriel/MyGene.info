/**
 * Client des API publiques BioThings.
 *
 * - MyGene.info    : informations sur les gènes (symbole, nom, résumé).
 * - MyVariant.info : variants ClinVar. MyGene.info n'expose pas ClinVar,
 *                    ces données proviennent donc de son service jumeau.
 */

const MYGENE_URL = 'https://mygene.info/v3'
const MYVARIANT_URL = 'https://myvariant.info/v1'

const CLINVAR_FIELDS = [
  'clinvar.variant_id',
  'clinvar.rcv.clinical_significance',
  'clinvar.rcv.origin',
].join(',')

/**
 * Normalise une valeur en tableau : l'API renvoie un objet seul ou un
 * tableau selon le nombre d'éléments.
 */
const toArray = (value) => {
  if (value == null) return []
  return Array.isArray(value) ? value : [value]
}

/** Effectue un GET et renvoie le JSON, ou lève une erreur si le statut HTTP n'est pas 2xx. */
async function getJson(url, signal) {
  const response = await fetch(url, { signal })
  if (!response.ok) {
    throw new Error(`Erreur HTTP ${response.status} (${new URL(url).host})`)
  }
  return response.json()
}

/**
 * Recherche un gène humain par son symbole.
 *
 * @param {string} symbol Symbole du gène (ex. « BRCA1 »).
 * @param {AbortSignal} [signal] Signal permettant d'annuler la requête.
 * @returns {Promise<object|null>} Le gène trouvé, ou `null` s'il n'existe pas.
 */
export async function fetchGene(symbol, signal) {
  const params = new URLSearchParams({
    q: `symbol:${symbol}`,
    species: 'human',
    fields: 'symbol,name,entrezgene,summary',
    size: '1',
  })
  const data = await getJson(`${MYGENE_URL}/query?${params}`, signal)
  return data.hits?.[0] ?? null
}

/**
 * Récupère les variants ClinVar associés à un gène.
 *
 * Un variant peut avoir plusieurs soumissions ClinVar (RCV) : leurs
 * significations cliniques et origines sont regroupées sans doublons.
 *
 * @param {string} symbol Symbole du gène.
 * @param {AbortSignal} [signal] Signal permettant d'annuler la requête.
 * @param {number} [size=100] Nombre maximal de variants à récupérer.
 * @returns {Promise<{total: number, variants: object[]}>}
 */
export async function fetchClinvarVariants(symbol, signal, size = 100) {
  const params = new URLSearchParams({
    q: `clinvar.gene.symbol:${symbol}`,
    fields: CLINVAR_FIELDS,
    size: String(size),
  })
  const data = await getJson(`${MYVARIANT_URL}/query?${params}`, signal)

  const variants = data.hits.flatMap((hit) =>
    toArray(hit.clinvar).map((clinvar) => {
      const rcvs = toArray(clinvar.rcv)
      const significances = rcvs.map((rcv) => rcv.clinical_significance).filter(Boolean)
      const origins = rcvs.flatMap((rcv) => toArray(rcv.origin)).filter(Boolean)

      return {
        key: `${hit._id}-${clinvar.variant_id}`,
        hgvs: hit._id,
        variantId: clinvar.variant_id,
        significances: [...new Set(significances)],
        origins: [...new Set(origins)],
      }
    }),
  )

  return { total: data.total, variants }
}
