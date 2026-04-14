import { describe, expect, it, vi } from 'vitest'
import { render, screen, fireEvent } from '@testing-library/react'
import { PhotoUpload } from './PhotoUpload'

describe('PhotoUpload', () => {
  it('renders upload prompt', () => {
    render(<PhotoUpload onSelect={vi.fn()} onError={vi.fn()} disabled={false} />)
    expect(screen.getByText('Take a photo or drop an image')).toBeInTheDocument()
  })

  it('renders file size hint', () => {
    render(<PhotoUpload onSelect={vi.fn()} onError={vi.fn()} disabled={false} />)
    expect(screen.getByText('JPG, PNG, WebP up to 10 MB')).toBeInTheDocument()
  })

  it('calls onSelect with valid image file', () => {
    const onSelect = vi.fn()
    render(<PhotoUpload onSelect={onSelect} onError={vi.fn()} disabled={false} />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake-image-data'], 'photo.jpg', { type: 'image/jpeg' })
    Object.defineProperty(file, 'size', { value: 1024 })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onSelect).toHaveBeenCalledOnce()
    expect(onSelect.mock.calls[0][0]).toBe(file)
    expect(onSelect.mock.calls[0][1]).toMatch(/^blob:/)
  })

  it('rejects non-image files with error', () => {
    const onError = vi.fn()
    render(<PhotoUpload onSelect={vi.fn()} onError={onError} disabled={false} />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'doc.pdf', { type: 'application/pdf' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onError).toHaveBeenCalledWith('Please select an image file (JPG, PNG, or WebP).')
  })

  it('rejects unsupported image formats', () => {
    const onError = vi.fn()
    render(<PhotoUpload onSelect={vi.fn()} onError={onError} disabled={false} />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['fake'], 'photo.bmp', { type: 'image/bmp' })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onError).toHaveBeenCalledWith(
      'Unsupported image format: image/bmp. Use JPG, PNG, or WebP.',
    )
  })

  it('rejects files over 10 MB', () => {
    const onError = vi.fn()
    render(<PhotoUpload onSelect={vi.fn()} onError={onError} disabled={false} />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File(['x'], 'huge.jpg', { type: 'image/jpeg' })
    Object.defineProperty(file, 'size', { value: 11 * 1024 * 1024 })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onError).toHaveBeenCalledWith('Image is too large (11.0 MB). Maximum is 10 MB.')
  })

  it('rejects empty files', () => {
    const onError = vi.fn()
    render(<PhotoUpload onSelect={vi.fn()} onError={onError} disabled={false} />)

    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const file = new File([], 'empty.jpg', { type: 'image/jpeg' })
    Object.defineProperty(file, 'size', { value: 0 })

    fireEvent.change(input, { target: { files: [file] } })

    expect(onError).toHaveBeenCalledWith('Image file is empty.')
  })

  it('has correct accept attribute', () => {
    render(<PhotoUpload onSelect={vi.fn()} onError={vi.fn()} disabled={false} />)
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    expect(input.accept).toBe('image/jpeg,image/png,image/webp')
  })

  it('has accessible role and label', () => {
    render(<PhotoUpload onSelect={vi.fn()} onError={vi.fn()} disabled={false} />)
    const area = screen.getByRole('button', { name: 'Upload a photo of your fridge' })
    expect(area).toBeInTheDocument()
  })

  it('opens file picker on Enter key', () => {
    render(<PhotoUpload onSelect={vi.fn()} onError={vi.fn()} disabled={false} />)
    const area = screen.getByRole('button', { name: 'Upload a photo of your fridge' })
    const input = document.querySelector('input[type="file"]') as HTMLInputElement
    const clickSpy = vi.spyOn(input, 'click')

    fireEvent.keyDown(area, { key: 'Enter' })
    expect(clickSpy).toHaveBeenCalled()
  })
})
