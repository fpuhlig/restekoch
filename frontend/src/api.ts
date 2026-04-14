import type { ScanResponse } from './types'

const TIMEOUT_MS = 30_000

export async function scanImage(
  file: File,
  limit = 5,
  signal?: AbortSignal,
): Promise<ScanResponse> {
  const form = new FormData()
  form.append('image', file)

  const controller = new AbortController()
  const mergedSignal = signal ?? controller.signal

  const timeout = setTimeout(() => controller.abort(), TIMEOUT_MS)

  let response: Response
  try {
    response = await fetch(`/api/scan?limit=${limit}`, {
      method: 'POST',
      body: form,
      signal: mergedSignal,
    })
  } catch (err) {
    clearTimeout(timeout)
    if (err instanceof DOMException && err.name === 'AbortError') {
      throw new Error('Scan was cancelled.')
    }
    throw new Error('Network error. Check your connection and try again.')
  }

  clearTimeout(timeout)

  if (!response.ok) {
    const body = await response.json().catch(() => null)
    const message = body?.message ?? `Scan failed (${response.status})`
    throw new Error(message)
  }

  return response.json()
}
