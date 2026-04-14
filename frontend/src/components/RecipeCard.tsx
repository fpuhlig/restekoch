import { useState } from 'react'
import type { Recipe } from '../types'

interface Props {
  recipe: Recipe
  scannedIngredients: string[]
  cached: boolean
}

export function RecipeCard({ recipe, scannedIngredients, cached }: Props) {
  const [expanded, setExpanded] = useState(false)
  const scannedLower = scannedIngredients.map((i) => i.toLowerCase())
  const uniqueNer = [...new Set(recipe.ner)]

  function isMatch(recipeIng: string): boolean {
    const r = recipeIng.toLowerCase()
    return scannedLower.some((s) => r.includes(s) || s.includes(r))
  }

  const matchCount = uniqueNer.filter(isMatch).length

  return (
    <article className={`recipe-card ${expanded ? 'expanded' : ''}`}>
      <div
        className="recipe-card-header"
        onClick={() => setExpanded(!expanded)}
        role="button"
        tabIndex={0}
        onKeyDown={(e) => {
          if (e.key === 'Enter' || e.key === ' ') {
            setExpanded(!expanded)
          }
        }}
      >
        <div>
          <h3 className="recipe-title">{recipe.title}</h3>
          <p className="recipe-meta">
            {matchCount} of {uniqueNer.length} ingredients match
          </p>
        </div>
        <div className="recipe-header-right">
          <span className={`cache-badge ${cached ? 'cached' : 'fresh'}`}>
            {cached ? 'Cached' : 'Fresh'}
          </span>
          <span className="expand-icon" aria-hidden="true">
            {expanded ? '\u25B2' : '\u25BC'}
          </span>
        </div>
      </div>

      <div className="recipe-ingredients">
        {uniqueNer.map((ing) => (
          <span key={ing} className={`recipe-ing ${isMatch(ing) ? 'match' : ''}`}>
            {ing}
          </span>
        ))}
      </div>

      {expanded && (
        <div className="recipe-detail">
          {recipe.ingredients.length > 0 && (
            <div className="recipe-section">
              <h4 className="recipe-section-label">Ingredients</h4>
              <ul className="recipe-list">
                {recipe.ingredients.map((ing, i) => (
                  <li key={i}>{ing}</li>
                ))}
              </ul>
            </div>
          )}

          {recipe.directions.length > 0 && (
            <div className="recipe-section">
              <h4 className="recipe-section-label">Directions</h4>
              <ol className="recipe-list">
                {recipe.directions.map((step, i) => (
                  <li key={i}>{step}</li>
                ))}
              </ol>
            </div>
          )}
        </div>
      )}
    </article>
  )
}
