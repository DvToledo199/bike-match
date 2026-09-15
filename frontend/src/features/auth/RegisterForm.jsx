import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import { registerUser } from '../../services/auth.js'
import { validateRegisterForm } from './registerValidation.js'
import styles from './RegisterForm.module.css'

const emptyValues = { email: '', username: '', password: '' }

function RegisterForm({ onContinueAsGuest }) {
  const { t } = useTranslation()
  const [values, setValues] = useState(emptyValues)
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
    const validationErrors = validateRegisterForm(values)
    setErrors(validationErrors)
    if (Object.keys(validationErrors).length > 0) return

    setStatus('loading')
    try {
      await registerUser({
        email: values.email.trim(),
        username: values.username.trim(),
        password: values.password,
      })
      setStatus('success')
    } catch (error) {
      setStatus(error.kind === 'duplicate' ? 'duplicate' : 'error')
    }
  }

  return (
    <section className={styles.card} aria-labelledby="register-title">
      <p className={styles.eyebrow}>{t('auth.register.eyebrow')}</p>
      <h1 id="register-title" className={styles.title}>{t('auth.register.title')}</h1>
      <p className={styles.description}>{t('auth.register.description')}</p>

      {status === 'success' ? (
        <div className={`${styles.message} ${styles.success}`} role="status">
          <p>{t('auth.register.success')}</p>
          <a className={styles.primaryButton} href="#/login">{t('auth.openLogin')}</a>
          <button type="button" className={styles.primaryButton} onClick={onContinueAsGuest}>
            {t('auth.continueAsGuest')}
          </button>
        </div>
      ) : (
        <form className={styles.form} onSubmit={handleSubmit} noValidate>
          <label className={styles.field}>
            <span>{t('auth.register.emailLabel')}</span>
            <input
              name="email"
              type="email"
              value={values.email}
              onChange={updateField}
              autoComplete="email"
              aria-invalid={Boolean(errors.email)}
              aria-describedby={errors.email ? 'register-email-error' : undefined}
            />
            {errors.email && <small id="register-email-error" className={styles.error}>{t(`auth.register.errors.${errors.email}`, { field: t('auth.register.emailLabel') })}</small>}
          </label>

          <label className={styles.field}>
            <span>{t('auth.register.usernameLabel')}</span>
            <input
              name="username"
              type="text"
              value={values.username}
              onChange={updateField}
              autoComplete="username"
              aria-invalid={Boolean(errors.username)}
              aria-describedby={errors.username ? 'register-username-error' : undefined}
            />
            {errors.username && <small id="register-username-error" className={styles.error}>{t(`auth.register.errors.${errors.username}`, { field: t('auth.register.usernameLabel') })}</small>}
          </label>

          <label className={styles.field}>
            <span>{t('auth.register.passwordLabel')}</span>
            <input
              name="password"
              type="password"
              value={values.password}
              onChange={updateField}
              autoComplete="new-password"
              aria-invalid={Boolean(errors.password)}
              aria-describedby={errors.password ? 'register-password-error' : undefined}
            />
            {errors.password && <small id="register-password-error" className={styles.error}>{t(`auth.register.errors.${errors.password}`, { field: t('auth.register.passwordLabel') })}</small>}
          </label>

          {status === 'duplicate' && <p className={`${styles.message} ${styles.errorMessage}`} role="alert">{t('auth.register.duplicate')}</p>}
          {status === 'error' && <p className={`${styles.message} ${styles.errorMessage}`} role="alert">{t('auth.register.unavailable')}</p>}

          <div className={styles.actions}>
            <button type="button" className={styles.secondaryButton} onClick={onContinueAsGuest}>
              {t('auth.continueAsGuest')}
            </button>
            <button type="submit" className={styles.primaryButton} disabled={status === 'loading'}>
              {status === 'loading' ? t('auth.register.submitting') : t('auth.register.submit')}
            </button>
          </div>
        </form>
      )}
    </section>
  )
}

export default RegisterForm
