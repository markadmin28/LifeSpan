import cors from 'cors';
import express, { type Request, type Response } from 'express';
import { computeLifeStats, ValidationError } from './lifespan';

export function createApp() {
  const app = express();
  app.use(cors());
  app.use(express.json());

  app.get('/api/health', (_req: Request, res: Response) => {
    res.json({ status: 'ok', service: 'lifespan-server' });
  });

  app.get('/api/lifespan', (req: Request, res: Response) => {
    const birthdate = String(req.query.birthdate ?? '');
    const expectancyRaw = req.query.expectancy;
    const lifeExpectancyYears =
      expectancyRaw === undefined ? 80 : Number(expectancyRaw);

    try {
      const stats = computeLifeStats(birthdate, lifeExpectancyYears);
      res.json(stats);
    } catch (error) {
      if (error instanceof ValidationError) {
        res.status(400).json({ error: error.message });
        return;
      }
      throw error;
    }
  });

  return app;
}

const PORT = Number(process.env.PORT ?? 3001);

if (require.main === module) {
  const app = createApp();
  app.listen(PORT, () => {
    console.log(`LifeSpan API listening on http://localhost:${PORT}`);
  });
}
