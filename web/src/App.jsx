import GeneSearch from './components/GeneSearch.jsx'

/** Composant racine : en-tête de l'application et formulaire de recherche. */
export default function App() {
  return (
    <main className="min-h-screen px-4 py-10">
      <div className="mx-auto max-w-5xl">
        <header className="mb-8">
          <h1 className="text-3xl font-bold text-slate-900">MyGene Explorer</h1>
          <p className="mt-1 text-slate-600">
            Recherche de gènes via MyGene.info et de variants ClinVar via MyVariant.info
          </p>
        </header>

        <GeneSearch />
      </div>
    </main>
  )
}
