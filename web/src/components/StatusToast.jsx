import { useEffect } from 'react'

/** Delay before a confirmation disappears; errors stay until dismissed or replaced. */
export const TOAST_DURATION_MS = 5000

/**
 * Short notification at the bottom of the screen, announced by screen readers.
 *
 * @param {{status: {message: string, isError: boolean, id: number}|null,
 *   onDismiss: () => void, dismissLabel: string}} props
 */
export default function StatusToast({ status, onDismiss, dismissLabel }) {
  const id = status?.id
  const autoDismiss = Boolean(status) && !status.isError
  useEffect(() => {
    if (!autoDismiss) return undefined
    const timer = setTimeout(onDismiss, TOAST_DURATION_MS)
    return () => clearTimeout(timer)
  }, [id, autoDismiss, onDismiss])

  return (
    <div
      role={status?.isError ? 'alert' : 'status'}
      aria-live={status?.isError ? 'assertive' : 'polite'}
      className="pointer-events-none fixed inset-x-0 bottom-4 z-30 flex justify-center px-4"
    >
      {status && (
        <div
          className={`pointer-events-auto flex items-center gap-4 rounded-lg px-4 py-2.5 text-sm shadow-lg ${
            status.isError ? 'bg-red-700 text-white' : 'bg-slate-900 text-white'
          }`}
        >
          <span>{status.message}</span>
          <button
            type="button"
            onClick={onDismiss}
            aria-label={dismissLabel}
            className="text-white/70 hover:text-white"
          >
            ✕
          </button>
        </div>
      )}
    </div>
  )
}
