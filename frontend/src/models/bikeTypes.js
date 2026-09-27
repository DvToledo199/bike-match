// A rider knows what kind of bike they have more readily than how many teeth it carries, so the
// wizard asks for the type and uses the gearing each type is usually sold with. The largest cog
// is the one used, because it is where pedal kickback is strongest.
export const bikeTypes = {
  ENDURO: { chainringTeeth: 32, sprocketTeeth: 52, cassetteType: 'TWELVE_SPEED' },
  E_ENDURO: { chainringTeeth: 34, sprocketTeeth: 52, cassetteType: 'TWELVE_SPEED' },
  DOWNHILL: { chainringTeeth: 36, sprocketTeeth: 25, cassetteType: 'DH_7_8' },
}

export const bikeTypeNames = Object.keys(bikeTypes)

// Every bike is read at the same sag, so the figures "at sag" can be compared between bikes.
export const REFERENCE_SAG_PERCENT = 30

/** The values the kinematics engine needs, derived from the bike type. */
export function referenceSetupFor(bikeType) {
  const { chainringTeeth, sprocketTeeth, cassetteType } = bikeTypes[bikeType]
  return { chainringTeeth, sprocketTeeth, cassetteType, sagPercent: REFERENCE_SAG_PERCENT }
}
