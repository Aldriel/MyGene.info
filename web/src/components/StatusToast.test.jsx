/*
 * Copyright 2026 Maxime Ethier - Consultant en Bioinformatique/Biocomputing Consultant
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
