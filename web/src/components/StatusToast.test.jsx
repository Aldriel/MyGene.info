// @vitest-environment jsdom
import { act, fireEvent, render, screen } from '@testing-library/react'
import { afterEach, describe, expect, it, vi } from 'vitest'
import StatusToast, { TOAST_DURATION_MS } from './StatusToast.jsx'

afterEach(() => {
  vi.useRealTimers()
})

describe('StatusToast', () => {
  it('announces a confirmation and hides it after a delay', () => {
    vi.useFakeTimers()
    const onDismiss = vi.fn()
    render(
      <StatusToast
        status={{ message: 'Saved.', isError: false, id: 1 }}
        onDismiss={onDismiss}
        dismissLabel="Dismiss"
      />,
    )

    expect(screen.getByRole('status')).toHaveTextContent('Saved.')
    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS - 1))
    expect(onDismiss).not.toHaveBeenCalled()
    act(() => vi.advanceTimersByTime(1))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('restarts the delay for a new message, not on every render', () => {
    vi.useFakeTimers()
    const onDismiss = vi.fn()
    const toast = (id) => (
      <StatusToast
        status={{ message: `m${id}`, isError: false, id }}
        onDismiss={onDismiss}
        dismissLabel="Dismiss"
      />
    )
    const { rerender } = render(toast(1))

    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS - 100))
    rerender(toast(1))
    act(() => vi.advanceTimersByTime(100))
    expect(onDismiss).toHaveBeenCalledOnce()

    rerender(toast(2))
    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS - 1))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('keeps errors until dismissed', () => {
    vi.useFakeTimers()
    const onDismiss = vi.fn()
    render(
      <StatusToast
        status={{ message: 'Failed.', isError: true, id: 1 }}
        onDismiss={onDismiss}
        dismissLabel="Dismiss"
      />,
    )

    expect(screen.getByRole('alert')).toHaveTextContent('Failed.')
    act(() => vi.advanceTimersByTime(TOAST_DURATION_MS * 3))
    expect(onDismiss).not.toHaveBeenCalled()

    fireEvent.click(screen.getByRole('button', { name: 'Dismiss' }))
    expect(onDismiss).toHaveBeenCalledOnce()
  })

  it('renders an empty live region without status', () => {
    render(<StatusToast status={null} onDismiss={() => {}} dismissLabel="Dismiss" />)

    expect(screen.getByRole('status')).toBeEmptyDOMElement()
  })
})
