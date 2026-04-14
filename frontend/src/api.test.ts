import { describe, expect, it, vi, beforeEach } from 'vitest'
import { scanImage } from './api'

describe('scanImage', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('sends POST with FormData and returns ScanResponse', async () => {
    const mockResponse = {
      ingredients: ['eggs', 'cheese'],
      recipes: [],
      explanation: 'Test',
      cached: false,
    }

    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve(mockResponse),
    } as Response)

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    const result = await scanImage(file)

    expect(result.ingredients).toEqual(['eggs', 'cheese'])
    expect(result.cached).toBe(false)

    const call = vi.mocked(fetch).mock.calls[0]
    expect(call[0]).toBe('/api/scan?limit=5')
    expect(call[1]?.method).toBe('POST')
    expect(call[1]?.body).toBeInstanceOf(FormData)
  })

  it('passes custom limit parameter', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ingredients: [], recipes: [], explanation: '', cached: false }),
    } as Response)

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await scanImage(file, 10)

    const call = vi.mocked(fetch).mock.calls[0]
    expect(call[0]).toBe('/api/scan?limit=10')
  })

  it('throws on non-ok response with error message from body', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 400,
      json: () => Promise.resolve({ message: 'Image file is required' }),
    } as Response)

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await expect(scanImage(file)).rejects.toThrow('Image file is required')
  })

  it('throws generic message when error body is not JSON', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: false,
      status: 500,
      json: () => Promise.reject(new Error('not json')),
    } as Response)

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await expect(scanImage(file)).rejects.toThrow('Scan failed (500)')
  })

  it('throws network error message on fetch failure', async () => {
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new TypeError('Failed to fetch'))

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await expect(scanImage(file)).rejects.toThrow(
      'Network error. Check your connection and try again.',
    )
  })

  it('throws cancel message on AbortError', async () => {
    const abortError = new DOMException('The operation was aborted.', 'AbortError')
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(abortError)

    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await expect(scanImage(file)).rejects.toThrow('Scan was cancelled.')
  })

  it('passes abort signal to fetch', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      json: () => Promise.resolve({ ingredients: [], recipes: [], explanation: '', cached: false }),
    } as Response)

    const controller = new AbortController()
    const file = new File(['fake'], 'photo.jpg', { type: 'image/jpeg' })
    await scanImage(file, 5, controller.signal)

    const call = vi.mocked(fetch).mock.calls[0]
    expect(call[1]?.signal).toBeDefined()
  })
})
