import { expect, it, vi } from 'vitest'
import { requestKinematicsPreview } from './kinematicsPreview.js'
import { validatePreview } from './previewValidation.js'
import { referencePreview } from '../test/referencePreview.js'

it('sends the explicit wheel preset without inventing rider weight', async () => {
  const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(referencePreview()), {
    headers: { 'content-type': 'application/json' },
  }))
  vi.stubGlobal('fetch', fetch)
  await requestKinematicsPreview({ points: {}, parameters: { eyeToEyeMm: '210', shockStrokeMm: '55',
    chainringTeeth: '32', sprocketTeeth: '50', declaredTravelMm: '150', sagPercent: '30', wheelConfiguration: 'MULLET' } })
  const body = JSON.parse(fetch.mock.calls[0][1].body)
  expect(body.parameters).toEqual({ shockStrokeMm: 55, chainringTeeth: 32, sprocketTeeth: 50,
    declaredTravelMm: 150, sagPercent: 30, wheelConfiguration: 'MULLET' })
})

it('accepts finite negative response percentages', () => {
  expect(validatePreview(referencePreview()).antiSquatCurve[1].percent).toBe(-10.2)
})

it('rejects old or incomplete reference responses before rendering', () => {
  for (const mutate of [
    (data) => { data.conditions.modelVersion = 'monopivot-v1' },
    (data) => { delete data.antiRiseCurve },
    (data) => { data.antiSquatCurve[0].percent = Infinity },
    (data) => { data.conditions.reference.centerOfGravityHeightMm = 0 },
    (data) => { data.conditions.reference.wheelConfiguration = 'FULL_26' },
    (data) => { data.conditions.reference.brakeModel = 'FLOATING' },
  ]) {
    const data = referencePreview()
    mutate(data)
    expect(() => validatePreview(data)).toThrow('invalidResponse')
  }
})
