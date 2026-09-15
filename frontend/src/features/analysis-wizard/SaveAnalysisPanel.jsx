import { useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { createAnalysisSaver } from '../../services/saveAnalysis.js'
import styles from './SaveAnalysisPanel.module.css'

export default function SaveAnalysisPanel({ session, wizardData, onLockedChange, onSaved, onStartNew }) {
  const { t } = useTranslation()
  const saver = useRef(null)
  if (!saver.current) saver.current = createAnalysisSaver()
  const [metadata, setMetadata] = useState({ brand: '', model: '', modelYear: '', category: '', cassetteType: '' })
  const [stage, setStage] = useState(null)
  const [error, setError] = useState(null)
  const [saved, setSaved] = useState(null)
  const busy = stage !== null
  const checkpoint = saver.current.checkpoint

  async function handleSave(event) {
    event.preventDefault()
    if (busy || !metadata.brand.trim() || !metadata.model.trim()) return
    setError(null)
    onLockedChange(true)
    try {
      const result = await saver.current.save({ username: session.username, metadata, wizardData }, setStage)
      setSaved(result)
      onSaved(result.bikeId)
    } catch (requestError) {
      const key = checkpoint.uncertainCreate ? 'uncertainCreate'
        : requestError.status === 401 ? 'sessionExpired'
          : requestError.kind === 'differentOwner' ? 'differentOwner'
            : requestError.status === 400 ? 'invalid'
              : checkpoint.bikeId ? 'resume' : 'unavailable'
      setError(key)
      onLockedChange(Boolean(checkpoint.bikeId || checkpoint.uncertainCreate))
    } finally {
      setStage(null)
    }
  }

  function update(event) {
    const { name, value } = event.target
    setMetadata((current) => ({ ...current, [name]: value }))
  }

  if (saved) return (
    <section className={styles.panel}>
      <h2>{t('saveAnalysis.saved')}</h2>
      <p>{t(saved.explanationReady ? 'saveAnalysis.ready' : 'saveAnalysis.explanationPending')}</p>
      <div className={styles.actions}>
        <a className={styles.primary} href={`#/bikes/${saved.bikeId}`}>{t('saveAnalysis.open')}</a>
        <button type="button" className={styles.secondary} onClick={onStartNew}>{t('saveAnalysis.startNew')}</button>
      </div>
    </section>
  )

  return (
    <section className={styles.panel} aria-labelledby="save-analysis-title" aria-busy={busy}>
      <p className={styles.eyebrow}>{t('saveAnalysis.eyebrow')}</p>
      <h2 id="save-analysis-title" tabIndex={-1}>{t('saveAnalysis.title')}</h2>
      <p>{t(session ? 'saveAnalysis.description' : 'saveAnalysis.guest')}</p>
      {!session ? (
        <div className={styles.actions}>
          <a className={styles.primary} href="#/register">{t('saveAnalysis.registerToSave')}</a>
          <a className={styles.secondary} href="#/login">{t('saveAnalysis.loginToSave')}</a>
        </div>
      ) : (
        <form onSubmit={handleSave}>
          <fieldset className={styles.fields} disabled={busy || Boolean(checkpoint.bikeId) || checkpoint.uncertainCreate}>
            <legend>{t('saveAnalysis.details')}</legend>
            {['brand', 'model', 'modelYear'].map((name) => <label key={name}>
              <span>{t(`saveAnalysis.fields.${name}`)}</span>
              <input name={name} value={metadata[name]} onChange={update} required={name !== 'modelYear'}
                type={name === 'modelYear' ? 'number' : 'text'}
                maxLength={name === 'brand' ? 80 : name === 'model' ? 100 : undefined}
                min={name === 'modelYear' ? 1900 : undefined} max={name === 'modelYear' ? 2100 : undefined}
                step={name === 'modelYear' ? 1 : undefined} />
            </label>)}
            <label><span>{t('saveAnalysis.fields.category')}</span>
              <select name="category" value={metadata.category} onChange={update} required>
                <option value="" disabled>{t('saveAnalysis.choose')}</option>
                {['ENDURO', 'E_ENDURO', 'DOWNHILL'].map((value) => <option key={value} value={value}>{t(`saveAnalysis.categories.${value}`)}</option>)}
              </select>
            </label>
            <label><span>{t('saveAnalysis.fields.cassetteType')}</span>
              <select name="cassetteType" value={metadata.cassetteType} onChange={update} required>
                <option value="" disabled>{t('saveAnalysis.choose')}</option>
                <option value="TWELVE_SPEED">{t('saveAnalysis.cassettes.twelve')}</option>
                <option value="DH_7_8">{t('saveAnalysis.cassettes.dh')}</option>
              </select>
            </label>
          </fieldset>
          <p className={styles.note}>{t('saveAnalysis.photoNotice')}</p>
          {error && <p className={styles.error} role="alert">{t(`saveAnalysis.errors.${error}`)}</p>}
          {busy && <p role="status">{t(`saveAnalysis.stages.${stage}`)}</p>}
          <div className={styles.actions}>
            <button className={styles.primary} type="submit" disabled={busy || checkpoint.uncertainCreate}>
              {t(busy ? 'saveAnalysis.working' : checkpoint.bikeId ? 'saveAnalysis.retry' : 'saveAnalysis.save')}
            </button>
            {(error === 'sessionExpired' || error === 'differentOwner') && <a className={styles.secondary} href="#/login">{t('auth.openLogin')}</a>}
            {checkpoint.uncertainCreate && <a className={styles.secondary} href="#/my-bikes">{t('myBikes.navLabel')}</a>}
            {error && !busy && (checkpoint.bikeId || checkpoint.uncertainCreate) && <button type="button" className={styles.secondary} onClick={onStartNew}>{t('saveAnalysis.startNew')}</button>}
          </div>
        </form>
      )}
    </section>
  )
}
