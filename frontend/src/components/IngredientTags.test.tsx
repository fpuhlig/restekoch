import { describe, expect, it } from 'vitest'
import { render, screen } from '@testing-library/react'
import { IngredientTags } from './IngredientTags'

describe('IngredientTags', () => {
  it('renders all ingredients as tags', () => {
    render(<IngredientTags ingredients={['eggs', 'cheese', 'bread']} />)

    expect(screen.getByText('eggs')).toBeInTheDocument()
    expect(screen.getByText('cheese')).toBeInTheDocument()
    expect(screen.getByText('bread')).toBeInTheDocument()
  })

  it('renders section label', () => {
    render(<IngredientTags ingredients={['eggs']} />)
    expect(screen.getByText('Detected ingredients')).toBeInTheDocument()
  })

  it('renders nothing when ingredients list is empty', () => {
    const { container } = render(<IngredientTags ingredients={[]} />)
    expect(container.innerHTML).toBe('')
  })
})
