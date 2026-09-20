import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { approveBike, listPendingBikes, rejectBike, removeBike } from '../../services/moderation.js'
import { listPublicBikes } from '../../services/myBikes.js'
import ModerationBikeCard from './ModerationBikeCard.jsx'
import styles from './Moderation.module.css'

function errorKey(error) {
  const statuses = { 400: 'invalid', 401: 'unauthenticated', 403: 'forbidden', 404: 'notFound', 409: 'conflict' }
  if (statuses[error.status]) return `moderation.errors.${statuses[error.status]}`
  if (error.kind === 'network' || error.kind === 'timeout') return 'moderation.errors.connection'
  return 'moderation.errors.unavailable'
}

export default function ModerationPage({ session }) {
  const { t } = useTranslation()
  const allowed = session?.role === 'MODERATOR' || session?.role === 'ADMIN'
  const [view, setView] = useState('pending')
  const [page, setPage] = useState(0)
  const [bikes, setBikes] = useState(null)
  const [pagination, setPagination] = useState(null)
  const [loadError, setLoadError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [action, setAction] = useState(null)
  const [removingId, setRemovingId] = useState(null)
  const [notice, setNotice] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    if (!allowed) return
    let active = true
    setBikes(null)
    setPagination(null)
    setLoadError(null)
    const request = view === 'pending' ? listPendingBikes() : listPublicBikes({ page })
    request.then((result) => {
      if (!active) return
      if (view === 'published') {
        // Deletion by another moderator can make the current catalog page disappear.
        if (page > 0 && page >= result.totalPages) {
          setPage(Math.max(0, result.totalPages - 1))
          return
        }
        setPagination(result)
        setBikes(result.items)
      } else {
        setBikes(result)
      }
    }).catch((error) => { if (active) setLoadError(error) })
    return () => { active = false }
  }, [allowed, session?.accessToken, view, page, reloadKey])

  function refresh() {
    setBikes(null)
    setActionError(null)
    setRemovingId(null)
    setReloadKey((key) => key + 1)
  }

  function changeView(nextView) {
    setView(nextView)
    setPage(0)
    setNotice(null)
    refresh()
  }

  async function decide(bike, type, reason) {
    if (action) return
    setAction({ bikeId: bike.id, type })
    setActionError(null)
    setNotice(null)
    try {
      if (type === 'approve') await approveBike(bike.id)
      else if (type === 'reject') await rejectBike(bike.id)
      else await removeBike(bike.id, reason)
      setNotice({ type, name: [bike.brand, bike.model].filter(Boolean).join(' ') || t('catalog.unnamed') })
      refresh()
    } catch (error) {
      setActionError(error)
    } finally {
      setAction(null)
    }
  }

  const accessError = [loadError, actionError].find((error) => error?.status === 401 || error?.status === 403)
  const loading = bikes === null && !loadError
  const busy = Boolean(action)
  // After a conflict or an uncertain response, refresh before attempting another decision.
  const mustRefresh = Boolean(actionError && actionError.status !== 400)

  if (!allowed || accessError) {
    const needsLogin = !session || accessError?.status === 401
    return <section className={styles.page}>
      <h1>{t('moderation.title')}</h1>
      <p role="alert">{t(needsLogin ? 'moderation.errors.unauthenticated' : 'moderation.errors.forbidden')}</p>
      {needsLogin ? <a href="#/login">{t('auth.openLogin')}</a> : <a href="#/">{t('moderation.home')}</a>}
    </section>
  }

  return (
    <section className={styles.page}>
      <header><h1>{t('moderation.title')}</h1><p className={styles.muted}>{t('moderation.description')}</p></header>
      <div className={styles.toolbar}>
        <div className={styles.actions} role="group" aria-label={t('moderation.views')}>
          <button type="button" aria-pressed={view === 'pending'} disabled={busy} onClick={() => changeView('pending')}>{t('moderation.pending')}</button>
          <button type="button" aria-pressed={view === 'published'} disabled={busy} onClick={() => changeView('published')}>{t('moderation.published')}</button>
        </div>
        <button type="button" disabled={busy || loading} onClick={refresh}>{t('moderation.refresh')}</button>
      </div>
      <p className={styles.muted}>{t(view === 'pending' ? 'moderation.pendingHelp' : 'moderation.publishedHelp')}</p>
      {notice && <p className={styles.success} role="status">{t(`moderation.success.${notice.type}`, { name: notice.name })}</p>}
      {actionError && <p className={styles.error} role="alert">{t(errorKey(actionError))}</p>}
      {loadError ? <div className={styles.message} role="alert">
        <p>{t(errorKey(loadError))}</p>
        <button type="button" onClick={refresh}>{t('moderation.retry')}</button>
      </div> : loading ? <p role="status">{t('moderation.loading')}</p> : <>
        {bikes.length === 0 ? <p className={styles.message} role="status">{t(view === 'pending' ? 'moderation.emptyPending' : 'moderation.emptyPublished')}</p> : (
          <div className={styles.grid}>
            {bikes.map((bike) => <ModerationBikeCard key={bike.id} bike={bike} pending={view === 'pending'}
              disabled={busy || mustRefresh} action={action?.bikeId === bike.id ? action.type : null}
              removing={removingId === bike.id}
              onApprove={() => decide(bike, 'approve')} onReject={() => decide(bike, 'reject')}
              onRemove={() => { setRemovingId(bike.id); setActionError(null) }}
              onConfirmRemoval={(reason) => decide(bike, 'remove', reason)} onCancelRemoval={() => setRemovingId(null)} />)}
          </div>
        )}
        {view === 'published' && pagination?.totalPages > 1 && (
          <nav className={styles.pagination} aria-label={t('catalog.pagination.label')}>
            <button type="button" disabled={busy || page === 0} onClick={() => { setPage(page - 1); refresh() }}>{t('catalog.pagination.previous')}</button>
            <span>{t('catalog.pagination.page', { page: pagination.page + 1, total: pagination.totalPages })}</span>
            <button type="button" disabled={busy || !pagination.hasNext} onClick={() => { setPage(page + 1); refresh() }}>{t('catalog.pagination.next')}</button>
          </nav>
        )}
      </>}
    </section>
  )
}
