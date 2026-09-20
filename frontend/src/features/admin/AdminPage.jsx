import { useEffect, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { changeUserRole, listUsers } from '../../services/admin.js'
import UsersTable from './UsersTable.jsx'
import ChangeRoleForm from './ChangeRoleForm.jsx'
import styles from './Admin.module.css'

function errorKey(error) {
  const statuses = { 400: 'invalid', 401: 'unauthenticated', 403: 'forbidden', 404: 'notFound', 409: 'ownRole' }
  if (statuses[error.status]) return `admin.errors.${statuses[error.status]}`
  if (error.kind === 'network' || error.kind === 'timeout') return 'admin.errors.connection'
  return 'admin.errors.unavailable'
}

export default function AdminPage({ session }) {
  const { t } = useTranslation()
  const allowed = session?.role === 'ADMIN'
  const [users, setUsers] = useState(null)
  const [selectedId, setSelectedId] = useState(null)
  const [saving, setSaving] = useState(false)
  const [loadError, setLoadError] = useState(null)
  const [actionError, setActionError] = useState(null)
  const [notice, setNotice] = useState(null)
  const [reloadKey, setReloadKey] = useState(0)
  const selectedUser = users?.find((user) => user.id === selectedId)

  useEffect(() => {
    if (!allowed) return
    let active = true
    setUsers(null)
    setLoadError(null)
    listUsers()
      .then((result) => { if (active) setUsers(result) })
      .catch((error) => { if (active) setLoadError(error) })
    return () => { active = false }
  }, [allowed, session?.accessToken, reloadKey])

  function refresh() {
    setUsers(null)
    setSelectedId(null)
    setActionError(null)
    setReloadKey((key) => key + 1)
  }

  function selectUser(user) {
    if (user.username === session.username || user.role === 'ADMIN') return
    setSelectedId(user.id)
    setActionError(null)
    setNotice(null)
  }

  async function changeRole(role) {
    if (saving || !selectedUser) return
    setSaving(true)
    setActionError(null)
    setNotice(null)
    try {
      await changeUserRole(selectedUser.id, role)
      // A 204 confirms the requested role. Only then update the corresponding row.
      setUsers((current) => current.map((user) => user.id === selectedUser.id ? { ...user, role } : user))
      setNotice({ username: selectedUser.username, role })
      setSelectedId(null)
    } catch (error) {
      setActionError(error)
    } finally {
      setSaving(false)
    }
  }

  const accessError = [loadError, actionError].find((error) => error?.status === 401 || error?.status === 403)
  const loading = users === null && !loadError
  const mustRefresh = Boolean(actionError && actionError.status !== 400)

  // The four states this screen can be in, in the order they are checked.
  function content() {
    if (loadError) {
      return (
        <div role="alert" className={styles.message}>
          <p>{t(errorKey(loadError))}</p>
          <button type="button" onClick={refresh}>{t('admin.retry')}</button>
        </div>
      )
    }

    if (loading) {
      return <p role="status">{t('admin.loading')}</p>
    }

    if (users.length === 0) {
      return <p className={styles.message} role="status">{t('admin.empty')}</p>
    }

    return (
      <>
        {selectedUser && <ChangeRoleForm key={selectedUser.id} user={selectedUser} busy={saving}
          disabled={saving || mustRefresh} onConfirm={changeRole} onCancel={() => setSelectedId(null)} />}
        <UsersTable users={users} currentUsername={session.username} disabled={saving || mustRefresh} onSelectUser={selectUser} />
      </>
    )
  }

  if (!allowed || accessError) {
    const needsLogin = !session || accessError?.status === 401
    return <section className={styles.page}>
      <h1>{t('admin.title')}</h1>
      <p role="alert">{t(needsLogin ? 'admin.errors.unauthenticated' : 'admin.errors.forbidden')}</p>
      {needsLogin ? <a href="#/login">{t('auth.openLogin')}</a> : <a href="#/">{t('admin.home')}</a>}
    </section>
  }

  return (
    <section className={styles.page}>
      <header className={styles.header}>
        <div><h1>{t('admin.title')}</h1><p className={styles.muted}>{t('admin.description')}</p></div>
        <button type="button" disabled={saving || loading} onClick={refresh}>{t('admin.refresh')}</button>
      </header>
      {notice && <p className={styles.success} role="status">{t('admin.success', { username: notice.username, role: t(`admin.roles.${notice.role}`) })}</p>}
      {actionError && <p className={styles.error} role="alert">{t(errorKey(actionError))}</p>}
      {content()}
    </section>
  )
}
