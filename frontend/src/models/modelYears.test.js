import { expect, it } from 'vitest'
import { OLDEST_MODEL_YEAR, modelYearOptions } from './modelYears.js'

it('starts a year ahead of today and goes back to the oldest year, newest first', () => {
  const years = modelYearOptions(new Date(2026, 8, 28))
  expect(years[0]).toBe(2027)
  expect(years.at(-1)).toBe(OLDEST_MODEL_YEAR)
  expect(years).toHaveLength(2027 - OLDEST_MODEL_YEAR + 1)
  expect(years).toEqual([...years].sort((a, b) => b - a))
})

it('moves on by itself when the year changes', () => {
  expect(modelYearOptions(new Date(2030, 0, 1))[0]).toBe(2031)
})
