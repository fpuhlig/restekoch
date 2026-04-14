interface Props {
  message: string
  onDismiss: () => void
}

export function ErrorMessage({ message, onDismiss }: Props) {
  return (
    <div className="error-banner" role="alert">
      <p>{message}</p>
      <button onClick={onDismiss} className="error-dismiss" aria-label="Dismiss error">
        Dismiss
      </button>
    </div>
  )
}
