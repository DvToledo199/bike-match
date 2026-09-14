import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import LoginForm from './LoginForm.jsx'

vi.mock('../../services/authLogin.js', () => ({
  loginUser: vi.fn(),
}))

vi.mock('../../services/session.js', () => ({
  saveSession: vi.fn(() => ({ username: 'david', role: 'USER', accessToken: 'token' })),
}))

import { loginUser } from '../../services/authLogin.js'

function fillValidForm() {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'david@example.com' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'BikeMatch8!' } })
}

it('validates required login fields before calling the API', () => {
  render(<LoginForm onLoggedIn={vi.fn()} onContinueAsGuest={vi.fn()} />)

  fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

  expect(screen.getByText('Email is required.')).toBeTruthy()
  expect(loginUser).not.toHaveBeenCalled()
})

it('stores the returned session and enters the application', async () => {
  const onLoggedIn = vi.fn()
  loginUser.mockResolvedValue({ accessToken: 'token', tokenType: 'Bearer' })
  render(<LoginForm onLoggedIn={onLoggedIn} onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

  await waitFor(() => expect(onLoggedIn).toHaveBeenCalledWith(expect.objectContaining({ role: 'USER' })))
})

it('explains invalid credentials', async () => {
  loginUser.mockRejectedValue({ kind: 'invalidCredentials', status: 401 })
  render(<LoginForm onLoggedIn={vi.fn()} onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Log in' }))

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('email or password is incorrect'))
})
