import { beforeEach, expect, it } from 'vitest'
import { authHeaders, clearSession, getSession, saveSession } from './session.js'

const token = `header.${btoa(JSON.stringify({ username: 'david', role: 'USER' }))}.signature`

beforeEach(() => {
  sessionStorage.clear()
})

it('saves the token and the non-sensitive display claims for the browser session', () => {
  const session = saveSession({ accessToken: token, tokenType: 'Bearer' })

  expect(session).toMatchObject({ username: 'david', role: 'USER' })
  expect(getSession()).toEqual(session)
  expect(authHeaders()).toEqual({ Authorization: `Bearer ${token}` })
})

it('clears the session on logout', () => {
  saveSession({ accessToken: token, tokenType: 'Bearer' })

  clearSession()

  expect(getSession()).toBeNull()
  expect(authHeaders()).toEqual({})
})
