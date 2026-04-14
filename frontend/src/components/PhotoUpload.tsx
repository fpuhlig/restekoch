import { useCallback, useRef, useState } from 'react'

const MAX_SIZE = 10 * 1024 * 1024
const ACCEPTED_TYPES = ['image/jpeg', 'image/png', 'image/webp']

interface Props {
  onSelect: (file: File, preview: string) => void
  onError: (message: string) => void
  disabled: boolean
}

export function PhotoUpload({ onSelect, onError, disabled }: Props) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [dragging, setDragging] = useState(false)

  const validateAndSelect = useCallback(
    (file: File) => {
      if (!file.type.startsWith('image/')) {
        onError('Please select an image file (JPG, PNG, or WebP).')
        return
      }

      if (!ACCEPTED_TYPES.includes(file.type)) {
        onError(`Unsupported image format: ${file.type}. Use JPG, PNG, or WebP.`)
        return
      }

      if (file.size > MAX_SIZE) {
        const sizeMB = (file.size / 1024 / 1024).toFixed(1)
        onError(`Image is too large (${sizeMB} MB). Maximum is 10 MB.`)
        return
      }

      if (file.size === 0) {
        onError('Image file is empty.')
        return
      }

      const url = URL.createObjectURL(file)
      onSelect(file, url)
    },
    [onSelect, onError],
  )

  const handleDrop = useCallback(
    (e: React.DragEvent) => {
      e.preventDefault()
      setDragging(false)
      const file = e.dataTransfer.files[0]
      if (file) {
        validateAndSelect(file)
      }
    },
    [validateAndSelect],
  )

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const file = e.target.files?.[0]
      if (file) {
        validateAndSelect(file)
      }
    },
    [validateAndSelect],
  )

  return (
    <div
      className={`upload-area ${dragging ? 'dragging' : ''}`}
      onClick={() => !disabled && inputRef.current?.click()}
      onDragOver={(e) => {
        e.preventDefault()
        setDragging(true)
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={handleDrop}
      role="button"
      tabIndex={0}
      aria-label="Upload a photo of your fridge"
      onKeyDown={(e) => {
        if (e.key === 'Enter' || e.key === ' ') {
          inputRef.current?.click()
        }
      }}
    >
      <span className="upload-icon" aria-hidden="true">
        &#128247;
      </span>
      <p>Take a photo or drop an image</p>
      <p className="hint">JPG, PNG, WebP up to 10 MB</p>
      <input
        ref={inputRef}
        type="file"
        accept="image/jpeg,image/png,image/webp"
        capture="environment"
        onChange={handleChange}
        disabled={disabled}
        hidden
        aria-hidden="true"
      />
    </div>
  )
}
