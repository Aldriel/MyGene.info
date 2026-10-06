// @vitest-environment jsdom
import { fireEvent, screen } from '@testing-library/react'
import { describe, expect, it, vi } from 'vitest'
import { renderWithI18n } from '../test/render.jsx'
import ExportMenu from './ExportMenu.jsx'

function renderMenu(props = {}) {
  const onExport = vi.fn()
  const view = renderWithI18n(<ExportMenu onExport={onExport} {...props} />)
  return { ...view, onExport }
}

describe('ExportMenu', () => {
  it('offers the three formats and exports the chosen one', async () => {
    const { user, onExport } = renderMenu()
    const button = screen.getByRole('button', { name: /Export/ })

    expect(button).toHaveAttribute('aria-expanded', 'false')
    await user.click(button)
    expect(button).toHaveAttribute('aria-expanded', 'true')
    expect(screen.getAllByRole('menuitem').map((item) => item.textContent)).toEqual([
      'CSV — spreadsheet',
      'JSON — reopenable results',
      'PDF — printable report',
    ])

    await user.click(screen.getByRole('menuitem', { name: /PDF/ }))

    expect(onExport).toHaveBeenCalledWith('pdf')
    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('closes on Escape and returns the focus to the button', async () => {
    const { user } = renderMenu()
    const button = screen.getByRole('button', { name: /Export/ })

    await user.click(button)
    await user.keyboard('{Escape}')

    expect(screen.queryByRole('menu')).toBeNull()
    expect(button).toHaveFocus()
  })

  it('closes on a click outside', async () => {
    const { user } = renderMenu()

    await user.click(screen.getByRole('button', { name: /Export/ }))
    fireEvent.pointerDown(document.body)

    expect(screen.queryByRole('menu')).toBeNull()
  })

  it('stays open when clicking inside', async () => {
    const { user } = renderMenu()

    await user.click(screen.getByRole('button', { name: /Export/ }))
    fireEvent.pointerDown(screen.getByRole('menu'))

    expect(screen.getByRole('menu')).toBeInTheDocument()
  })

  it('can be disabled', () => {
    renderMenu({ disabled: true })

    expect(screen.getByRole('button', { name: /Export/ })).toBeDisabled()
  })
})
