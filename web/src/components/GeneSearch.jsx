import { useRef, useState } from 'react'
import { fetchClinvarVariants, fetchGene } from '../api/biothings.js'
import ClinvarTable from './ClinvarTable.jsx'

const INPUT_CLASSES = [
  'flex-1 rounded-lg border border-slate-300 bg-white px-4 py-2.5',
  'text-slate-900 shadow-sm outline-none placeholder:text-slate-400',
  'focus:border-indigo-500 focus:ring-2 focus:ring-indigo-200',
].join(' ')

const BUTTON_CLASSES = [
  'rounded-lg bg-indigo-600 px-5 py-2.5 font-medium text-white shadow-sm',
  'transition hover:bg-indigo-700',
  'disabled:cursor-not-allowed disabled:opacity-50',
].join(' ')

const PANEL_CLASSES = 'rounded-lg border border-slate-200 bg-white p-4'

/**
 * Formulaire de recherche d'un gène par symbole.
 *
 * Interroge MyGene.info pour la fiche du gène, puis MyVariant.info pour
 * ses variants ClinVar. Gère les états : idle, loading, success, error.
 */
export default function GeneSearch() {
  const [query, setQuery] = useState('')
  const [status, setStatus] = useState('idle')
  const [error, setError] = useState(null)
  const [gene, setGene] = useState(null)
  const [clinvar, setClinvar] = useState(null)

  // Contrôleur de la recherche en cours, pour l'annuler si une nouvelle
  // recherche est lancée avant la fin de la précédente.
  const abortRef = useRef(null)

  async function handleSubmit(event) {
    event.preventDefault()
    const symbol = query.trim().toUpperCase()
    if (!symbol) return

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    setStatus('loading')
    setError(null)
    setGene(null)
    setClinvar(null)

    try {
      const foundGene = await fetchGene(symbol, controller.signal)
      if (!foundGene) {
        throw new Error(`Aucun gène humain trouvé pour le symbole « ${symbol} ».`)
      }

      const clinvarData = await fetchClinvarVariants(foundGene.symbol, controller.signal)
      setGene(foundGene)
      setClinvar(clinvarData)
      setStatus('success')
    } catch (err) {
      // Une requête annulée a été remplacée par une plus récente : on l'ignore.
      if (err.name === 'AbortError') return
      setError(err.message || 'Une erreur inattendue est survenue.')
      setStatus('error')
    }
  }

  const isLoading = status === 'loading'

  return (
    <section className="space-y-6">
      <form onSubmit={handleSubmit} className="flex gap-3">
        <input
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          placeholder="Symbole du gène (ex : BRCA1, TP53, CFTR)"
          aria-label="Symbole du gène"
          className={INPUT_CLASSES}
        />
        <button type="submit" disabled={isLoading || !query.trim()} className={BUTTON_CLASSES}>
          {isLoading ? 'Recherche…' : 'Rechercher'}
        </button>
      </form>

      {isLoading && <LoadingPanel />}

      {status === 'error' && (
        <div role="alert" className="rounded-lg border border-red-200 bg-red-50 p-4 text-red-800">
          <p className="font-semibold">Erreur</p>
          <p className="text-sm">{error}</p>
        </div>
      )}

      {status === 'success' && gene && (
        <>
          <GeneCard gene={gene} />

          {clinvar?.variants.length > 0 ? (
            <ClinvarTable variants={clinvar.variants} total={clinvar.total} />
          ) : (
            <div className={`${PANEL_CLASSES} text-slate-600`}>
              Aucune donnée ClinVar trouvée pour {gene.symbol}.
            </div>
          )}
        </>
      )}
    </section>
  )
}

/** Indicateur affiché pendant les requêtes. */
function LoadingPanel() {
  return (
    <div className={`${PANEL_CLASSES} flex items-center gap-3 text-slate-600`}>
      <span
        className={
          'h-5 w-5 animate-spin rounded-full border-2 border-indigo-200 border-t-indigo-600'
        }
      />
      Interrogation de MyGene.info et MyVariant.info…
    </div>
  )
}

/** Fiche résumée du gène trouvé, avec un lien vers NCBI Gene. */
function GeneCard({ gene }) {
  const entrezId = gene.entrezgene ?? gene._id

  return (
    <div className="rounded-lg border border-slate-200 bg-white p-5 shadow-sm">
      <div className="flex flex-wrap items-baseline gap-x-3">
        <h2 className="text-2xl font-bold text-slate-900">{gene.symbol}</h2>
        <span className="text-slate-600">{gene.name}</span>
        <a
          href={`https://www.ncbi.nlm.nih.gov/gene/${entrezId}`}
          target="_blank"
          rel="noreferrer"
          className="text-sm text-indigo-600 hover:underline"
        >
          Entrez {entrezId}
        </a>
      </div>

      {gene.summary && <p className="mt-3 line-clamp-4 text-sm text-slate-600">{gene.summary}</p>}
    </div>
  )
}
