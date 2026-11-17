import bcrypt from 'bcryptjs'
import { pool } from '../db.js'
import { demoStudentIds } from '../config.js'

export const registerAuthRoutes = (app) => {
  app.post('/api/auth/login', async (request, response) => {
    const username = String(request.body.username || '').trim().toLowerCase()
    const password = String(request.body.password || '')
    if (!username || !password) return response.status(400).json({ error: 'Username and password are required.' })
    try {
      const [rows] = await pool.execute(
        `SELECT l.Username, l.Password, l.Role, s.StudentID, s.Firstname, s.Lastname, s.EmailAddress
         FROM LoginCredential l LEFT JOIN StudentRecord s ON s.Username = l.Username
         WHERE LOWER(l.Username) = ?`, [username],
      )
      const user = rows[0]
      if (!user || !(await bcrypt.compare(password, user.Password))) return response.status(401).json({ error: 'Invalid username or password.' })
      const role = user.Role === 'INSTRUCTOR' ? 'Instructor' : 'Student'
      const studentId = user.StudentID || demoStudentIds[user.Username.toLowerCase()] || null
      response.json({ user: {
        id: studentId || user.Username, username: user.Username,
        name: role === 'Instructor' ? 'Dr. Kasi' : `${user.Firstname || ''} ${user.Lastname || user.Username}`.trim(),
        studentId, email: user.EmailAddress || '', role,
      } })
    } catch {
      response.status(500).json({ error: 'Unable to complete login.' })
    }
  })

  app.post('/api/auth/logout', async (_request, response) => response.status(204).send())

  app.post('/api/auth/change-password', async (request, response) => {
    const username = String(request.body.username || '').trim()
    const oldPassword = String(request.body.oldPassword || '')
    const newPassword = String(request.body.newPassword || '')
    if (!username || !oldPassword || !newPassword) return response.status(400).json({ error: 'All password fields are required.' })
    if (!/(?=.*[A-Za-z])(?=.*\d)(?=.*[^A-Za-z\d]).{8,}/.test(newPassword)) return response.status(400).json({ error: 'New password must be at least 8 characters with a letter, number, and special character.' })
    try {
      const [rows] = await pool.execute('SELECT Password FROM LoginCredential WHERE Username = ?', [username])
      if (!rows[0] || !(await bcrypt.compare(oldPassword, rows[0].Password))) return response.status(401).json({ error: 'The old password is incorrect.' })
      await pool.execute('UPDATE LoginCredential SET Password = ? WHERE Username = ?', [await bcrypt.hash(newPassword, 12), username])
      response.status(204).send()
    } catch {
      response.status(500).json({ error: 'Unable to change password.' })
    }
  })
}
