import { describe, expect, it, vi } from 'vitest'
import { requestApi } from './apiClient.js'
import { validatePreview } from './previewValidation.js'

describe('API failures', () => {
  it.each([
    ['text/html', '<html>error</html>'],
    ['application/json', '{'],
    ['application/json', 'null'],
  ])('rejects successful HTTP with invalid body: %s %s', async (contentType, body) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(body, { headers: { 'content-type': contentType } })))
    await expect(requestApi('/test')).rejects.toMatchObject({ kind: 'invalidResponse' })
  })

  it.each([[400, 'invalidRequest'], [500, 'server'], [403, 'unavailable']])('explains HTTP %s even with an HTML error body', async (status, kind) => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response('error', { status })))
    await expect(requestApi('/test')).rejects.toMatchObject({ kind, status })
  })

  it('keeps the duplicate error from the registration API', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue(new Response(
      JSON.stringify({ detail: 'Email or username already exists' }),
      { status: 409, headers: { 'content-type': 'application/problem+json' } },
    )))

    await expect(requestApi('/api/auth/register')).rejects.toMatchObject({
      kind: 'duplicate',
      status: 409,
      detail: 'Email or username already exists',
    })
  })

  it('attaches the current session token to API calls', async () => {
    sessionStorage.setItem('bikematch.session', JSON.stringify({
      accessToken: 'session-token',
      tokenType: 'Bearer',
    }))
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}', {
      headers: { 'content-type': 'application/json' },
    }))
    vi.stubGlobal('fetch', fetchMock)

    await requestApi('/api/my-bikes')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://localhost:8080/api/my-bikes',
      expect.objectContaining({
        headers: {
          Accept: 'application/json',
          Authorization: 'Bearer session-token',
        },
      }),
    )
  })

  it('lets a specific request override the default authorization header', async () => {
    sessionStorage.setItem('bikematch.session', JSON.stringify({
      accessToken: 'session-token',
      tokenType: 'Bearer',
    }))
    const fetchMock = vi.fn().mockResolvedValue(new Response('{}', {
      headers: { 'content-type': 'application/json' },
    }))
    vi.stubGlobal('fetch', fetchMock)

    await requestApi('/test', { headers: { Authorization: 'Bearer other-token' } })

    expect(fetchMock.mock.calls[0][1].headers.Authorization).toBe('Bearer other-token')
  })

  it('times out an unresponsive server', async () => {
    vi.useFakeTimers()
    vi.stubGlobal('fetch', vi.fn((_url, { signal }) => new Promise((_resolve, reject) => {
      signal.addEventListener('abort', () => reject(new DOMException('Aborted', 'AbortError')))
    })))
    const assertion = expect(requestApi('/test', { timeoutMs: 50 })).rejects.toMatchObject({ kind: 'timeout' })
    await vi.advanceTimersByTimeAsync(51)
    await assertion
  })

  it.each([null, {}, { leverageCurve: [{ ratio: NaN }] }])('rejects an incomplete curve contract', (body) => {
    expect(() => validatePreview(body)).toThrow('invalidResponse')
  })
})
