import express from 'express'
import cors from 'cors'
import { port } from './config.js'
import { registerAuthRoutes } from './routes/auth.js'
import { registerDiscussionRoutes } from './routes/discussions.js'
import { registerHealthRoutes } from './routes/health.js'
import { registerRequestRoutes } from './routes/requests.js'
import { registerStudentRoutes } from './routes/students.js'

const app = express()

app.use(cors({ origin: true, credentials: true }))
app.use(express.json({ limit: '10mb' }))

registerHealthRoutes(app)
registerStudentRoutes(app)
registerDiscussionRoutes(app)
registerRequestRoutes(app)
registerAuthRoutes(app)

app.listen(port, () => {
  console.log(`Discussion tool API listening on http://localhost:${port}`)
})
