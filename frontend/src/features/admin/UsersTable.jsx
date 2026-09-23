import { useTranslation } from 'react-i18next'
import styles from './Admin.module.css'

export default function UsersTable({ users, currentUsername, disabled, onSelectUser }) {
  const { t, i18n } = useTranslation()

  return (
    <div className={styles.tableScroll}>
      <table className={styles.table} role="table">
        <caption>{t('admin.accounts')}</caption>
        <thead role="rowgroup"><tr role="row">
          <th scope="col" role="columnheader">{t('admin.username')}</th>
          <th scope="col" role="columnheader">{t('admin.role')}</th>
          <th scope="col" role="columnheader">{t('admin.createdAt')}</th>
          <th scope="col" role="columnheader">{t('admin.actions')}</th>
        </tr></thead>
        <tbody role="rowgroup">{users.map((user) => (
          <tr key={user.id} role="row">
            <th scope="row" role="rowheader">
              {user.username}{' '}
              <span className={styles.email}>{user.email}</span>
            </th>
            <td role="cell" data-label={t('admin.role')}>{t(`admin.roles.${user.role}`)}</td>
            <td role="cell" data-label={t('admin.createdAt')}><time dateTime={user.createdAt}>{new Date(user.createdAt).toLocaleDateString(i18n.language)}</time></td>
            <td role="cell" className={styles.accountActions}>{user.username === currentUsername ? (
              <span className={styles.muted}>{t('admin.yourAccount')}</span>
            ) : user.role === 'ADMIN' ? (
              <span className={styles.muted}>{t('admin.adminReadOnly')}</span>
            ) : (
              <button type="button" disabled={disabled} aria-label={t('admin.changeRoleFor', { username: user.username })}
                onClick={() => onSelectUser(user)}>{t('admin.changeRole')}</button>
            )}</td>
          </tr>
        ))}</tbody>
      </table>
    </div>
  )
}
