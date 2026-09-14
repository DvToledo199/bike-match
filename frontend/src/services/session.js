const sessionKey = 'bikematch.session'

function decodeClaims(accessToken) {
  const encodedPayload = accessToken.split('.')[1]
  if (!encodedPayload) throw new Error('invalidToken')

  const paddedPayload = encodedPayload.replace(/-/g, '+').replace(/_/g, '/')
    .padEnd(Math.ceil(encodedPayload.length / 4) * 4, '=')
  return JSON.parse(atob(paddedPayload))
}

export function saveSession(loginResponse) {
  if (!loginResponse?.accessToken) throw new Error('invalidToken')

  const claims = decodeClaims(loginResponse.accessToken)
  const session = {
    accessToken: loginResponse.accessToken,
    tokenType: loginResponse.tokenType ?? 'Bearer',
    username: claims.username,
    role: claims.role,
  }
  sessionStorage.setItem(sessionKey, JSON.stringify(session))
  return session
}

export function getSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(sessionKey))
    return session?.accessToken ? session : null
  } catch {
    return null
  }
}

export function clearSession() {
  sessionStorage.removeItem(sessionKey)
}

export function authHeaders() {
  const session = getSession()
  return session
    ? { Authorization: `${session.tokenType} ${session.accessToken}` }
    : {}
}
