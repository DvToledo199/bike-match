import { fireEvent, render, screen, waitFor } from '@testing-library/react'
import { expect, it, vi } from 'vitest'
import RegisterForm from './RegisterForm.jsx'

vi.mock('../../services/auth.js', () => ({
  registerUser: vi.fn(),
}))

import { registerUser } from '../../services/auth.js'

function fillValidForm() {
  fireEvent.change(screen.getByLabelText('Email'), { target: { value: 'david@example.com' } })
  fireEvent.change(screen.getByLabelText('Username'), { target: { value: 'david' } })
  fireEvent.change(screen.getByLabelText('Password'), { target: { value: 'BikeMatch8!' } })
}

it('validates the registration form before calling the API', () => {
  render(<RegisterForm onContinueAsGuest={vi.fn()} />)

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  expect(screen.getAllByText('Email is required.').length).toBeGreaterThan(0)
  expect(registerUser).not.toHaveBeenCalled()
})

it('registers a valid account and shows the success state', async () => {
  registerUser.mockResolvedValue({ id: 7, username: 'david' })
  render(<RegisterForm onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  await waitFor(() => expect(screen.getByRole('status').textContent).toContain('Your account was created'))
  expect(registerUser).toHaveBeenCalledWith({
    email: 'david@example.com',
    username: 'david',
    password: 'BikeMatch8!',
  })
})

it('explains duplicate accounts', async () => {
  registerUser.mockRejectedValue({ kind: 'duplicate', status: 409 })
  render(<RegisterForm onContinueAsGuest={vi.fn()} />)
  fillValidForm()

  fireEvent.click(screen.getByRole('button', { name: 'Create account' }))

  await waitFor(() => expect(screen.getByRole('alert').textContent).toContain('already in use'))
})
