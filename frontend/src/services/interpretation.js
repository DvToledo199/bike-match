import i18n from '../i18n.js'
import { ApiError, requestApi } from './apiClient.js'

const validSources = ['RULES', 'AI']

// Generating with an external provider takes longer than a normal request: the browser waits
// past the backend's worst case instead of reporting a failure while the server is still working.
const generateTimeoutMs = 45000

// The explanation follows the interface language unless the caller asks for another one.
function interfaceLanguage() {
  return i18n.resolvedLanguage ?? 'en'
}

export function getBikeInterpretation(bikeId, language = interfaceLanguage()) {
  return requestApi(`/api/bikes/${bikeId}/interpretation?language=${encodeURIComponent(language)}`)
    .then(validateInterpretation)
}

export function generateBikeInterpretation(bikeId, language = interfaceLanguage()) {
  return requestApi(`/api/bikes/${bikeId}/interpretation?language=${encodeURIComponent(language)}`, {
    method: 'POST',
    timeoutMs: generateTimeoutMs,
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
