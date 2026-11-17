import bcrypt from 'bcryptjs'
import { pool } from '../db.js'
import { createUniqueUsername, generatePassword } from '../utils.js'
import { sendCredentialsEmail, sendEmail } from '../email.js'

export const createStudent = async ({ studentId, firstname, lastname, email }) => {
  const connection = await pool.getConnection()
  try {
    await connection.beginTransaction()
    const username = await createUniqueUsername(connection, firstname, lastname)
    const password = generatePassword()
    const passwordHash = await bcrypt.hash(password, 12)
    await connection.execute('INSERT INTO LoginCredential (Username, Password, Role) VALUES (?, ?, \'STUDENT\')', [username, passwordHash])
    await connection.execute(
      'INSERT INTO StudentRecord (StudentID, Firstname, Lastname, EmailAddress, Username) VALUES (?, ?, ?, ?, ?)',
      [studentId, firstname, lastname, email, username],
    )
    await connection.execute(
      'INSERT INTO course_student (course_number, student_id) VALUES (?, ?) ON DUPLICATE KEY UPDATE enrolled_at = enrolled_at',
      ['SWE-101', studentId],
    )
    await connection.commit()
    await sendCredentialsEmail(email, firstname, username, password)
    return { id: studentId, firstname, lastname, email, username, role: 'Student' }
  } catch (error) {
    await connection.rollback()
    throw error
  } finally {
    connection.release()
  }
}

export const removeStudent = async (studentId) => {
  const [rows] = await pool.execute('SELECT Username FROM StudentRecord WHERE StudentID = ?', [studentId])
  const username = rows[0]?.Username
  await pool.execute('DELETE FROM EnrollQueue WHERE StudentID = ?', [studentId])
  await pool.execute('DELETE FROM RemoveStudentQueue WHERE StudentID = ?', [studentId])
  await pool.execute('DELETE FROM ReuqestToCreateDiscussion WHERE StudentID = ?', [studentId])
  await pool.execute('DELETE FROM RequestToRetrieveDiscussion WHERE StudentID = ?', [studentId])
  await pool.execute('DELETE FROM StudentRecord WHERE StudentID = ?', [studentId])
  if (username) await pool.execute('DELETE FROM LoginCredential WHERE Username = ?', [username])
}
