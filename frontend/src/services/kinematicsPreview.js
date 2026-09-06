import { requestApi } from './apiClient.js'
import { validatePreview } from './previewValidation.js'

function toNumber(value) {
  return Number(value)
}

export async function requestKinematicsPreview(wizardData, signal) {
  const { parameters, points } = wizardData

  const response = await requestApi('/api/kinematics/preview', {
    signal,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
    },
    body: JSON.stringify({
      points: Object.values(points).map(({ type, x, y }) => ({ type, x, y })),
      eyeToEyeMm: toNumber(parameters.eyeToEyeMm),
      parameters: {
        shockStrokeMm: toNumber(parameters.shockStrokeMm),
        chainringTeeth: toNumber(parameters.chainringTeeth),
        sprocketTeeth: toNumber(parameters.sprocketTeeth),
        declaredTravelMm: toNumber(parameters.declaredTravelMm),
        sagPercent: toNumber(parameters.sagPercent),
      },
    }),
  })
  return validatePreview(response)
}
