export function validateLoginForm(values) {
  const errors = {}

  if (!values.email.trim()) errors.email = 'required'
  else if (!/^\S+@\S+\.\S+$/.test(values.email.trim())) errors.email = 'email'
  if (!values.password) errors.password = 'required'

  return errors
}
