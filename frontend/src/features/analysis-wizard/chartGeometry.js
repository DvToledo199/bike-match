// Equal domains only mean equal physical scales when the drawable area is square.
export const chartMargin = { top: 16, right: 16, bottom: 0, left: 0 }
export const chartAxisSize = 64

export function relativeAxlePath(points) {
  const start = points[0]
  return points.map((point) => ({ x: point.x - start.x, y: start.y - point.y }))
}

export function getEqualScaleDomains(points) {
  const xs = points.map((point) => point.x)
  const ys = points.map((point) => point.y)
  const minX = Math.min(...xs), maxX = Math.max(...xs)
  const minY = Math.min(...ys), maxY = Math.max(...ys)
  const span = Math.max(maxX - minX, maxY - minY, 1) * 1.15
  const centerX = (minX + maxX) / 2, centerY = (minY + maxY) / 2
  return {
    xDomain: [centerX - span / 2, centerX + span / 2],
    yDomain: [centerY - span / 2, centerY + span / 2],
  }
}

export function getKickbackLevel(curve) {
  // Negative rotation is chain release, not a stronger backwards pedal kick.
  const peak = Math.max(0, ...curve.map((sample) => sample.kickbackDegrees))
  if (peak < 20) return 'low'
  if (peak < 35) return 'medium'
  if (peak < 45) return 'high'
  return 'veryHigh'
}
