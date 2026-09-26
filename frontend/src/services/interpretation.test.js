import { beforeEach, expect, it, vi } from 'vitest'

vi.mock('./apiClient.js', () => ({
  ApiError: class ApiError extends Error {
    constructor(kind) {
      super(kind)
      this.kind = kind
    }
  },
  requestApi: vi.fn(),
}))

import { requestApi } from './apiClient.js'
import { generateBikeInterpretation, getBikeInterpretation, validateInterpretation } from './interpretation.js'

const interpretation = {
  resultVersion: 1,
  interpretationContextVersion: 1,
  rulesVersion: 'kinematics-rules-1',
  language: 'en',
  source: 'RULES',
  providerVersion: 'rules-1',
  summary: 'This analysis shows a progressive response.',
  evidence: [
    { key: 'usefulProgressionPercent', value: 18, unit: '%' },
    { key: 'maxRearwardMm', value: 12, unit: 'mm' },
  ],
}

beforeEach(() => vi.clearAllMocks())

it('reads a saved interpretation without generating one', async () => {
  requestApi.mockResolvedValue(interpretation)

  await getBikeInterpretation(7)

  expect(requestApi).toHaveBeenCalledWith('/api/bikes/7/interpretation?language=en')
})

it('generates an interpretation only when explicitly requested', async () => {
  requestApi.mockResolvedValue(interpretation)

  await generateBikeInterpretation(7)

  expect(requestApi).toHaveBeenCalledWith(
    '/api/bikes/7/interpretation?language=en',
    { method: 'POST', timeoutMs: 75000 },
  )
})

it('rejects an incomplete interpretation response', () => {
  expect(() => validateInterpretation({ ...interpretation, evidence: [] })).toThrow('invalidResponse')
  expect(() => validateInterpretation({ ...interpretation, evidence: [null, null] })).toThrow('invalidResponse')
  expect(() => validateInterpretation({ ...interpretation, summary: '<script>' })).toThrow('invalidResponse')
  expect(() => validateInterpretation({ ...interpretation, source: 'UNKNOWN' })).toThrow('invalidResponse')
})
