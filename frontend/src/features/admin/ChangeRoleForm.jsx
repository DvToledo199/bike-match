import { useId, useState } from 'react'
import { useTranslation } from 'react-i18next'
import styles from './Admin.module.css'

export default function ChangeRoleForm({ user, busy, disabled, onConfirm, onCancel }) {
  const { t } = useTranslation()
  const id = useId()
  const [role, setRole] = useState(user.role)

  function submit(event) {
    event.preventDefault()
    if (disabled || role === user.role || !['USER', 'MODERATOR'].includes(role)) return
    onConfirm(role)
  }

  return (
    <form className={styles.form} aria-labelledby={`${id}-title`} onSubmit={submit}>
      <h2 id={`${id}-title`}>{t('admin.changeRoleFor', { username: user.username })}</h2>
      <label htmlFor={id}>{t('admin.newRole')}</label>
      <select id={id} value={role} disabled={disabled} autoFocus
        onChange={(event) => setRole(event.target.value)} aria-describedby={`${id}-confirmation ${id}-sessions`}>
        <option value="USER">{t('admin.roles.USER')}</option>
        <option value="MODERATOR">{t('admin.roles.MODERATOR')}</option>
      </select>
      <p id={`${id}-confirmation`}>{role === user.role ? t('admin.chooseDifferentRole') : t('admin.confirmation', {
        username: user.username, previousRole: t(`admin.roles.${user.role}`), nextRole: t(`admin.roles.${role}`),
      })}</p>
      <p id={`${id}-sessions`} className={styles.muted}>{t('admin.existingSessions')}</p>
      <div className={styles.actions}>
        <button type="submit" className={styles.primary} disabled={disabled || role === user.role}>
          {t(busy ? 'admin.saving' : 'admin.confirm')}
        </button>
        <button type="button" onClick={onCancel} disabled={busy}>{t('admin.cancel')}</button>
      </div>
    </form>
  )
}
