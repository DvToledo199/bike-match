import { afterEach, describe, expect, it } from 'vitest'
import { fireEvent, render, screen } from '@testing-library/react'
import i18n from '../i18n.js'
import Layout from './Layout.jsx'

describe('LanguageSwitcher', () => {
  afterEach(async () => {
    await i18n.changeLanguage('en')
    localStorage.clear()
  })

  it('switches the interface to Spanish and remembers the choice', () => {
    render(<Layout screen="home" onLogout={() => {}}>content</Layout>)

    fireEvent.click(screen.getByRole('button', { name: 'Español' }))

    expect(screen.getByRole('link', { name: 'Catálogo' })).toBeTruthy()
    expect(screen.getByRole('button', { name: 'Español' }).getAttribute('aria-pressed')).toBe('true')
    expect(document.documentElement.lang).toBe('es')
    expect(localStorage.getItem('i18nextLng')).toBe('es')
  })

  it('switches back to English', () => {
    render(<Layout screen="home" onLogout={() => {}}>content</Layout>)

    fireEvent.click(screen.getByRole('button', { name: 'Español' }))
    fireEvent.click(screen.getByRole('button', { name: 'English' }))

    expect(screen.getByRole('link', { name: 'Catalog' })).toBeTruthy()
    expect(document.documentElement.lang).toBe('en')
  })
})
