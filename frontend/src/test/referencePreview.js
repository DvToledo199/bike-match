// Small synthetic response for contract/UI tests, not a measured bicycle.
export function referencePreview() {
  return {
    leverageCurve: [{ wheelTravelMm: 1, ratio: 3 }, { wheelTravelMm: 150, ratio: 2.5 }],
    kickbackCurve: [{ wheelTravelMm: 0, kickbackDegrees: 0 }, { wheelTravelMm: 150, kickbackDegrees: 25 }],
    axlePath: [{ x: 0, y: 0 }, { x: -5, y: -150 }],
    antiSquatCurve: [{ wheelTravelMm: 0, percent: 120.5 }, { wheelTravelMm: 150, percent: -10.2 }],
    antiRiseCurve: [{ wheelTravelMm: 0, percent: 80.4 }, { wheelTravelMm: 150, percent: 60.1 }],
    leverageDescriptors: {
      lrInitial: 3, lrAtSag: 2.9, lrFinal: 2.5, lrMean: 2.8, totalProgressionPercent: 20,
      usefulProgressionPercent: 16, slopeInitialToSag: -0.01, slopeSagToEnd: -0.01,
      initialTrend: 'PROGRESSIVE', middleTrend: 'PROGRESSIVE', finalTrend: 'PROGRESSIVE', progressionBand: 'MEDIUM',
    },
    axlePathDescriptors: { maxRearwardMm: 5, atTravelPercent: 100 },
    travelCheck: { calculatedTravelMm: 150, declaredTravelMm: 150, deviationPercent: 0, withinTolerance: true },
    conditions: {
      sagPercent: 30, chainringTeeth: 32, sprocketTeeth: 50, modelVersion: 'monopivot-reference-v2',
      reference: { wheelConfiguration: 'MULLET', frontWheelRadiusMm: 371, rearWheelRadiusMm: 352,
        centerOfGravityHeightMm: 1100, photoRotationDegrees: 0, motionModel: 'FIXED_FRAME_LOCAL_GROUND',
        brakeModel: 'SWINGARM_FIXED', validationLevel: 'ANALYTICAL_REFERENCE' },
    },
  }
}
