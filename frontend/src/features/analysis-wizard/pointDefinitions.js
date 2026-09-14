export const pointDefinitions = [
  {
    type: 'MAIN_PIVOT',
    translationKey: 'wizard.marking.points.mainPivot',
  },
  {
    type: 'BOTTOM_BRACKET',
    translationKey: 'wizard.marking.points.bottomBracket',
  },
  {
    type: 'SHOCK_FRAME',
    translationKey: 'wizard.marking.points.shockFrame',
  },
  {
    type: 'SHOCK_SWINGARM',
    translationKey: 'wizard.marking.points.shockSwingarm',
  },
  {
    type: 'REAR_AXLE',
    translationKey: 'wizard.marking.points.rearAxle',
  },
  {
    type: 'FRONT_AXLE',
    translationKey: 'wizard.marking.points.frontAxle',
  },
]

export function hasAllPoints(points) {
  return pointDefinitions.every((point) => points[point.type])
}
