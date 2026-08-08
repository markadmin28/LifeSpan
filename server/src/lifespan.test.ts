import { describe, expect, it } from 'vitest';
import { computeLifeStats, MAX_EXPECTANCY, ValidationError } from './lifespan';

const NOW = new Date('2024-01-01T00:00:00.000Z');

describe('computeLifeStats', () => {
  it('computes weeks lived and remaining for a known date', () => {
    const stats = computeLifeStats('1994-01-01', 80, NOW);
    expect(stats.daysLived).toBe(10957);
    expect(stats.weeksLived).toBe(1565);
    expect(stats.ageYears).toBeCloseTo(30, 1);
    expect(stats.weeksRemaining).toBe(stats.totalWeeks - stats.weeksLived);
    expect(stats.percentLived).toBeGreaterThan(0);
    expect(stats.percentLived).toBeLessThan(100);
  });

  it('defaults life expectancy to 80 years', () => {
    const stats = computeLifeStats('2000-06-15', undefined, NOW);
    expect(stats.lifeExpectancyYears).toBe(80);
    expect(stats.estimatedEndDate).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('caps percentLived at 100 for someone past their expectancy', () => {
    const stats = computeLifeStats('1900-01-01', 80, NOW);
    expect(stats.percentLived).toBe(100);
    expect(stats.weeksRemaining).toBe(0);
  });

  it('rejects malformed dates', () => {
    expect(() => computeLifeStats('01-01-1990', 80, NOW)).toThrow(ValidationError);
    expect(() => computeLifeStats('not-a-date', 80, NOW)).toThrow(ValidationError);
  });

  it('rejects impossible calendar dates', () => {
    expect(() => computeLifeStats('2023-02-31', 80, NOW)).toThrow(ValidationError);
  });

  it('rejects future birthdates', () => {
    expect(() => computeLifeStats('2999-01-01', 80, NOW)).toThrow(ValidationError);
  });

  it('rejects out-of-range life expectancy', () => {
    expect(() => computeLifeStats('1990-01-01', 0, NOW)).toThrow(ValidationError);
    expect(() => computeLifeStats('1990-01-01', MAX_EXPECTANCY + 1, NOW)).toThrow(
      ValidationError,
    );
  });
});
