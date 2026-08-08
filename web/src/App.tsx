import { useMemo, useState, type FormEvent } from 'react';
import type { LifeStats } from './types';

const numberFormat = new Intl.NumberFormat('en-US');
const compactFormat = new Intl.NumberFormat('en-US', {
  notation: 'compact',
  maximumFractionDigits: 2,
});

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="stat-card">
      <span className="stat-value">{value}</span>
      <span className="stat-label">{label}</span>
    </div>
  );
}

function LifeGrid({ stats }: { stats: LifeStats }) {
  // Cap the rendered dots so the grid stays performant while remaining
  // representative of a full life laid out one row (year) at a time.
  const weeksPerYear = 52;
  const years = Math.min(Math.ceil(stats.totalWeeks / weeksPerYear), stats.lifeExpectancyYears);
  const cells = [];
  for (let i = 0; i < years * weeksPerYear; i += 1) {
    const lived = i < stats.weeksLived;
    cells.push(<span key={i} className={lived ? 'week lived' : 'week'} />);
  }
  return <div className="life-grid" aria-hidden="true">{cells}</div>;
}

export default function App() {
  const [birthdate, setBirthdate] = useState('1990-05-20');
  const [expectancy, setExpectancy] = useState(80);
  const [stats, setStats] = useState<LifeStats | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const summary = useMemo(() => {
    if (!stats) return null;
    return `You have lived about ${numberFormat.format(stats.weeksLived)} of an estimated ${numberFormat.format(stats.totalWeeks)} weeks.`;
  }, [stats]);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        birthdate,
        expectancy: String(expectancy),
      });
      const response = await fetch(`/api/lifespan?${params.toString()}`);
      const body = await response.json();
      if (!response.ok) {
        throw new Error(body.error ?? 'Request failed');
      }
      setStats(body as LifeStats);
    } catch (err) {
      setStats(null);
      setError(err instanceof Error ? err.message : 'Something went wrong');
    } finally {
      setLoading(false);
    }
  }

  return (
    <main className="app">
      <header className="hero">
        <h1>LifeSpan</h1>
        <p className="tagline">
          Your life, one week at a time. Each dot is a week — the filled ones are
          already behind you.
        </p>
      </header>

      <form className="controls" onSubmit={handleSubmit}>
        <label className="field">
          <span>Date of birth</span>
          <input
            type="date"
            value={birthdate}
            max={new Date().toISOString().slice(0, 10)}
            onChange={(event) => setBirthdate(event.target.value)}
            required
          />
        </label>

        <label className="field">
          <span>Life expectancy: {expectancy} years</span>
          <input
            type="range"
            min={30}
            max={100}
            value={expectancy}
            onChange={(event) => setExpectancy(Number(event.target.value))}
          />
        </label>

        <button type="submit" disabled={loading}>
          {loading ? 'Calculating…' : 'Visualize my life'}
        </button>
      </form>

      {error && <p className="error" role="alert">{error}</p>}

      {stats && (
        <section className="results">
          <p className="summary">{summary}</p>

          <div className="progress" aria-label="Percentage of life lived">
            <div className="progress-bar" style={{ width: `${stats.percentLived}%` }}>
              {stats.percentLived}%
            </div>
          </div>

          <div className="stat-grid">
            <StatCard label="Age" value={`${stats.ageYears} yrs`} />
            <StatCard label="Weeks lived" value={numberFormat.format(stats.weeksLived)} />
            <StatCard label="Weeks remaining" value={numberFormat.format(stats.weeksRemaining)} />
            <StatCard label="Days lived" value={numberFormat.format(stats.daysLived)} />
            <StatCard label="Est. heartbeats" value={compactFormat.format(stats.approxHeartbeats)} />
            <StatCard label="Est. final year" value={stats.estimatedEndDate.slice(0, 4)} />
          </div>

          <LifeGrid stats={stats} />
        </section>
      )}
    </main>
  );
}
