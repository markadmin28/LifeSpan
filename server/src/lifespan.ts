const MS_PER_DAY = 1000 * 60 * 60 * 24;
const DAYS_PER_YEAR = 365.2425;

export const MIN_EXPECTANCY = 1;
export const MAX_EXPECTANCY = 122;

export interface LifeStats {
  birthdate: string;
  lifeExpectancyYears: number;
  ageYears: number;
  daysLived: number;
  weeksLived: number;
  totalWeeks: number;
  weeksRemaining: number;
  percentLived: number;
  estimatedEndDate: string;
  approxHeartbeats: number;
}

export class ValidationError extends Error {}

/**
 * Parse a strict `YYYY-MM-DD` date string into a UTC Date, rejecting
 * malformed values and impossible calendar dates (e.g. 2023-02-31).
 */
function parseIsoDate(value: string): Date {
  const match = /^(\d{4})-(\d{2})-(\d{2})$/.exec(value.trim());
  if (!match) {
    throw new ValidationError('birthdate must be in YYYY-MM-DD format');
  }
  const [, y, m, d] = match;
  const year = Number(y);
  const month = Number(m);
  const day = Number(d);
  const date = new Date(Date.UTC(year, month - 1, day));
  const roundTrips =
    date.getUTCFullYear() === year &&
    date.getUTCMonth() === month - 1 &&
    date.getUTCDate() === day;
  if (!roundTrips) {
    throw new ValidationError('birthdate is not a valid calendar date');
  }
  return date;
}

/**
 * Compute a set of "life in weeks" statistics for a given birthdate and life
 * expectancy. Pure and deterministic aside from the injectable `now`.
 */
export function computeLifeStats(
  birthdate: string,
  lifeExpectancyYears = 80,
  now: Date = new Date(),
): LifeStats {
  const birth = parseIsoDate(birthdate);

  if (!Number.isFinite(lifeExpectancyYears)) {
    throw new ValidationError('lifeExpectancyYears must be a number');
  }
  if (lifeExpectancyYears < MIN_EXPECTANCY || lifeExpectancyYears > MAX_EXPECTANCY) {
    throw new ValidationError(
      `lifeExpectancyYears must be between ${MIN_EXPECTANCY} and ${MAX_EXPECTANCY}`,
    );
  }
  if (birth.getTime() > now.getTime()) {
    throw new ValidationError('birthdate cannot be in the future');
  }

  const msLived = now.getTime() - birth.getTime();
  const daysLived = Math.floor(msLived / MS_PER_DAY);
  const weeksLived = Math.floor(daysLived / 7);
  const ageYears = msLived / MS_PER_DAY / DAYS_PER_YEAR;

  const totalDays = Math.round(lifeExpectancyYears * DAYS_PER_YEAR);
  const totalWeeks = Math.floor(totalDays / 7);
  const weeksRemaining = Math.max(0, totalWeeks - weeksLived);
  const percentLived = Math.min(100, (weeksLived / totalWeeks) * 100);

  const estimatedEnd = new Date(birth.getTime() + totalDays * MS_PER_DAY);
  const approxHeartbeats = Math.round((daysLived * 24 * 60 * 70) / 1_000_000) * 1_000_000;

  return {
    birthdate,
    lifeExpectancyYears,
    ageYears: Number(ageYears.toFixed(2)),
    daysLived,
    weeksLived,
    totalWeeks,
    weeksRemaining,
    percentLived: Number(percentLived.toFixed(2)),
    estimatedEndDate: estimatedEnd.toISOString().slice(0, 10),
    approxHeartbeats,
  };
}
