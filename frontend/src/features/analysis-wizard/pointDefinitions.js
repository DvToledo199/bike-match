const singlePivotPointDefinitions = [
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

const horstLinkPointDefinitions = [
  {
    type: 'MAIN_PIVOT',
    translationKey: 'wizard.marking.points.mainPivot',
  },
  {
    type: 'BOTTOM_BRACKET',
    translationKey: 'wizard.marking.points.bottomBracket',
  },
  {
    type: 'HORST_PIVOT',
    translationKey: 'wizard.marking.points.horstPivot',
  },
  {
    type: 'ROCKER_FRAME_PIVOT',
    translationKey: 'wizard.marking.points.rockerFramePivot',
  },
  {
    type: 'ROCKER_SEATSTAY_PIVOT',
    translationKey: 'wizard.marking.points.rockerSeatstayPivot',
  },
  {
    type: 'SHOCK_FRAME',
    translationKey: 'wizard.marking.points.shockFrame',
  },
  {
    type: 'SHOCK_ROCKER',
    translationKey: 'wizard.marking.points.shockRocker',
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

const horstLinkYokePointDefinitions = [
  {
    type: 'MAIN_PIVOT',
    translationKey: 'wizard.marking.points.mainPivot',
  },
  {
    type: 'BOTTOM_BRACKET',
    translationKey: 'wizard.marking.points.bottomBracket',
  },
  {
    type: 'HORST_PIVOT',
    translationKey: 'wizard.marking.points.horstPivot',
  },
  {
    type: 'ROCKER_FRAME_PIVOT',
    translationKey: 'wizard.marking.points.rockerFramePivot',
  },
  {
    type: 'ROCKER_SEATSTAY_PIVOT',
    translationKey: 'wizard.marking.points.rockerSeatstayPivot',
  },
  {
    type: 'SHOCK_FRAME',
    translationKey: 'wizard.marking.points.shockFrame',
  },
  {
    type: 'YOKE_ROCKER_PIVOT',
    translationKey: 'wizard.marking.points.yokeRockerPivot',
  },
  {
    type: 'SHOCK_YOKE_EYE',
    translationKey: 'wizard.marking.points.shockYokeEye',
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

const pointDefinitionsByLayout = {
  SINGLE_PIVOT: singlePivotPointDefinitions,
  HORST_LINK: horstLinkPointDefinitions,
  HORST_LINK_YOKE: horstLinkYokePointDefinitions,
}

export function getPointDefinitions(suspensionLayout = 'SINGLE_PIVOT') {
  return pointDefinitionsByLayout[suspensionLayout] ?? singlePivotPointDefinitions
}

export function hasAllPoints(points, suspensionLayout) {
  return getPointDefinitions(suspensionLayout).every((point) => points[point.type])
}
