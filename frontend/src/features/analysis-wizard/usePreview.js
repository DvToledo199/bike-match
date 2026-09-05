import { useState } from 'react'
import { ApiError } from '../../services/apiClient.js'
import { requestKinematicsPreview } from '../../services/kinematicsPreview.js'

function getPreviewError(error) {
  if (error instanceof ApiError) {
    return { kind: error.kind }
  }

  return { kind: 'unexpected' }
}

function usePreview() {
  const [data, setData] = useState(null)
  const [error, setError] = useState(null)
  const [isLoading, setIsLoading] = useState(false)

  async function requestPreview(wizardData) {
    setIsLoading(true)
    setError(null)
    setData(null)

    try {
      const response = await requestKinematicsPreview(wizardData)
      setData(response)
      return response
    } catch (requestError) {
      setError(getPreviewError(requestError))
      return null
    } finally {
      setIsLoading(false)
    }
  }

  return {
    data,
    error,
    isLoading,
    requestPreview,
  }
}

export default usePreview
