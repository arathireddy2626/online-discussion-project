import { pool } from '../db.js'

export const registerHealthRoutes = (app) => {
  app.get('/api/health', async (_request, response) => {
    try {
      await pool.query('SELECT 1')
      response.json({ ok: true, database: 'connected' })
    } catch {
      response.status(503).json({ ok: false, error: 'Database unavailable' })
    }
  })
}
