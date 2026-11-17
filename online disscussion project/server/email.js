import nodemailer from 'nodemailer'

const mailer = process.env.SMTP_HOST
  ? nodemailer.createTransport({
      host: process.env.SMTP_HOST,
      port: Number(process.env.SMTP_PORT || 587),
      secure: Number(process.env.SMTP_PORT || 587) === 465,
      auth: process.env.SMTP_USER ? { user: process.env.SMTP_USER, pass: process.env.SMTP_PASS } : undefined,
    })
  : null

export const sendEmail = async ({ to, subject, text, attachments }) => {
  if (!mailer) {
    console.log(`\n[EMAIL NOT SENT - set SMTP_HOST in .env]\nTo: ${to}\nSubject: ${subject}\n${text}\n`)
    return false
  }
  try {
    await mailer.sendMail({ from: process.env.MAIL_FROM || 'Dr. Kasi <no-reply@campus.edu>', to, subject, text, attachments })
    return true
  } catch (error) {
    console.error('Email failed:', error.message)
    return false
  }
}

export const sendCredentialsEmail = (email, firstname, username, password) => sendEmail({
  to: email,
  subject: 'Your Software Engineering student account',
  text: `Dear ${firstname},\n\nYour request to enroll in Software Engineering has been approved.\n\nUsername: ${username}\nPassword: ${password}\n\nPlease sign in and change your password.\n\nBest regards,\nDr. Kasi`,
})
