import { pool } from '../db.js'
import { toKey } from '../utils.js'

export const getDiscussionRows = async () => {
  const [rows] = await pool.execute(
    `SELECT d.DiscussionID, d.CourseName, d.DateAndTimeOfCreation, d.Topic,
            m.MessageID, m.DateAndTime, m.Username, m.Contents,
            m.ImageName, m.ImageType, m.ImageData,
            a.DiscussionID AS ArchivedDiscussionID
     FROM Dicussion d
     LEFT JOIN DiscussionToMessage dm ON dm.DicussionID = d.DiscussionID
     LEFT JOIN Message m ON m.MessageID = dm.MessageID
     LEFT JOIN ArchivedDicussions a ON a.DiscussionID = d.DiscussionID
     ORDER BY d.DateAndTimeOfCreation DESC, m.DateAndTime ASC`,
  )
  const discussions = new Map()
  for (const row of rows) {
    if (!discussions.has(row.DiscussionID)) {
      discussions.set(row.DiscussionID, {
        id: String(row.DiscussionID), courseTitle: row.CourseName, topic: row.Topic,
        createdAt: toKey(row.DateAndTimeOfCreation), archived: Boolean(row.ArchivedDiscussionID), entries: [],
      })
    }
    if (row.MessageID) {
      discussions.get(row.DiscussionID).entries.push({
        id: String(row.MessageID), username: row.Username, postedAt: toKey(row.DateAndTime),
        content: row.Contents, imageName: row.ImageName,
        imageUrl: row.ImageData ? `data:image/${row.ImageType};base64,${Buffer.from(row.ImageData).toString('base64')}` : null,
      })
    }
  }
  return [...discussions.values()]
}

export const buildArchiveText = async (discussionId) => {
  const [discussions] = await pool.execute('SELECT CourseName, DateAndTimeOfCreation, Topic FROM Dicussion WHERE DiscussionID = ?', [discussionId])
  if (!discussions[0]) return null
  const [messages] = await pool.execute(
    `SELECT m.Username, m.DateAndTime, m.Contents, m.ImageName
     FROM Message m JOIN DiscussionToMessage dm ON dm.MessageID = m.MessageID
     WHERE dm.DicussionID = ? ORDER BY m.DateAndTime`, [discussionId],
  )
  const header = discussions[0]
  const lines = [`Course: ${header.CourseName}`, `Topic: ${header.Topic}`, `Date and time of creation: ${toKey(header.DateAndTimeOfCreation)}`, '', 'Entries', '-------']
  for (const message of messages) {
    lines.push(`${message.Username} | ${toKey(message.DateAndTime)}`, message.Contents)
    if (message.ImageName) lines.push(`[Picture: ${message.ImageName}]`)
    lines.push('')
  }
  return Buffer.from(lines.join('\n'), 'utf-8')
}
