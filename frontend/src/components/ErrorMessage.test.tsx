import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { ErrorMessage } from './ErrorMessage'

describe('ErrorMessage', () => {
  it('renders error text', () => {
    render(<ErrorMessage message="Something went wrong" onDismiss={vi.fn()} />)
    expect(screen.getByText('Something went wrong')).toBeInTheDocument()
  })

  it('has alert role for accessibility', () => {
    render(<ErrorMessage message="Error" onDismiss={vi.fn()} />)
    expect(screen.getByRole('alert')).toBeInTheDocument()
  })

  it('calls onDismiss when dismiss button is clicked', () => {
    const onDismiss = vi.fn()
    render(<ErrorMessage message="Error" onDismiss={onDismiss} />)

    fireEvent.click(screen.getByText('Dismiss'))
    expect(onDismiss).toHaveBeenCalledOnce()
  })
})
