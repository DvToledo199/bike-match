import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { deleteBike, dismissNotice, listMyBikes, listMyNotices, publishBike } from '../../services/myBikes.js'
import styles from './MyBikesPage.module.css'

function MyBikesPage({ onOpenBikeDetail }) {
  const { t, i18n } = useTranslation()
  const [bikes, setBikes] = useState(null)
  const [error, setError] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const [confirmingBikeId, setConfirmingBikeId] = useState(null)
  const [publishingBikeId, setPublishingBikeId] = useState(null)
  const [publishError, setPublishError] = useState(null)
  const [confirmingDeleteBikeId, setConfirmingDeleteBikeId] = useState(null)
  const [deletingBikeId, setDeletingBikeId] = useState(null)
  const [deleteError, setDeleteError] = useState(null)
  const [notices, setNotices] = useState([])
  const [noticesError, setNoticesError] = useState(null)
  const [noticesReloadKey, setNoticesReloadKey] = useState(0)
  const [dismissingNoticeId, setDismissingNoticeId] = useState(null)
  const [dismissError, setDismissError] = useState(null)

  useEffect(() => {
    let active = true
    setBikes(null)
    setError(null)

    listMyBikes()
      .then((result) => {
        if (active) setBikes(result)
      })
      .catch((requestError) => {
        if (active) setError(requestError)
      })

    return () => { active = false }
  }, [reloadKey])

  // Notices are secondary: if they cannot be loaded, the bikes still show and the page says so.
  useEffect(() => {
    let active = true

    listMyNotices()
      .then((result) => {
        if (active) {
          setNotices(result)
          setNoticesError(null)
        }
      })
      .catch((requestError) => {
        if (active) {
          setNotices([])
          setNoticesError(requestError)
        }
      })

    return () => { active = false }
  }, [reloadKey, noticesReloadKey])

  async function handlePublish(bikeId) {
    setPublishingBikeId(bikeId)
    setPublishError(null)
    try {
      const result = await publishBike(bikeId)
      setBikes((current) => current.map((bike) => (
        bike.id === bikeId ? { ...bike, status: result.status } : bike
      )))
      setConfirmingBikeId(null)
    } catch (requestError) {
      setPublishError({ bikeId, requestError })
    } finally {
      setPublishingBikeId(null)
    }
  }

  async function handleDelete(bikeId) {
    setDeletingBikeId(bikeId)
    setDeleteError(null)
    try {
      await deleteBike(bikeId)
      removeBike(bikeId)
    } catch (requestError) {
      // A bike that no longer exists was already deleted, for example from another tab.
      if (requestError.status === 404) removeBike(bikeId)
      else setDeleteError({ bikeId, requestError })
    } finally {
      setDeletingBikeId(null)
    }
  }

  function removeBike(bikeId) {
    setBikes((current) => current.filter((bike) => bike.id !== bikeId))
    setConfirmingDeleteBikeId(null)
  }

  async function handleDismissNotice(noticeId) {
    setDismissingNoticeId(noticeId)
    setDismissError(null)
    try {
      await dismissNotice(noticeId)
      removeNotice(noticeId)
    } catch (requestError) {
      // A notice that no longer exists was already dismissed, for example from another tab.
      if (requestError.status === 404) removeNotice(noticeId)
      else setDismissError({ noticeId, requestError })
    } finally {
      setDismissingNoticeId(null)
    }
  }

  function removeNotice(noticeId) {
    setNotices((current) => current.filter((notice) => notice.id !== noticeId))
  }

  if (error) {
    const message = error.status === 401
      ? t('myBikes.errors.sessionExpired')
      : t('myBikes.errors.unavailable')

    return (
      <section className={styles.page} aria-labelledby="my-bikes-title">
        <PageHeading t={t} />
        <div className={`${styles.message} ${styles.error}`} role="alert">
          <p>{message}</p>
          <button type="button" className={styles.secondaryButton} onClick={() => setReloadKey((key) => key + 1)}>
            {t('myBikes.retry')}
          </button>
        </div>
      </section>
    )
  }

  if (!bikes) {
    return (
      <section className={styles.page} aria-labelledby="my-bikes-title" aria-busy="true">
        <PageHeading t={t} />
        <p className={styles.muted}>{t('myBikes.loading')}</p>
      </section>
    )
  }

  return (
    <section className={styles.page} aria-labelledby="my-bikes-title">
      <PageHeading t={t} />
      {noticesError && (
        <p className={styles.noticesError} role="status">
          <span>{t('myBikes.notices.errors.load')}</span>
          <button type="button" className={styles.linkButton} onClick={() => setNoticesReloadKey((key) => key + 1)}>
            {t('myBikes.retry')}
          </button>
        </p>
      )}
      {notices.length > 0 && (
        <RemovalNotices
          notices={notices}
          t={t}
          language={i18n.language}
          dismissingNoticeId={dismissingNoticeId}
          dismissError={dismissError}
          onDismiss={handleDismissNotice}
        />
      )}
      {bikes.length === 0 ? (
        <div className={styles.message}>
          <p>{t('myBikes.empty')}</p>
        </div>
      ) : (
        <div className={styles.grid}>
          {bikes.map((bike) => (
            <BikeCard
              key={bike.id}
              bike={bike}
              t={t}
              confirming={confirmingBikeId === bike.id}
              publishing={publishingBikeId === bike.id}
              publishError={publishError?.bikeId === bike.id ? publishError.requestError : null}
              confirmingDelete={confirmingDeleteBikeId === bike.id}
              deleting={deletingBikeId === bike.id}
              deleteError={deleteError?.bikeId === bike.id ? deleteError.requestError : null}
              onOpenDetails={() => onOpenBikeDetail(bike.id)}
              onRequestPublish={() => { setPublishError(null); setConfirmingDeleteBikeId(null); setConfirmingBikeId(bike.id) }}
              onCancelPublish={() => setConfirmingBikeId(null)}
              onConfirmPublish={() => handlePublish(bike.id)}
              onRequestDelete={() => { setDeleteError(null); setConfirmingBikeId(null); setConfirmingDeleteBikeId(bike.id) }}
              onCancelDelete={() => setConfirmingDeleteBikeId(null)}
              onConfirmDelete={() => handleDelete(bike.id)}
            />
          ))}
        </div>
      )}
    </section>
  )
}

function PageHeading({ t }) {
  return (
    <header className={styles.heading}>
      <p className={styles.eyebrow}>{t('myBikes.eyebrow')}</p>
      <h1 id="my-bikes-title">{t('myBikes.title')}</h1>
      <p className={styles.description}>{t('myBikes.description')}</p>
    </header>
  )
}

function RemovalNotices({ notices, t, language, dismissingNoticeId, dismissError, onDismiss }) {
  const dateFormat = new Intl.DateTimeFormat(language, { dateStyle: 'medium' })

  return (
    <section className={styles.notices} aria-labelledby="removal-notices-title">
      <h2 id="removal-notices-title">{t('myBikes.notices.title')}</h2>
      <p className={styles.muted}>{t('myBikes.notices.description')}</p>
      <ul className={styles.noticeList}>
        {notices.map((notice) => {
          const name = [notice.brand, notice.model].filter(Boolean).join(' ')
          const dismissing = dismissingNoticeId === notice.id
          const noticeError = dismissError?.noticeId === notice.id ? dismissError.requestError : null

          return (
            <li key={notice.id} className={styles.notice}>
              <div className={styles.noticeText}>
                <h3>{t('myBikes.notices.bikeRemoved', { name })}</h3>
                <p>{t('myBikes.notices.reason', { reason: notice.reason })}</p>
                <p className={styles.muted}>
                  {t('myBikes.notices.removedAt', { date: dateFormat.format(new Date(notice.removedAt)) })}
                </p>
              </div>
              <button type="button" className={styles.secondaryButton} onClick={() => onDismiss(notice.id)} disabled={dismissing}>
                {dismissing ? t('myBikes.notices.dismissing') : t('myBikes.notices.dismiss')}
              </button>
              {noticeError && (
                <p className={styles.actionError} role="alert">
                  {noticeError.status === 401
                    ? t('myBikes.notices.errors.sessionExpired')
                    : t('myBikes.notices.errors.unavailable')}
                </p>
              )}
            </li>
          )
        })}
      </ul>
    </section>
  )
}

function BikeCard({
  bike, t,
  confirming, publishing, publishError, onRequestPublish, onCancelPublish, onConfirmPublish,
  confirmingDelete, deleting, deleteError, onRequestDelete, onCancelDelete, onConfirmDelete,
  onOpenDetails,
}) {
  const statusKey = bike.status?.toLowerCase() ?? 'unknown'
  const title = [bike.brand, bike.model].filter(Boolean).join(' ')

  return (
    <article className={styles.card}>
      {bike.photoUrl ? (
        <img className={styles.photo} src={bike.photoUrl} alt={t('myBikes.photoAlt', { name: title })} />
      ) : (
        <div className={styles.photoPlaceholder} aria-hidden="true">{t('myBikes.noPhoto')}</div>
      )}
      <div className={styles.cardBody}>
        <div className={styles.cardHeading}>
          <span className={`${styles.status} ${styles[`status${statusKey}`]}`}>
            {t(`myBikes.status.${statusKey}`)}
          </span>
          <h2>{title || t('myBikes.unnamed')}</h2>
        </div>
        <dl className={styles.details}>
          <div><dt>{t('myBikes.fields.year')}</dt><dd>{bike.modelYear ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.category')}</dt><dd>{bike.category ?? t('myBikes.notAvailable')}</dd></div>
          <div><dt>{t('myBikes.fields.analysis')}</dt><dd>{bike.analyzed ? t('myBikes.analysisReady') : t('myBikes.analysisPending')}</dd></div>
        </dl>
        <button type="button" className={styles.detailButton} onClick={onOpenDetails}>
          {t('myBikes.openDetail')}
        </button>
        {bike.status === 'PRIVATE' && bike.analyzed && !confirming && (
          <button type="button" className={styles.primaryButton} onClick={onRequestPublish}>
            {t('myBikes.publish.request')}
          </button>
        )}
        {confirming && (
          <div className={styles.confirmation} role="group" aria-label={t('myBikes.publish.confirmation')}>
            <p>{t('myBikes.publish.confirmation')}</p>
            <p>{t('myBikes.publish.photoRights')}</p>
            <div className={styles.confirmationActions}>
              <button type="button" className={styles.secondaryButton} onClick={onCancelPublish} disabled={publishing}>
                {t('myBikes.publish.cancel')}
              </button>
              <button type="button" className={styles.primaryButton} onClick={onConfirmPublish} disabled={publishing}>
                {publishing ? t('myBikes.publish.submitting') : t('myBikes.publish.confirm')}
              </button>
            </div>
          </div>
        )}
        {publishError && (
          <p className={styles.actionError} role="alert">
            {publishError.status === 401
              ? t('myBikes.publish.errors.sessionExpired')
              : publishError.status === 403
                ? t('myBikes.publish.errors.forbidden')
                : t('myBikes.publish.errors.unavailable')}
          </p>
        )}
        {!confirmingDelete && (
          <button type="button" className={styles.deleteButton} onClick={onRequestDelete}>
            {t('myBikes.delete.request')}
          </button>
        )}
        {confirmingDelete && (
          <div className={styles.confirmation} role="group" aria-label={t('myBikes.delete.confirmation')}>
            <p>{t('myBikes.delete.confirmation')}</p>
            <div className={styles.confirmationActions}>
              <button type="button" className={styles.secondaryButton} onClick={onCancelDelete} disabled={deleting}>
                {t('myBikes.delete.cancel')}
              </button>
              <button type="button" className={styles.dangerButton} onClick={onConfirmDelete} disabled={deleting}>
                {deleting ? t('myBikes.delete.submitting') : t('myBikes.delete.confirm')}
              </button>
            </div>
          </div>
        )}
        {deleteError && (
          <p className={styles.actionError} role="alert">
            {deleteError.status === 401
              ? t('myBikes.delete.errors.sessionExpired')
              : t('myBikes.delete.errors.unavailable')}
          </p>
        )}
      </div>
    </article>
  )
}

export default MyBikesPage
