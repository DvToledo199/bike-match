import { ApiError } from './apiClient.js'

const finiteFields = (value, keys) => value && keys.every((key) => Number.isFinite(value[key]))
const validCurve = (curve, keys) => Array.isArray(curve) && curve.length >= 2 && curve.length <= 1000
  && curve.every((sample) => finiteFields(sample, keys))

// Check the API contract before handing numbers to charts or interpretation badges.
export function validatePreview(data) {
  const leverage = data?.leverageDescriptors
  const trends = ['PROGRESSIVE', 'LINEAR', 'REGRESSIVE']
  const bands = ['REGRESSIVE', 'LINEAR', 'SLIGHTLY_PROGRESSIVE', 'MEDIUM', 'HIGH', 'VERY_HIGH']
  if (!validCurve(data?.leverageCurve, ['wheelTravelMm', 'ratio'])
    || !data.leverageCurve.every((sample) => sample.ratio > 0)
    || !validCurve(data?.kickbackCurve, ['wheelTravelMm', 'kickbackDegrees'])
    || !validCurve(data?.axlePath, ['x', 'y'])
    || !finiteFields(leverage, ['lrInitial', 'lrAtSag', 'lrFinal', 'lrMean',
      'totalProgressionPercent', 'usefulProgressionPercent', 'slopeInitialToSag', 'slopeSagToEnd'])
    || !['initialTrend', 'middleTrend', 'finalTrend'].every((key) => trends.includes(leverage[key]))
    || !bands.includes(leverage.progressionBand)
    || !finiteFields(data?.axlePathDescriptors, ['maxRearwardMm', 'atTravelPercent'])
    || !finiteFields(data?.travelCheck, ['calculatedTravelMm', 'declaredTravelMm', 'deviationPercent'])
    || typeof data.travelCheck.withinTolerance !== 'boolean'
    || !finiteFields(data?.conditions, ['sagPercent', 'chainringTeeth', 'sprocketTeeth'])) {
    throw new ApiError('invalidResponse')
  }
  return data
}
