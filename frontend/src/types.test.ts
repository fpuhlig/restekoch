import { describe, expect, it } from 'vitest'
import type { Recipe, ScanResponse } from './types'

describe('types', () => {
  it('Recipe has required fields', () => {
    const recipe: Recipe = {
      id: 'abc',
      title: 'Omelette',
      ingredients: ['3 eggs', '1 tbsp butter'],
      directions: ['Beat eggs', 'Cook'],
      ner: ['eggs', 'butter'],
    }
    expect(recipe.id).toBe('abc')
    expect(recipe.ner).toHaveLength(2)
  })

  it('ScanResponse has required fields', () => {
    const response: ScanResponse = {
      ingredients: ['eggs', 'cheese'],
      recipes: [],
      explanation: 'No matching recipes found.',
      cached: false,
    }
    expect(response.cached).toBe(false)
    expect(response.recipes).toHaveLength(0)
  })

  it('ScanResponse cached flag is true on cache hit', () => {
    const response: ScanResponse = {
      ingredients: ['eggs'],
      recipes: [],
      explanation: 'Cached result.',
      cached: true,
    }
    expect(response.cached).toBe(true)
  })
})
