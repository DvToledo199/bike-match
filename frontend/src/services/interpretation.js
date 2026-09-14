import { ApiError, requestApi } from './apiClient.js'

const validSources = ['RULES', 'AI']

export function getBikeInterpretation(bikeId, language = 'en') {
  return requestApi(`/api/bikes/${bikeId}/interpretation?language=${encodeURIComponent(language)}`)
    .then(validateInterpretation)
}

export function generateBikeInterpretation(bikeId, language = 'en') {
  return requestApi(`/api/bikes/${bikeId}/interpretation?language=${encodeURIComponent(language)}`, {
    method: 'POST',
  }).then(validateInterpretation)
}

export function validateInterpretation(data) {
  const evidence = data?.evidence
  if (!data
    || !Number.isInteger(data.resultVersion)
    || !Number.isInteger(data.interpretationContextVersion)
    || typeof data.rulesVersion !== 'string'
    || typeof data.language !== 'string'
    || !validSources.includes(data.source)
    || typeof data.providerVersion !== 'string'
    || typeof data.summary !== 'string'
    || data.summary.trim().length === 0
    || data.summary.length > 1200
    || data.summary.includes('<')
    || data.summary.includes('>')
    || !Array.isArray(evidence)
    || evidence.length < 2
    || evidence.length > 4
    || evidence.some((item) => typeof item?.key !== 'string'
      || typeof item?.unit !== 'string'
      || !Number.isFinite(item?.value))) {
    throw new ApiError('invalidResponse')
  }
  return data
}
