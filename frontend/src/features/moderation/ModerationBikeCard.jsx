import { useState } from 'react'
import { useTranslation } from 'react-i18next'
import RemoveBikeForm from './RemoveBikeForm.jsx'
import styles from './Moderation.module.css'

export default function ModerationBikeCard({ bike, pending, disabled, action, removing, onApprove, onReject, onRemove, onConfirmRemoval, onCancelRemoval }) {
  const { t, i18n } = useTranslation()
  const [failedPhoto, setFailedPhoto] = useState(null)
  const name = [bike.brand, bike.model].filter(Boolean).join(' ') || t('catalog.unnamed')
  const categories = { ENDURO: 'enduro', E_ENDURO: 'eEnduro', DOWNHILL: 'downhill' }
  const date = pending ? bike.requestedAt : bike.createdAt

  return (
    <article className={styles.card} aria-labelledby={`moderation-bike-${bike.id}`} aria-busy={Boolean(action)}>
      <div className={styles.photo}>
        {bike.photoUrl && failedPhoto !== bike.photoUrl ? (
          <img src={bike.photoUrl} alt={t('catalog.photoAlt', { name })} loading="lazy"
            onError={() => setFailedPhoto(bike.photoUrl)} />
        ) : <span className={styles.muted}>{t('catalog.noPhoto')}</span>}
      </div>
      <div className={styles.cardBody}>
        <h2 id={`moderation-bike-${bike.id}`}>{name}</h2>
        <p className={styles.muted}>
          {bike.modelYear && <span>{bike.modelYear} · </span>}
          {categories[bike.category] ? t(`catalog.filters.${categories[bike.category]}`) : t('myBikes.notAvailable')}
        </p>
        {pending && <p>{t('moderation.owner', { username: bike.ownerUsername })}</p>}
        {date && <p className={styles.muted}>
          {t(pending ? 'moderation.requestedAt' : 'moderation.createdAt')}{' '}
          <time dateTime={date}>{new Date(date).toLocaleDateString(i18n.language)}</time>
        </p>}
        {!removing ? <div className={styles.actions}>
          {pending && <>
            <button type="button" className={styles.primary} disabled={disabled} onClick={onApprove}>
              {t(action === 'approve' ? 'moderation.approving' : 'moderation.approve')}
            </button>
            <button type="button" disabled={disabled} onClick={onReject}>
              {t(action === 'reject' ? 'moderation.rejecting' : 'moderation.reject')}
            </button>
          </>}
          <button type="button" className={styles.danger} disabled={disabled} onClick={onRemove}>{t('moderation.remove.open')}</button>
        </div> : <RemoveBikeForm bikeName={name} busy={Boolean(action)} disabled={disabled}
          onConfirm={onConfirmRemoval} onCancel={onCancelRemoval} />}
      </div>
    </article>
  )
}
