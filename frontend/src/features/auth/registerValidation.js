const usernamePattern = /^[A-Za-z0-9_]+$/

export function validateRegisterForm(values) {
  const errors = {}

  if (!values.email.trim()) errors.email = 'required'
  else if (!/^\S+@\S+\.\S+$/.test(values.email.trim())) errors.email = 'email'

  if (!values.username.trim()) errors.username = 'required'
  else if (values.username.trim().length < 3 || values.username.trim().length > 30) errors.username = 'usernameLength'
  else if (!usernamePattern.test(values.username.trim())) errors.username = 'usernameCharacters'

  if (!values.password) errors.password = 'required'
  else if (values.password.length < 8 || values.password.length > 72) errors.password = 'passwordLength'

  return errors
}
