import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { beforeEach, expect, it, vi } from 'vitest'
import RegisterForm from './RegisterForm.jsx'

vi.mock('../../services/auth.js', () => ({
  registerUser: vi.fn(),
}))

vi.mock('../../services/authLogin.js', () => ({
  loginUser: vi.fn(),
}))

vi.mock('../../services/session.js', () => ({
  saveSession: vi.fn(() => ({ username: 'david', role: 'USER', accessToken: 'token' })),
}))

import { registerUser } from '../../services/auth.js'
import { loginUser } from '../../services/authLogin.js'

beforeEach(() => {
  registerUser.mockReset()
  loginUser.mockReset()
})

function fillValidForm() {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'david@example.com' } })
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'david' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'BikeMatch8!' } })
}

it('validates the registration form before calling the API', () => {
  render(<RegisterForm onRegistered={vi.fn()} onContinueAsGuest={vi.fn()} />)

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  expect(screen.getAllByText('Email is required.').length).toBeGreaterThan(0)
  expect(registerUser).not.toHaveBeenCalled()
})

it('registers a valid account and signs the new user in', async () => {
  registerUser.mockResolvedValue({ id: 7, username: 'david' })
  loginUser.mockResolvedValue({ accessToken: 'token', tokenType: 'Bearer' })
  const onRegistered = vi.fn()
  render(<RegisterForm onRegistered={onRegistered} onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  await waitFor(() => expect(onRegistered).toHaveBeenCalledWith({ username: 'david', role: 'USER', accessToken: 'token' }))
  expect(registerUser).toHaveBeenCalledWith({
    email: 'david@example.com',
    username: 'david',
    password: 'BikeMatch8!',
  })
  expect(loginUser).toHaveBeenCalledWith({ email: 'david@example.com', password: 'BikeMatch8!' })
})

it('offers the manual login when the automatic sign-in fails', async () => {
  registerUser.mockResolvedValue({ id: 7, username: 'david' })
  loginUser.mockRejectedValue({ kind: 'network' })
  const onRegistered = vi.fn()
  render(<RegisterForm onRegistered={onRegistered} onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  await waitFor(() => expect(screen.getByRole('status').textContent).toContain('Your account was created'))
  expect(screen.getByRole('link', { name: 'Log in' }).getAttribute('href')).toBe('#/login')
  expect(onRegistered).not.toHaveBeenCalled()
})

it('explains duplicate accounts', async () => {
  registerUser.mockRejectedValue({ kind: 'duplicate', status: 409 })
  render(<RegisterForm onRegistered={vi.fn()} onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('already in use'))
  expect(loginUser).not.toHaveBeenCalled()
})
