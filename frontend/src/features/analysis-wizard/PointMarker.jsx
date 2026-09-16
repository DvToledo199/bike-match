import { useEffect, useRef, useState } from 'react'
import { useTranslation } from 'react-i18next'
import { getPointDefinitions, hasAllPoints } from './pointDefinitions.js'
import styles from './PointMarker.module.css'

import { clamp, constrainView, getViewBox, imagePoint, maximumZoom, minimumZoom, zoomAt } from './markerViewport.js'
const regularNudgePixels = 0.5
const fastNudgePixels = 2

function PointMarker({ photo, points, suspensionLayout, updateWizardData }) {
  const { t } = useTranslation()
  const pointDefinitions = getPointDefinitions(suspensionLayout)
  const [selectedPointType, setSelectedPointType] = useState(pointDefinitions[0].type)
  const [view, setView] = useState(() => ({ zoom: 1, center: { x: photo.width / 2, y: photo.height / 2 } }))
  const [aspect, setAspect] = useState(photo.width / photo.height)
  const markerRef = useRef(null)
  const dragRef = useRef(null)
  const [dragging, setDragging] = useState(false)
  const { zoom, center: viewCenter } = view
  const [keyboardCursor, setKeyboardCursor] = useState(null)
  const selectedPoint = pointDefinitions.find((point) => point.type === selectedPointType)
  const viewBox = getViewBox(photo, zoom, viewCenter, aspect)
  const markedPointCount = pointDefinitions.filter((point) => points[point.type]).length
  const allPointsMarked = hasAllPoints(points, suspensionLayout)
  const selectedPointIndex = pointDefinitions.findIndex((point) => point.type === selectedPointType)
  const previousPoint = pointDefinitions[Math.max(0, selectedPointIndex - 1)]
  const pointToUndo = points[selectedPointType] ? selectedPoint : previousPoint
  const canUndoPoint = Boolean(points[pointToUndo.type])
  const cursor = keyboardCursor?.type === selectedPointType
    ? keyboardCursor : points[selectedPointType] ?? viewCenter

  useEffect(() => {
    const marker = markerRef.current
    const updateSize = () => {
      const bounds = marker.getBoundingClientRect()
      if (bounds.width && bounds.height) setAspect(bounds.width / bounds.height)
    }
    updateSize()
    const observer = new ResizeObserver(updateSize)
    observer.observe(marker)
    return () => observer.disconnect()
  }, [])

  useEffect(() => {
    setKeyboardCursor(null)
    setSelectedPointType((currentType) => pointDefinitions.some((point) => point.type === currentType)
      ? currentType : pointDefinitions[0].type)
  }, [pointDefinitions])

  useEffect(() => {
    const marker = markerRef.current
    const wheel = (event) => {
      if (event.ctrlKey || event.metaKey || !event.deltaY) return
      event.preventDefault()
      const bounds = marker.getBoundingClientRect()
      if (!bounds.width || !bounds.height) return
      const anchor = { x: (event.clientX - bounds.left) / bounds.width, y: (event.clientY - bounds.top) / bounds.height }
      const delta = event.deltaY * (event.deltaMode === 1 ? 16 : event.deltaMode === 2 ? bounds.height : 1)
      setView((current) => zoomAt(photo, current, current.zoom * Math.exp(-clamp(delta, -200, 200) * 0.003), anchor, aspect))
    }
    // The listener must be non-passive to keep wheel zoom from scrolling the page.
    marker.addEventListener('wheel', wheel, { passive: false })
    return () => marker.removeEventListener('wheel', wheel)
  }, [photo, aspect])

  function handleMarkerPointerDown(event) {
    if (event.button !== 0 && event.button !== 2) return
    if (dragRef.current) return
    const bounds = event.currentTarget.getBoundingClientRect()
    if (!bounds.width || !bounds.height) return
    event.currentTarget.focus()
    dragRef.current = { pointerId: event.pointerId, button: event.button, x: event.clientX, y: event.clientY, view, bounds, viewBox }
    event.currentTarget.setPointerCapture?.(event.pointerId)
    if (event.button === 2) {
      event.preventDefault()
      setDragging(true)
    }
  }

  function handleMarkerPointerMove(event) {
    const drag = dragRef.current
    if (!drag || drag.pointerId !== event.pointerId || drag.button !== 2) return
    setView(constrainView(photo, { zoom: drag.view.zoom, center: {
      x: drag.view.center.x - (event.clientX - drag.x) / drag.bounds.width * drag.viewBox.width,
      y: drag.view.center.y - (event.clientY - drag.y) / drag.bounds.height * drag.viewBox.height,
    } }, aspect))
  }

  function finishPointer(event) {
    const drag = dragRef.current
    if (!drag || drag.pointerId !== event.pointerId) return
    dragRef.current = null
    setDragging(false)
    if (event.type === 'pointerup' && drag.button === 0 && Math.hypot(event.clientX - drag.x, event.clientY - drag.y) < 5) {
      const point = imagePoint(drag.viewBox, drag.bounds, event.clientX, event.clientY)
      if (point.x >= 0 && point.x <= photo.width && point.y >= 0 && point.y <= photo.height) placePoint(point.x, point.y)
    }
    if (event.currentTarget.hasPointerCapture?.(event.pointerId)) event.currentTarget.releasePointerCapture(event.pointerId)
  }

  function placePoint(x, y) {
    setKeyboardCursor(null)
    const pointWasAlreadyMarked = Boolean(points[selectedPointType])
    const nextPoints = {
      ...points,
      [selectedPointType]: { type: selectedPointType, x, y },
    }

    updateWizardData({ points: nextPoints })

    const nextUnmarkedPoint = pointDefinitions.find((point) => !nextPoints[point.type])
    if (!pointWasAlreadyMarked && nextUnmarkedPoint) {
      setSelectedPointType(nextUnmarkedPoint.type)
    }
  }

  function handleMarkerKeyDown(event) {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault()
      placePoint(cursor.x, cursor.y)
      return
    }
    const movements = {
      ArrowUp: { x: 0, y: -1 },
      ArrowDown: { x: 0, y: 1 },
      ArrowLeft: { x: -1, y: 0 },
      ArrowRight: { x: 1, y: 0 },
    }
    const movement = movements[event.key]
    const currentPoint = cursor

    if (!movement) return

    event.preventDefault()
    const distance = event.shiftKey ? fastNudgePixels : regularNudgePixels
    const nextPoint = {
      ...currentPoint,
      type: selectedPointType,
      x: clamp(currentPoint.x + movement.x * distance, 0, photo.width),
      y: clamp(currentPoint.y + movement.y * distance, 0, photo.height),
    }

    setKeyboardCursor(nextPoint)
    if (points[selectedPointType]) {
      updateWizardData({ points: { ...points, [selectedPointType]: nextPoint } })
    }
    if (nextPoint.x < viewBox.x || nextPoint.x > viewBox.x + viewBox.width
      || nextPoint.y < viewBox.y || nextPoint.y > viewBox.y + viewBox.height) {
      setView(constrainView(photo, { zoom, center: nextPoint }, aspect))
    }
  }

  function changeZoom(direction) {
    setView((current) => zoomAt(photo, current, current.zoom * (direction > 0 ? 1.5 : 1 / 1.5), null, aspect))
  }

  function panView(horizontalDirection, verticalDirection) {
    setView((current) => constrainView(photo, { zoom: current.zoom, center: {
      x: current.center.x + viewBox.width * 0.25 * horizontalDirection,
      y: current.center.y + viewBox.height * 0.25 * verticalDirection,
    } }, aspect))
  }

  function resetView() {
    setView({ zoom: 1, center: { x: photo.width / 2, y: photo.height / 2 } })
  }

  function goToPreviousPoint() {
    if (!canUndoPoint) return

    const nextPoints = { ...points }
    delete nextPoints[pointToUndo.type]

    updateWizardData({ points: nextPoints })
    setKeyboardCursor(null)
    setSelectedPointType(pointToUndo.type)
  }

  return (
    <div className={styles.content}>
      <div>
        <p className={styles.eyebrow}>{t('wizard.marking.eyebrow')}</p>
        <h1 id="wizard-title" className={styles.title}>{t('wizard.marking.title')}</h1>
        <p className={styles.description}>{t('wizard.marking.description')}</p>
      </div>

      <div className={styles.markerFrame}>
        <div className={styles.overlay}>
          <div className={styles.precisionControls} role="group" aria-label={t('wizard.marking.precisionControls')}>
            <div className={styles.controlButtons}>
              <button type="button" className={styles.controlButton} onClick={() => changeZoom(-1)} disabled={zoom <= minimumZoom} aria-label={t('wizard.marking.zoomOut')}>−</button>
              <output className={styles.zoomValue}>{t('wizard.marking.zoomValue', { zoom: Math.round(zoom * 100) })}</output>
              <button type="button" className={styles.controlButton} onClick={() => changeZoom(1)} disabled={zoom >= maximumZoom} aria-label={t('wizard.marking.zoomIn')}>+</button>
            </div>
            <button type="button" className={styles.resetButton} onClick={resetView}>{t('wizard.marking.resetView')}</button>
            <button type="button" className={styles.resetButton} onClick={goToPreviousPoint} disabled={!canUndoPoint}>↶ {t('wizard.marking.previousPoint')}</button>
            <details className={styles.moveControls}>
              <summary>{t('wizard.marking.panLabel')}</summary>
              <div className={styles.panButtons}>
                <button type="button" className={styles.panButton} onClick={() => panView(0, -1)} aria-label={t('wizard.marking.panUp')}>↑</button>
                <button type="button" className={styles.panButton} onClick={() => panView(-1, 0)} aria-label={t('wizard.marking.panLeft')}>←</button>
                <button type="button" className={styles.panButton} onClick={() => panView(1, 0)} aria-label={t('wizard.marking.panRight')}>→</button>
                <button type="button" className={styles.panButton} onClick={() => panView(0, 1)} aria-label={t('wizard.marking.panDown')}>↓</button>
              </div>
            </details>
          </div>
          <div className={styles.guidance}>
            <p className={styles.progress} role="status">{t('wizard.marking.progress', { marked: markedPointCount, total: pointDefinitions.length })}</p>
            <h2 className={styles.pointName} aria-live="polite">{t(selectedPoint.translationKey + '.label')}</h2>
            <p className={styles.pointDescription}>{t(selectedPoint.translationKey + '.description')}</p>
          </div>
        </div>
        <svg ref={markerRef} className={styles.marker} data-dragging={dragging}
          viewBox={`${viewBox.x} ${viewBox.y} ${viewBox.width} ${viewBox.height}`}
          preserveAspectRatio="none" aria-label={t('wizard.marking.canvasLabel')} role="application"
          aria-describedby="marker-instructions marker-keyboard-hint" tabIndex="0"
          onPointerDown={handleMarkerPointerDown} onPointerMove={handleMarkerPointerMove}
          onPointerUp={finishPointer} onPointerCancel={finishPointer} onLostPointerCapture={finishPointer}
          onContextMenu={(event) => event.preventDefault()} onKeyDown={handleMarkerKeyDown}>
          <image href={photo.previewUrl} width={photo.width} height={photo.height} preserveAspectRatio="none" />
          {!points[selectedPointType] && keyboardCursor?.type === selectedPointType && (
            <g className={styles.keyboardCursor} aria-hidden="true">
              <circle cx={cursor.x} cy={cursor.y} r={12 / zoom} />
              <line x1={cursor.x - 16 / zoom} y1={cursor.y} x2={cursor.x + 16 / zoom} y2={cursor.y} />
              <line x1={cursor.x} y1={cursor.y - 16 / zoom} x2={cursor.x} y2={cursor.y + 16 / zoom} />
            </g>
          )}
          {pointDefinitions.map((point, index) => {
            const markedPoint = points[point.type]
            if (!markedPoint) return null
            return (
              <g key={point.type} className={styles.markerPoint} data-selected={point.type === selectedPointType}
                transform={`translate(${markedPoint.x} ${markedPoint.y}) scale(${1 / zoom})`}>
                <path d="M-10 -10L10 10M-10 10L10 -10" />
                <circle r="3" />
                <text x="13" y="-13">{index + 1}</text>
              </g>
            )
          })}
        </svg>
      </div>
      <div className={styles.hints}>
        <p id="marker-instructions">{allPointsMarked ? t('wizard.marking.allPointsMarked') : t('wizard.marking.mouseHint')}</p>
        <details><summary>{t('wizard.marking.keyboardLabel')}</summary><p id="marker-keyboard-hint">{t('wizard.marking.keyboardHint')}</p></details>
      </div>

      <ol className={styles.pointList} aria-label={t('wizard.marking.pointListLabel')}>
        {pointDefinitions.map((point, index) => {
          const isMarked = Boolean(points[point.type])
          const isSelected = point.type === selectedPointType

          return (
            <li key={point.type}>
              <button
                type="button"
                className={styles.pointButton}
                data-selected={isSelected}
                onClick={() => {
                  setKeyboardCursor(null)
                  setSelectedPointType(point.type)
                  if (points[point.type]) setView(constrainView(photo, { zoom, center: points[point.type] }, aspect))
                }}
                aria-pressed={isSelected}
              >
                <span className={styles.pointNumber}>{index + 1}</span>
                <span className={styles.pointLabel}>{t(`${point.translationKey}.label`)}</span>
                <span className={styles.pointStatus} data-marked={isMarked}>
                  {t(isMarked ? 'wizard.marking.marked' : 'wizard.marking.toMark')}
                </span>
              </button>
            </li>
          )
        })}
      </ol>
    </div>
  )
}

export default PointMarker
