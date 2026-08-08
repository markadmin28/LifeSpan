import request from 'supertest';
import { describe, expect, it } from 'vitest';
import { createApp } from './index';

const app = createApp();

describe('LifeSpan API', () => {
  it('reports health', async () => {
    const res = await request(app).get('/api/health');
    expect(res.status).toBe(200);
    expect(res.body.status).toBe('ok');
  });

  it('returns stats for a valid request', async () => {
    const res = await request(app)
      .get('/api/lifespan')
      .query({ birthdate: '1990-05-20', expectancy: 80 });
    expect(res.status).toBe(200);
    expect(res.body.birthdate).toBe('1990-05-20');
    expect(res.body.totalWeeks).toBeGreaterThan(0);
    expect(res.body.weeksLived).toBeGreaterThan(0);
  });

  it('rejects an invalid birthdate with 400', async () => {
    const res = await request(app)
      .get('/api/lifespan')
      .query({ birthdate: 'nope' });
    expect(res.status).toBe(400);
    expect(res.body.error).toBeTruthy();
  });
});
