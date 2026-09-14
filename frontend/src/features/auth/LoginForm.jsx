import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { loginUser } from '../../services/authLogin.js'
import { saveSession } from '../../services/session.js'
import { validateLoginForm } from './loginValidation.js'
import styles from './LoginForm.module.css'

function LoginForm({ onLoggedIn, onContinueAsGuest }) {
  const { t } = useTranslation()
  const [values, setValues] = useState({ email: '', password: '' })
  const [errors, setErrors] = useState({})
  const [status, setStatus] = useState('idle')

  function updateField(event) {
    const { name, value } = event.target
    setValues((current) => ({ ...current, [name]: value }))
    setErrors((current) => ({ ...current, [name]: undefined }))
    setStatus('idle')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const validationErrors = validateLoginForm(values)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setStatus('loading')
    try {
      const loginResponse = await loginUser({
        email: values.email.trim(),
        password: values.password,
      })
      const session = saveSession(loginResponse)
      onLoggedIn(session)
    } catch (error) {
      setStatus(error.kind === 'invalidCredentials' ? 'invalidCredentials' : 'error')
    }
  }

  return (
    <section className={styles.card} aria-labelledby="login-title">
      <p className={styles.eyebrow}>{t('auth.login.eyebrow')}</p>
      <h1 id="login-title" className={styles.title}>{t('auth.login.title')}</h1>
      <p className={styles.description}>{t('auth.login.description')}</p>

      <form className={styles.form} onSubmit={handleSubmit} noValidate>
        <label className={styles.field}>
          <span>{t('auth.login.emailLabel')}</span>
          <input
            name="email"
            type="email"
            value={values.email}
            onChange={updateField}
            autoComplete="email"
            aria-invalid={Boolean(errors.email)}
            aria-describedby={errors.email ? 'login-email-error' : undefined}
          />
          {errors.email && <small id="login-email-error" className={styles.error}>{t(`auth.login.errors.${errors.email}`, { field: t('auth.login.emailLabel') })}</small>}
        </label>

        <label className={styles.field}>
          <span>{t('auth.login.passwordLabel')}</span>
          <input
            name="password"
            type="password"
            value={values.password}
            onChange={updateField}
            autoComplete="current-password"
            aria-invalid={Boolean(errors.password)}
            aria-describedby={errors.password ? 'login-password-error' : undefined}
          />
          {errors.password && <small id="login-password-error" className={styles.error}>{t(`auth.login.errors.${errors.password}`, { field: t('auth.login.passwordLabel') })}</small>}
        </label>

        {status === 'invalidCredentials' && <p className={`${styles.message} ${styles.errorMessage}`} role="alert">{t('auth.login.invalidCredentials')}</p>}
        {status === 'error' && <p className={`${styles.message} ${styles.errorMessage}`} role="alert">{t('auth.login.unavailable')}</p>}

        <div className={styles.actions}>
          <button type="button" className={styles.secondaryButton} onClick={onContinueAsGuest}>
            {t('auth.continueAsGuest')}
          </button>
          <button type="submit" className={styles.primaryButton} disabled={status === 'loading'}>
            {status === 'loading' ? t('auth.login.submitting') : t('auth.login.submit')}
          </button>
        </div>
      </form>
    </section>
  )
}

export default LoginForm
