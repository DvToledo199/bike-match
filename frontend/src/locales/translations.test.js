import { describe, expect, it } from 'vitest'
import en from './en/translation.json'
import es from './es/translation.json'

function flatten(tree, prefix = '') {
  return Object.entries(tree).flatMap(([key, value]) => (
    typeof value === 'object'
      ? flatten(value, `${prefix}${key}.`)
      : [[`${prefix}${key}`, value]]
  ))
}

function placeholders(text) {
  return [...text.matchAll(/{{\s*(\w+)\s*}}/g)].map((match) => match[1]).sort()
}

const english = Object.fromEntries(flatten(en))
const spanish = Object.fromEntries(flatten(es))

describe('translations', () => {
  it('Spanish has exactly the English keys, so no text is left untranslated', () => {
    expect(Object.keys(spanish).sort()).toEqual(Object.keys(english).sort())
  })

  it('every Spanish text keeps the values the interface fills in', () => {
    for (const [key, text] of Object.entries(english)) {
      expect(placeholders(spanish[key]), key).toEqual(placeholders(text))
    }
  })

  it('no Spanish text is empty', () => {
    for (const [key, text] of Object.entries(spanish)) {
      expect(text.trim(), key).not.toBe('')
    }
  })
})
