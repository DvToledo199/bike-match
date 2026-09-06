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
