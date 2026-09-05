import { requestApi } from './apiClient.js'

function toNumber(value) {
  return Number(value)
}

export function requestKinematicsPreview(wizardData) {
  const { parameters, points } = wizardData

  return requestApi('/api/kinematics/preview', {
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
}
