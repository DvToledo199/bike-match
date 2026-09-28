// Full-suspension bikes older than this are unlikely to be analysed.
export const OLDEST_MODEL_YEAR = 2000

/**
 * The model years to offer, newest first. Brands launch next year's models during the
 * current one, so the list starts a year ahead and moves on by itself every year.
 */
export function modelYearOptions(today = new Date()) {
  const newest = today.getFullYear() + 1
  return Array.from({ length: newest - OLDEST_MODEL_YEAR + 1 }, (_, index) => newest - index)
}
