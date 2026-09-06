import { useEffect, useRef, useState } from 'react'
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
  const activeRequest = useRef(null)

  useEffect(() => () => activeRequest.current?.abort(), [])

  function cancelPreview() {
    activeRequest.current?.abort()
    activeRequest.current = null
    setIsLoading(false)
    setData(null)
    setError(null)
  }

  async function requestPreview(wizardData) {
    activeRequest.current?.abort()
    const controller = new AbortController()
    activeRequest.current = controller
    setIsLoading(true)
    setError(null)
    setData(null)

    try {
      const response = await requestKinematicsPreview(wizardData, controller.signal)
      if (controller.signal.aborted || activeRequest.current !== controller) return null
      setData(response)
      return response
    } catch (requestError) {
      if (controller.signal.aborted || activeRequest.current !== controller) return null
      setError(getPreviewError(requestError))
      return null
    } finally {
      if (!controller.signal.aborted && activeRequest.current === controller) setIsLoading(false)
    }
  }

  return {
    data,
    error,
    isLoading,
    requestPreview,
    cancelPreview,
  }
}

export default usePreview
