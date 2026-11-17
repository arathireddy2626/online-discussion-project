import { pool } from '../db.js'
import { sendEmail } from '../email.js'
import { toKey } from '../utils.js'
import { removeStudent } from '../services/students.js'

export const registerRequestRoutes = (app) => {
  app.post('/api/enrollment-requests', async (request, response) => {
    const studentId = Number(request.body.studentId)
    const type = request.body.type === 'withdrawal' ? 'withdrawal' : 'enrollment'
    if (!studentId) return response.status(400).json({ error: 'Student ID is required.' })
    try {
      if (type === 'enrollment' && request.body.firstname) {
        await pool.execute('INSERT INTO StudentRecord (StudentID, Firstname, Lastname, EmailAddress) VALUES (?, ?, ?, ?)', [studentId, request.body.firstname, request.body.lastname, request.body.email])
      }
      const table = type === 'enrollment' ? 'EnrollQueue' : 'RemoveStudentQueue'
      await pool.execute(`INSERT INTO ${table} (StudentID) VALUES (?)`, [studentId])
      response.status(201).json({ message: 'Request submitted.' })
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'You already have a pending request.' })
      response.status(500).json({ error: 'Unable to submit request.' })
    }
  })

  app.post('/api/students/:studentId/withdraw', async (request, response) => {
    const studentId = Number(request.params.studentId)
    try {
      const [rows] = await pool.execute('SELECT Firstname, EmailAddress FROM StudentRecord WHERE StudentID = ?', [studentId])
      await removeStudent(studentId)
      const student = rows[0]
      if (student) await sendEmail({ to: student.EmailAddress, subject: 'Course withdrawal confirmation: Software Engineering', text: `Dear ${student.Firstname},\n\nYour request to withdraw from Software Engineering has been processed. Your student account has been removed, while your discussion messages have been retained.\n\nBest regards,\nDr. Kasi` })
      response.status(204).send()
    } catch {
      response.status(500).json({ error: 'Unable to process withdrawal.' })
    }
  })

  app.get('/api/enrollment-requests', async (_request, response) => {
    try {
      const [enrollments] = await pool.execute(`SELECT CONCAT('enroll-', e.StudentID) AS id, 'enrollment' AS type, s.StudentID AS studentId, s.Firstname AS firstname, s.Lastname AS lastname, s.EmailAddress AS email, 'Pending review' AS status FROM EnrollQueue e JOIN StudentRecord s ON s.StudentID = e.StudentID`)
      const [withdrawals] = await pool.execute(`SELECT CONCAT('withdraw-', r.StudentID) AS id, 'withdrawal' AS type, s.StudentID AS studentId, s.Firstname AS firstname, s.Lastname AS lastname, s.EmailAddress AS email, 'Pending review' AS status FROM RemoveStudentQueue r JOIN StudentRecord s ON s.StudentID = r.StudentID`)
      response.json({ requests: [...enrollments, ...withdrawals] })
    } catch {
      response.status(500).json({ error: 'Unable to load enrollment requests.' })
    }
  })

  app.get('/api/archive-requests', async (request, response) => {
    const username = String(request.query.username || '').trim()
    try {
      const params = username ? [username] : []
      const where = username ? 'WHERE s.Username = ?' : ''
      const [rows] = await pool.execute(`SELECT r.StudentID, r.Topic, r.Status, r.RequestedAt, COALESCE(r.DiscussionID, d.DiscussionID) AS DiscussionID, s.Username, s.EmailAddress FROM RequestToRetrieveDiscussion r JOIN StudentRecord s ON s.StudentID = r.StudentID LEFT JOIN Dicussion d ON d.Topic = r.Topic ${where} ORDER BY r.RequestedAt DESC`, params)
      response.json({ requests: rows.map((row) => ({ id: `archive-${row.StudentID}-${row.DiscussionID}`, studentId: row.StudentID, studentUsername: row.Username || '', studentEmail: row.EmailAddress, topic: row.Topic, archiveId: String(row.DiscussionID), requestedAt: toKey(row.RequestedAt), status: row.Status === 'APPROVED' ? 'Approved' : 'Pending review' })) })
    } catch {
      response.status(500).json({ error: 'Unable to load archive requests.' })
    }
  })

  app.post('/api/archive-requests', async (request, response) => {
    const studentId = Number(request.body.studentId)
    const topic = String(request.body.topic || '').trim()
    if (!studentId || !topic) return response.status(400).json({ error: 'Student ID and topic are required.' })
    try {
      const [matches] = await pool.execute('SELECT d.DiscussionID FROM Dicussion d JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID WHERE d.Topic = ?', [topic])
      if (!matches[0]) return response.status(404).json({ error: 'No archived discussion matches that topic or key.' })
      await pool.execute('INSERT INTO RequestToRetrieveDiscussion (StudentID, Topic, DiscussionID) VALUES (?, ?, ?)', [studentId, topic, matches[0].DiscussionID])
      response.status(201).json({ message: 'Archive request submitted.' })
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'You already requested this discussion.' })
      response.status(500).json({ error: 'Unable to submit archive request.' })
    }
  })

  app.post('/api/archive-requests/approve', async (request, response) => {
    const studentId = Number(request.body.studentId)
    const topic = String(request.body.topic || '').trim()
    if (!studentId || !topic) return response.status(400).json({ error: 'Student ID and topic are required.' })
    try {
      const [rows] = await pool.execute('SELECT s.Firstname, s.EmailAddress, a.FileName, a.FileData FROM RequestToRetrieveDiscussion r JOIN StudentRecord s ON s.StudentID = r.StudentID JOIN Dicussion d ON d.Topic = r.Topic JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID WHERE r.StudentID = ? AND r.Topic = ?', [studentId, topic])
      const match = rows[0]
      if (!match) return response.status(404).json({ error: 'Archive request was not found.' })
      await pool.execute("UPDATE RequestToRetrieveDiscussion SET Status = 'APPROVED' WHERE StudentID = ? AND Topic = ?", [studentId, topic])
      await sendEmail({ to: match.EmailAddress, subject: `Archived discussion: ${topic}`, text: `Dear ${match.Firstname},\n\nYour request for the archived discussion "${topic}" has been approved. A copy is attached.\n\nBest regards,\nDr. Kasi`, attachments: match.FileData ? [{ filename: match.FileName || 'discussion.txt', content: Buffer.from(match.FileData) }] : [] })
      response.json({ message: 'Archive request approved.' })
    } catch {
      response.status(500).json({ error: 'Unable to approve archive request.' })
    }
  })

  app.get('/api/discussion-requests', async (_request, response) => {
    try {
      const [rows] = await pool.execute('SELECT r.StudentID AS studentId, r.Topic AS topic, s.Username AS studentUsername FROM ReuqestToCreateDiscussion r JOIN StudentRecord s ON s.StudentID = r.StudentID ORDER BY r.Topic')
      response.json({ requests: rows.map((row) => ({ ...row, studentUsername: row.studentUsername || '' })) })
    } catch {
      response.status(500).json({ error: 'Unable to load discussion requests.' })
    }
  })

  app.post('/api/discussion-requests', async (request, response) => {
    const studentId = Number(request.body.studentId)
    const topic = String(request.body.topic || '').trim()
    if (!studentId || !topic) return response.status(400).json({ error: 'Student ID and topic are required.' })
    if (topic.length > 80) return response.status(400).json({ error: 'Topic must be 80 characters or fewer.' })
    try {
      await pool.execute('INSERT INTO ReuqestToCreateDiscussion (StudentID, Topic) VALUES (?, ?)', [studentId, topic])
      response.status(201).json({ message: 'Discussion request submitted.' })
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'You already requested this topic.' })
      response.status(500).json({ error: 'Unable to submit discussion request.' })
    }
  })

  app.delete('/api/discussion-requests', async (request, response) => {
    try {
      await pool.execute('DELETE FROM ReuqestToCreateDiscussion WHERE StudentID = ? AND Topic = ?', [Number(request.body.studentId), String(request.body.topic || '').trim()])
      response.status(204).send()
    } catch {
      response.status(500).json({ error: 'Unable to remove discussion request.' })
    }
  })
}
