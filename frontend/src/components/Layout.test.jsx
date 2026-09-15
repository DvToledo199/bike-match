import { render, screen } from '@testing-library/react'
import { afterEach, expect, it } from 'vitest'
import { readFileSync } from 'node:fs'
import Layout from './Layout.jsx'
import styles from './Layout.module.css'

const css = readFileSync('src/components/Layout.module.css', 'utf8')

let styleElement
afterEach(() => { styleElement?.remove() })

it.each([['home', false], ['analysis', false], ['home', true], ['analysis', true]])('keeps the analysis link foreground on %s (hover: %s)', (currentScreen, hovered) => {
  // Model hover with an attribute because jsdom has no pointer rendering.
  styleElement = document.createElement('style')
  styleElement.textContent = css.replace(/\.([a-zA-Z_][a-zA-Z0-9_-]*)/g, (selector, name) => (
    styles[name] ? `.${styles[name]}` : selector
  )).replaceAll(':hover', '[data-test-hover]')
  document.head.append(styleElement)
  render(<Layout screen={currentScreen} />)
  const link = screen.getByRole('link', { name: 'Analyze your bike' })
  if (hovered) link.setAttribute('data-test-hover', '')
  expect(link.getAttribute('href')).toBe('#/analyze')
  expect(getComputedStyle(link).color).toBe('var(--color-on-primary)')
  const accentRules = [...styleElement.sheet.cssRules].filter((rule) => rule.style?.color === 'var(--color-primary)')
  expect(accentRules.some((rule) => link.matches(rule.selectorText))).toBe(false)
})
