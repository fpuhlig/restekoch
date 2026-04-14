interface Props {
  ingredients: string[]
}

export function IngredientTags({ ingredients }: Props) {
  if (ingredients.length === 0) return null

  return (
    <section>
      <h2 className="section-label">Detected ingredients</h2>
      <div className="ingredients">
        {ingredients.map((ing) => (
          <span key={ing} className="tag">
            {ing}
          </span>
        ))}
      </div>
    </section>
  )
}
