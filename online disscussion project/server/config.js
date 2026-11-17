import dotenv from 'dotenv'

dotenv.config()

export const port = Number(process.env.PORT || 3001)
export const poolConfig = {
  host: process.env.MYSQL_HOST || '127.0.0.1',
  port: Number(process.env.MYSQL_PORT || 3306),
  database: process.env.MYSQL_DATABASE || 'discussion_tool',
  user: process.env.MYSQL_USER || 'root',
  password: process.env.MYSQL_PASSWORD || '',
  waitForConnections: true,
  connectionLimit: 10,
}

export const demoStudentIds = {
  jdoe23: 1,
  arahman17: 2,
  dkim41: 3,
}
