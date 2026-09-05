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
    return null
  }

  return response.json().catch(() => null)
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
  let response

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...options,
      headers: {
        Accept: 'application/json',
        ...options.headers,
      },
    })
  } catch {
    throw new ApiError('network')
  }

  const body = await readResponseBody(response)

  if (!response.ok) {
    throw new ApiError(getErrorKind(response, body), response.status)
  }

  return body
}
