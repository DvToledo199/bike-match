import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Moderation.module.css'

export default function RemoveBikeForm({ bikeName, busy, disabled, onConfirm, onCancel }) {
  const { t } = useTranslation()
  const id = useId()
  const [reason, setReason] = useState('')
  const [invalid, setInvalid] = useState(false)

  function submit(event) {
    event.preventDefault()
    if (disabled) return
    if (!reason.trim() || reason.length > 500) {
      setInvalid(true)
      return
    }
    setInvalid(false)
    onConfirm(reason.trim())
  }

  return (
    <form className={styles.removeForm} onSubmit={submit} aria-label={t('moderation.remove.title', { name: bikeName })}>
      <p id={`${id}-warning`}>{t('moderation.remove.warning')}</p>
      <label htmlFor={id}>{t('moderation.remove.reason')}</label>
      <textarea id={id} value={reason} rows={3} maxLength={500} disabled={disabled} autoFocus
        aria-invalid={invalid} aria-describedby={`${id}-warning ${id}-help${invalid ? ` ${id}-error` : ''}`}
        onChange={(event) => { setReason(event.target.value); setInvalid(false) }} />
      <p id={`${id}-help`} className={styles.muted}>{t('moderation.remove.limit', { count: reason.length })}</p>
      {invalid && <p id={`${id}-error`} className={styles.error} role="alert">{t('moderation.remove.invalidReason')}</p>}
      <div className={styles.actions}>
        <button type="submit" className={styles.danger} disabled={disabled}>
          {t(busy ? 'moderation.removing' : 'moderation.remove.confirm')}
        </button>
        <button type="button" onClick={onCancel} disabled={busy}>{t('moderation.remove.cancel')}</button>
      </div>
    </form>
  )
}
