const apiBaseUrl = import.meta.env?.VITE_API_URL ?? 'http://localhost:8080'

export class ApiError extends Error {
  constructor(kind, status) {
    super(kind)
    this.kind = kind
    this.status = status
  }
}

async function readResponseBody(response) {
  const contentType = response.headers.get('content-type') ?? ''

  if (!contentType.includes('json')) {
    throw new ApiError('invalidResponse')
  }

  try {
    const body = await response.json()
    if (!body || typeof body !== 'object') throw new ApiError('invalidResponse')
    return body
  } catch (error) {
    if (error.name === 'AbortError') throw error
    throw new ApiError('invalidResponse')
  }
}

function getErrorKind(response, body) {
  if (response.status === 400) {
    return 'invalidRequest'
  }

  if (response.status >= 500) {
    return 'server'
  }

  return body?.title ? 'unexpected' : 'unavailable'
}

export async function requestApi(path, options = {}) {
  const { signal, timeoutMs = 15000, ...fetchOptions } = options
  const controller = new AbortController()
  let timedOut = false
  const cancel = () => controller.abort()
  signal?.addEventListener('abort', cancel, { once: true })
  if (signal?.aborted) cancel()
  const timer = setTimeout(() => { timedOut = true; cancel() }, timeoutMs)
  try {
    const response = await fetch(`${apiBaseUrl}${path}`, {
      ...fetchOptions,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...options.headers,
      },
    })
    if (!response.ok) {
      throw new ApiError(getErrorKind(response, null), response.status)
    }
    return await readResponseBody(response)
  } catch (error) {
    if (controller.signal.aborted) throw new ApiError(timedOut ? 'timeout' : 'aborted')
    if (error instanceof ApiError) throw error
    throw new ApiError('network')
  } finally {
    clearTimeout(timer)
    signal?.removeEventListener('abort', cancel)
  }
}
