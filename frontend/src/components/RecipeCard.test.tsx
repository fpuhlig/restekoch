import { describe, expect, it } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { RecipeCard } from './RecipeCard'
import type { Recipe } from '../types'

const recipe: Recipe = {
  id: '1',
  title: 'French Omelette',
  ingredients: ['3 eggs', '1 tbsp butter', '50g cheese', '1 cup milk'],
  directions: ['Beat eggs', 'Melt butter in pan', 'Pour eggs and cook'],
  ner: ['eggs', 'butter', 'cheese', 'milk'],
}

describe('RecipeCard', () => {
  it('renders recipe title', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={['eggs']} cached={false} />)
    expect(screen.getByText('French Omelette')).toBeInTheDocument()
  })

  it('shows Fresh badge when not cached', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={false} />)
    expect(screen.getByText('Fresh')).toBeInTheDocument()
  })

  it('shows Cached badge when cached', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={true} />)
    expect(screen.getByText('Cached')).toBeInTheDocument()
  })

  it('counts exact ingredient matches', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={['eggs', 'butter']} cached={false} />)
    expect(screen.getByText('2 of 4 ingredients match')).toBeInTheDocument()
  })

  it('matches substring ingredients (tomatoes matches tomato)', () => {
    const tomatoRecipe: Recipe = {
      ...recipe,
      ner: ['tomato', 'onion', 'garlic'],
    }
    render(<RecipeCard recipe={tomatoRecipe} scannedIngredients={['tomatoes']} cached={false} />)
    expect(screen.getByText('1 of 3 ingredients match')).toBeInTheDocument()
  })

  it('matches reverse substring (lettuce matches head lettuce)', () => {
    const saladRecipe: Recipe = {
      ...recipe,
      ner: ['head lettuce', 'cucumbers'],
    }
    render(<RecipeCard recipe={saladRecipe} scannedIngredients={['lettuce']} cached={false} />)
    expect(screen.getByText('1 of 2 ingredients match')).toBeInTheDocument()
  })

  it('deduplicates NER entries', () => {
    const dupeRecipe: Recipe = {
      ...recipe,
      ner: ['chicken', 'chicken', 'broth', 'chicken'],
    }
    render(<RecipeCard recipe={dupeRecipe} scannedIngredients={['chicken']} cached={false} />)
    expect(screen.getByText('1 of 2 ingredients match')).toBeInTheDocument()
  })

  it('highlights matching ingredients with match class', () => {
    const { container } = render(
      <RecipeCard recipe={recipe} scannedIngredients={['eggs']} cached={false} />,
    )
    const matchedEggs = container.querySelector('.recipe-ing.match')
    expect(matchedEggs).toHaveTextContent('eggs')
  })

  it('case insensitive matching', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={['EGGS', 'Butter']} cached={false} />)
    expect(screen.getByText('2 of 4 ingredients match')).toBeInTheDocument()
  })

  it('is collapsed by default', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={false} />)
    expect(screen.queryByText('Directions')).not.toBeInTheDocument()
    expect(screen.queryByText('Beat eggs')).not.toBeInTheDocument()
  })

  it('expands on click to show ingredients and directions', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={false} />)

    fireEvent.click(screen.getByText('French Omelette'))

    expect(screen.getByText('Ingredients')).toBeInTheDocument()
    expect(screen.getByText('3 eggs')).toBeInTheDocument()
    expect(screen.getByText('1 tbsp butter')).toBeInTheDocument()

    expect(screen.getByText('Directions')).toBeInTheDocument()
    expect(screen.getByText('Beat eggs')).toBeInTheDocument()
    expect(screen.getByText('Melt butter in pan')).toBeInTheDocument()
  })

  it('collapses again on second click', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={false} />)

    fireEvent.click(screen.getByText('French Omelette'))
    expect(screen.getByText('Beat eggs')).toBeInTheDocument()

    fireEvent.click(screen.getByText('French Omelette'))
    expect(screen.queryByText('Beat eggs')).not.toBeInTheDocument()
  })

  it('expands via keyboard Enter', () => {
    render(<RecipeCard recipe={recipe} scannedIngredients={[]} cached={false} />)
    const header = screen.getByRole('button')
    fireEvent.keyDown(header, { key: 'Enter' })
    expect(screen.getByText('Beat eggs')).toBeInTheDocument()
  })
})
