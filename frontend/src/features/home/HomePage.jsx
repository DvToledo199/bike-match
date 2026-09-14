import { useTranslation } from 'react-i18next'
import styles from './HomePage.module.css'

export default function HomePage() {
  const { t } = useTranslation()
  return (
    <div className={styles.page}>
      <section className={styles.hero} aria-labelledby="home-title">
        <div className={styles.intro}>
          <p className={styles.eyebrow}>{t('home.eyebrow')}</p>
          <h1 id="home-title">{t('home.title')}<span>{t('home.titleAccent')}</span></h1>
          <p className={styles.description}>{t('home.description')}</p>
          <div className={styles.actions}>
            <a className={styles.primary} href="#/analyze">{t('app.analyze')} <span aria-hidden="true">↗</span></a>
            <a className={styles.secondary} href="#/catalog">{t('home.explore')} <span aria-hidden="true">→</span></a>
          </div>
          <p className={styles.note}>{t('home.guest')}</p>
        </div>
        <div className={styles.visual}>
          <div className={styles.visualHeader}><span>{t('home.visualTitle')}</span><span className={styles.dot} aria-hidden="true" /></div>
          <svg viewBox="0 0 640 360" aria-hidden="true">
            <g className={styles.grid}><path d="M0 90H640M0 180H640M0 270H640M160 0V360M320 0V360M480 0V360" /></g>
            <g className={styles.wheels}><circle cx="148" cy="233" r="94" /><circle cx="498" cy="233" r="94" /><circle cx="148" cy="233" r="82" /><circle cx="498" cy="233" r="82" /></g>
            <g className={styles.frame}><path d="M148 233L294 225L243 126L148 233M294 225L411 97L253 145M411 97L498 233M410 97L401 75L425 70M243 126L237 99M218 99H252" /><path className={styles.shock} d="M268 176L328 166" /></g>
            <g className={styles.marks}>{[[268,176],[294,225],[328,166],[148,233],[498,233]].map(([x,y]) => <g key={x}><circle cx={x} cy={y} r="8" /><path d={`M${x-12} ${y}h24M${x} ${y-12}v24`} /></g>)}</g>
          </svg>
          <div className={styles.visualFooter}><span>{t('home.visualCaption')}</span><span aria-hidden="true">01 → 02 → 03</span></div>
        </div>
      </section>
      <section className={styles.how} aria-labelledby="how-title">
        <div className={styles.sectionHeading}><p className={styles.eyebrow}>{t('home.howEyebrow')}</p><h2 id="how-title">{t('home.howTitle')}</h2></div>
        <ol className={styles.steps}>
          {['photo', 'mark', 'understand'].map((step, index) => <li key={step}><span className={styles.number}>0{index + 1}</span><h3>{t(`home.steps.${step}.title`)}</h3><p>{t(`home.steps.${step}.description`)}</p></li>)}
        </ol>
      </section>
      <section className={styles.community}><div><p className={styles.eyebrow}>{t('home.communityEyebrow')}</p><h2>{t('home.communityTitle')}</h2><p>{t('home.communityDescription')}</p></div><a className={styles.secondary} href="#/catalog">{t('home.explore')} <span aria-hidden="true">→</span></a></section>
    </div>
  )
}
