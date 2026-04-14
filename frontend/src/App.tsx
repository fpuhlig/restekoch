import { useState } from 'react'
import { scanImage } from './api'
import { ErrorMessage } from './components/ErrorMessage'
import { IngredientTags } from './components/IngredientTags'
import { LoadingSpinner } from './components/LoadingSpinner'
import { PhotoUpload } from './components/PhotoUpload'
import { RecipeCard } from './components/RecipeCard'
import type { Recipe, ScanResponse } from './types'
import './App.css'

type State = 'idle' | 'preview' | 'scanning' | 'results'

function matchRatio(recipe: Recipe, scanned: string[]): number {
  const lower = scanned.map((s) => s.toLowerCase())
  const unique = [...new Set(recipe.ner)]
  if (unique.length === 0) return 0
  const matches = unique.filter((ing) =>
    lower.some((s) => ing.toLowerCase().includes(s) || s.includes(ing.toLowerCase())),
  ).length
  return matches / unique.length
}

function sortByMatchRatio(recipes: Recipe[], scanned: string[]): Recipe[] {
  return [...recipes].sort((a, b) => matchRatio(b, scanned) - matchRatio(a, scanned))
}

export default function App() {
  const [state, setState] = useState<State>('idle')
  const [file, setFile] = useState<File | null>(null)
  const [preview, setPreview] = useState<string | null>(null)
  const [result, setResult] = useState<ScanResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [duration, setDuration] = useState<number | null>(null)
  const [abortController, setAbortController] = useState<AbortController | null>(null)

  function handleSelect(selectedFile: File, previewUrl: string) {
    setFile(selectedFile)
    setPreview(previewUrl)
    setResult(null)
    setError(null)
    setDuration(null)
    setState('preview')
  }

  function handleUploadError(message: string) {
    setError(message)
  }

  async function handleScan() {
    if (!file) return
    setState('scanning')
    setError(null)

    const controller = new AbortController()
    setAbortController(controller)

    const start = performance.now()
    try {
      const response = await scanImage(file, 5, controller.signal)
      const elapsed = (performance.now() - start) / 1000
      setResult(response)
      setDuration(elapsed)
      setState('results')
    } catch (err) {
      if (controller.signal.aborted) return
      const message = err instanceof Error ? err.message : 'Something went wrong'
      setError(message)
      setState('preview')
    } finally {
      setAbortController(null)
    }
  }

  function handleReset() {
    if (abortController) abortController.abort()
    if (preview) URL.revokeObjectURL(preview)
    setFile(null)
    setPreview(null)
    setResult(null)
    setError(null)
    setDuration(null)
    setState('idle')
  }

  return (
    <>
      <header className="app-header">
        <h1>
          Reste<span className="brand">koch</span>
        </h1>
        <p>Scan your fridge. Cook what you have.</p>
      </header>

      <main className="app-main">
        {error && <ErrorMessage message={error} onDismiss={() => setError(null)} />}

        {state === 'idle' && (
          <PhotoUpload onSelect={handleSelect} onError={handleUploadError} disabled={false} />
        )}

        {(state === 'preview' || state === 'scanning') && preview && (
          <div className="preview">
            <img
              src={preview}
              alt="Selected photo"
              className={state === 'scanning' ? 'dimmed' : ''}
            />
            {state === 'scanning' ? (
              <>
                <LoadingSpinner />
                <button onClick={handleReset} className="reset-btn">
                  Cancel
                </button>
              </>
            ) : (
              <div className="preview-actions">
                <button onClick={handleScan} className="scan-btn">
                  Find recipes
                </button>
                <button onClick={handleReset} className="reset-btn">
                  Choose different photo
                </button>
              </div>
            )}
          </div>
        )}

        {state === 'results' && result && (
          <div className="results">
            <IngredientTags ingredients={result.ingredients} />

            {result.explanation && (
              <section>
                <h2 className="section-label">How these recipes match</h2>
                <p className="explanation">{result.explanation}</p>
              </section>
            )}

            <section>
              <h2 className="section-label">{result.recipes.length} recipes found</h2>
              {result.recipes.length === 0 ? (
                <p className="empty-state">
                  No matching recipes found. Try a photo with different ingredients.
                </p>
              ) : (
                sortByMatchRatio(result.recipes, result.ingredients).map((recipe) => (
                  <RecipeCard
                    key={recipe.id}
                    recipe={recipe}
                    scannedIngredients={result.ingredients}
                    cached={result.cached}
                  />
                ))
              )}
            </section>

            {duration !== null && (
              <p className="timing">
                Scanned in <strong>{duration.toFixed(1)}s</strong>
                {result.cached && ' (cached)'}
              </p>
            )}

            <button onClick={handleReset} className="scan-again">
              Scan another photo
            </button>
          </div>
        )}
      </main>
    </>
  )
}
