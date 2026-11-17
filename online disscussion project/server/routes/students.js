import bcrypt from 'bcryptjs'
import { pool } from '../db.js'
import { createUniqueUsername, generatePassword } from '../utils.js'
import { sendCredentialsEmail } from '../email.js'
import { createStudent, removeStudent } from '../services/students.js'

export const registerStudentRoutes = (app) => {
  app.get('/api/students', async (_request, response) => {
    try {
      const [rows] = await pool.execute(
        `SELECT StudentID AS id, Firstname AS firstname, Lastname AS lastname,
                EmailAddress AS email, Username AS username
         FROM StudentRecord ORDER BY StudentID`,
      )
      response.json(rows.map((student) => ({ ...student, username: student.username || '', role: 'Student' })))
    } catch {
      response.status(500).json({ error: 'Unable to load students.' })
    }
  })

  app.post('/api/students', async (request, response) => {
    const studentId = Number(request.body.studentId)
    const firstname = String(request.body.firstname || '').trim()
    const lastname = String(request.body.lastname || '').trim()
    const email = String(request.body.email || '').trim()
    if (!studentId || !firstname || !lastname || !email) return response.status(400).json({ error: 'All student fields are required.' })
    try {
      response.status(201).json(await createStudent({ studentId, firstname, lastname, email }))
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'A student or username already exists.' })
      response.status(500).json({ error: 'Unable to create student.' })
    }
  })

  app.delete('/api/students/:studentId', async (request, response) => {
    try {
      await removeStudent(Number(request.params.studentId))
      response.status(204).send()
    } catch {
      response.status(500).json({ error: 'Unable to remove student.' })
    }
  })

  app.post('/api/students/:studentId/credentials', async (request, response) => {
    const studentId = Number(request.params.studentId)
    if (!studentId) return response.status(400).json({ error: 'Student ID is required.' })
    try {
      const [students] = await pool.execute('SELECT Firstname, Lastname, EmailAddress FROM StudentRecord WHERE StudentID = ?', [studentId])
      const student = students[0]
      if (!student) return response.status(404).json({ error: 'Student record was not found.' })
      const username = await createUniqueUsername(pool, student.Firstname, student.Lastname)
      const password = generatePassword()
      const passwordHash = await bcrypt.hash(password, 12)
      await pool.execute('INSERT INTO LoginCredential (Username, Password, Role) VALUES (?, ?, \'STUDENT\')', [username, passwordHash])
      await pool.execute('UPDATE StudentRecord SET Username = ? WHERE StudentID = ?', [username, studentId])
      await pool.execute('DELETE FROM EnrollQueue WHERE StudentID = ?', [studentId])
      await sendCredentialsEmail(student.EmailAddress, student.Firstname, username, password)
      response.status(201).json({ username, password, email: student.EmailAddress })
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'These login credentials already exist.' })
      response.status(500).json({ error: 'Unable to save student credentials.' })
    }
  })
}
