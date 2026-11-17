import { pool } from '../db.js'
import { buildArchiveText, getDiscussionRows } from '../services/discussions.js'

export const registerDiscussionRoutes = (app) => {
  app.get('/api/discussions', async (_request, response) => {
    try {
      response.json({ discussions: (await getDiscussionRows()).filter((discussion) => !discussion.archived) })
    } catch {
      response.status(500).json({ error: 'Unable to load discussions.' })
    }
  })

  app.get('/api/archives', async (_request, response) => {
    try {
      response.json({ discussions: (await getDiscussionRows()).filter((discussion) => discussion.archived) })
    } catch {
      response.status(500).json({ error: 'Unable to load archived discussions.' })
    }
  })

  app.post('/api/discussions', async (request, response) => {
    const topic = String(request.body.topic || '').trim()
    const content = String(request.body.content || '').trim()
    const username = String(request.body.username || '').trim()
    if (!topic || !username) return response.status(400).json({ error: 'Topic and username are required.' })
    if (topic.length > 80) return response.status(400).json({ error: 'Topic must be 80 characters or fewer.' })
    const connection = await pool.getConnection()
    try {
      await connection.beginTransaction()
      const [discussionResult] = await connection.execute('INSERT INTO Dicussion (CourseName, DateAndTimeOfCreation, Topic) VALUES (?, UTC_TIMESTAMP(), ?)', ['Software Engineering', topic])
      if (content) {
        const [messageResult] = await connection.execute('INSERT INTO Message (DateAndTime, Username, Contents) VALUES (UTC_TIMESTAMP(), ?, ?)', [username, content])
        await connection.execute('INSERT INTO DiscussionToMessage (DicussionID, MessageID, EntryNo) VALUES (?, ?, ?)', [discussionResult.insertId, messageResult.insertId, `${discussionResult.insertId}.1`])
      }
      await connection.commit()
      response.status(201).json({ id: String(discussionResult.insertId) })
    } catch (error) {
      await connection.rollback()
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'A discussion was created at the same time. Please try again.' })
      response.status(500).json({ error: 'Unable to create discussion.' })
    } finally {
      connection.release()
    }
  })

  app.post('/api/discussions/:discussionId/messages', async (request, response) => {
    const discussionId = Number(request.params.discussionId)
    const username = String(request.body.username || '').trim()
    const content = String(request.body.content || '').trim()
    const imageName = String(request.body.imageName || '').trim() || null
    const imageType = String(request.body.imageType || '').toLowerCase() || null
    const imageData = request.body.imageData ? Buffer.from(String(request.body.imageData).replace(/^data:image\/[^;]+;base64,/, ''), 'base64') : null
    if (!discussionId || !username || (!content && !imageData)) return response.status(400).json({ error: 'Discussion, username, and a message or picture are required.' })
    if (imageType && !['png', 'jpg', 'gif'].includes(imageType)) return response.status(400).json({ error: 'Only PNG, JPG, and GIF images are allowed.' })
    try {
      const [archived] = await pool.execute('SELECT DiscussionID FROM ArchivedDicussions WHERE DiscussionID = ?', [discussionId])
      if (archived.length > 0) return response.status(409).json({ error: 'Archived discussions cannot receive messages.' })
      const [messageResult] = await pool.execute('INSERT INTO Message (DateAndTime, Username, Contents, ImageName, ImageType, ImageData) VALUES (UTC_TIMESTAMP(), ?, ?, ?, ?, ?)', [username, content, imageName, imageType, imageData])
      const [nextRows] = await pool.execute("SELECT COALESCE(MAX(CAST(SUBSTRING_INDEX(EntryNo, '.', -1) AS UNSIGNED)), 0) + 1 AS nextNo FROM DiscussionToMessage WHERE DicussionID = ?", [discussionId])
      await pool.execute('INSERT INTO DiscussionToMessage (DicussionID, MessageID, EntryNo) VALUES (?, ?, ?)', [discussionId, messageResult.insertId, `${discussionId}.${nextRows[0].nextNo}`])
      response.status(201).json({ id: String(messageResult.insertId) })
    } catch {
      response.status(500).json({ error: 'Unable to post message.' })
    }
  })

  app.delete('/api/messages/:messageId', async (request, response) => {
    try {
      await pool.execute('DELETE FROM DiscussionToMessage WHERE MessageID = ?', [Number(request.params.messageId)])
      await pool.execute('DELETE FROM Message WHERE MessageID = ?', [Number(request.params.messageId)])
      response.status(204).send()
    } catch {
      response.status(500).json({ error: 'Unable to remove message.' })
    }
  })

  app.post('/api/discussions/:discussionId/archive', async (request, response) => {
    const discussionId = Number(request.params.discussionId)
    try {
      const fileData = await buildArchiveText(discussionId)
      if (!fileData) return response.status(404).json({ error: 'Discussion was not found.' })
      await pool.execute('INSERT INTO ArchivedDicussions (DiscussionID, FileName, FileType, FileData) VALUES (?, ?, ?, ?)', [discussionId, `discussion-${discussionId}.txt`, 'txt', fileData])
      response.status(204).send()
    } catch (error) {
      if (error.code === 'ER_DUP_ENTRY') return response.status(409).json({ error: 'This discussion is already archived.' })
      response.status(500).json({ error: 'Unable to archive discussion.' })
    }
  })
}
