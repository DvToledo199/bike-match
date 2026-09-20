import { useTranslation } from 'react-i18next'
import styles from './Admin.module.css'

export default function UsersTable({ users, currentUsername, disabled, onSelectUser }) {
  const { t, i18n } = useTranslation()

  return (
    <div className={styles.tableScroll}>
      <table className={styles.table}>
        <caption>{t('admin.accounts')}</caption>
        <thead><tr>
          <th scope="col">{t('admin.username')}</th>
          <th scope="col">{t('admin.role')}</th>
          <th scope="col">{t('admin.createdAt')}</th>
          <th scope="col">{t('admin.actions')}</th>
        </tr></thead>
        <tbody>{users.map((user) => (
          <tr key={user.id}>
            <th scope="row">{user.username}</th>
            <td>{t(`admin.roles.${user.role}`)}</td>
            <td><time dateTime={user.createdAt}>{new Date(user.createdAt).toLocaleDateString(i18n.language)}</time></td>
            <td>{user.username === currentUsername ? (
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
