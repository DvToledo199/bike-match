import { expect, it, vi } from 'vitest'
import { requestKinematicsPreview } from './kinematicsPreview.js'
import { validatePreview } from './previewValidation.js'
import { referencePreview } from '../test/referencePreview.js'

it('sends the selected layout and explicit wheel preset without inventing rider weight', async () => {
  const fetch = vi.fn().mockResolvedValue(new Response(JSON.stringify(referencePreview()), {
    headers: { 'content-type': 'application/json' },
  }))
  vi.stubGlobal('fetch', fetch)
  await requestKinematicsPreview({ suspensionLayout: 'HORST_LINK', points: {}, parameters: { eyeToEyeMm: '210', shockStrokeMm: '55',
    chainringTeeth: '32', sprocketTeeth: '50', declaredTravelMm: '150', sagPercent: '30', wheelConfiguration: 'MULLET' } })
  const body = JSON.parse(fetch.mock.calls[0][1].body)
  expect(body.suspensionLayout).toBe('HORST_LINK')
  expect(body.parameters).toEqual({ shockStrokeMm: 55, chainringTeeth: 32, sprocketTeeth: 50,
    declaredTravelMm: 150, sagPercent: 30, wheelConfiguration: 'MULLET' })
})

it('accepts finite negative response percentages', () => {
  expect(validatePreview(referencePreview()).antiSquatCurve[1].percent).toBe(-10.2)
})

it.each(['horst-link-reference-v1', 'horst-link-yoke-reference-v2'])('accepts %s only with its seatstay brake model', (version) => {
  const horstPreview = referencePreview()
  horstPreview.conditions.modelVersion = version
  horstPreview.conditions.reference.brakeModel = 'SEATSTAY_FIXED'

  expect(validatePreview(horstPreview)).toBe(horstPreview)
  horstPreview.conditions.reference.brakeModel = 'SWINGARM_FIXED'
  expect(() => validatePreview(horstPreview)).toThrow('invalidResponse')
})

it('rejects old or incomplete reference responses before rendering', () => {
  for (const mutate of [
    (data) => { data.conditions.modelVersion = 'monopivot-v1' },
    (data) => { data.conditions.modelVersion = 'horst-link-yoke-reference-v1' },
    (data) => { data.conditions.modelVersion = 'horst-link-reference-v1' },
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
