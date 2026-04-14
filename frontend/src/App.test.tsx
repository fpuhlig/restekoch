import { describe, expect, it, vi, beforeEach } from 'vitest'
import { render, screen, fireEvent, waitFor } from '@testing-library/react'
import App from './App'

describe('App', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('renders header', () => {
    render(<App />)
    expect(screen.getByText('koch')).toBeInTheDocument()
    expect(screen.getByText('Scan your fridge. Cook what you have.')).toBeInTheDocument()
  })

  it('shows upload area in idle state', () => {
    render(<App />)
    expect(screen.getByText('Take a photo or drop an image')).toBeInTheDocument()
  })

  it('shows preview and scan button after selecting a file', () => {
    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(screen.getByText('Find recipes')).toBeInTheDocument()
    expect(screen.getByText('Choose different photo')).toBeInTheDocument()
    expect(screen.getByAltText('Selected photo')).toBeInTheDocument()
  })

  it('resets to idle when choosing different photo', () => {
    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    fireEvent.change(input, { target: { files: [file] } })

    fireEvent.click(screen.getByText('Choose different photo'))
    expect(screen.getByText('Take a photo or drop an image')).toBeInTheDocument()
  })

  it('shows loading spinner during scan', async () => {
    vi.spyOn(globalThis, 'fetch').mockReturnValue(new Promise((_resolve) => {}))

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    fireEvent.change(input, { target: { files: [file] } })
    fireEvent.click(screen.getByText('Find recipes'))

    expect(screen.getByText('Detecting ingredients...')).toBeInTheDocument()
  })

  it('shows results after successful scan', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          ingredients: ['eggs', 'cheese'],
          recipes: [
            {
              id: '1',
              title: 'Omelette',
              ingredients: ['eggs', 'cheese'],
              directions: ['Cook'],
              ner: ['eggs', 'cheese'],
            },
          ],
          explanation: 'A simple egg dish.',
          cached: false,
        }),
    } as Response)

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    fireEvent.change(input, { target: { files: [file] } })
    fireEvent.click(screen.getByText('Find recipes'))

    await waitFor(() => {
      expect(screen.getByText('Omelette')).toBeInTheDocument()
      expect(screen.getByText('A simple egg dish.')).toBeInTheDocument()
      expect(screen.getByText('Fresh')).toBeInTheDocument()
      expect(screen.getByText('Scan another photo')).toBeInTheDocument()
      expect(screen.getByText('1 recipes found')).toBeInTheDocument()
    })
  })

  it('shows Cached badge on cache hit', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          ingredients: ['eggs'],
          recipes: [
            { id: '1', title: 'Boiled Eggs', ingredients: [], directions: [], ner: ['eggs'] },
          ],
          explanation: 'Cached.',
          cached: true,
        }),
    } as Response)

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, {
      target: { files: [new File(['x'], 'p.jpg', { type: 'image/jpeg' })] },
    })
    fireEvent.click(screen.getByText('Find recipes'))

    await waitFor(() => {
      expect(screen.getByText('Cached')).toBeInTheDocument()
    })
  })

  it('shows error message on scan failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ message: 'Image file is required' }),
    } as Response)

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, {
      target: { files: [new File(['x'], 'p.jpg', { type: 'image/jpeg' })] },
    })
    fireEvent.click(screen.getByText('Find recipes'))

    await waitFor(() => {
      expect(screen.getByText('Image file is required')).toBeInTheDocument()
    })
  })

  it('dismisses error and returns to preview', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('not json')),
    } as Response)

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, {
      target: { files: [new File(['x'], 'p.jpg', { type: 'image/jpeg' })] },
    })
    fireEvent.click(screen.getByText('Find recipes'))

    await waitFor(() => {
      expect(screen.getByText('Scan failed (500)')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('Dismiss'))
    expect(screen.queryByText('Scan failed (500)')).not.toBeInTheDocument()
    expect(screen.getByText('Find recipes')).toBeInTheDocument()
  })

  it('shows empty state when no recipes found', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () =>
        Promise.resolve({
          ingredients: ['something rare'],
          recipes: [],
          explanation: '',
          cached: false,
        }),
    } as Response)

    render(<App />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(input, {
      target: { files: [new File(['x'], 'p.jpg', { type: 'image/jpeg' })] },
    })
    fireEvent.click(screen.getByText('Find recipes'))

    await waitFor(() => {
      expect(
        screen.getByText('No matching recipes found. Try a photo with different ingredients.'),
      ).toBeInTheDocument()
      expect(screen.getByText('0 recipes found')).toBeInTheDocument()
    })
  })
})
