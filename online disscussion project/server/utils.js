import { randomInt } from 'node:crypto'

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

export const pick = (source) => source[randomInt(source.length)]

export const generatePassword = () => {
  const letters = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz'
  const digits = '23456789'
  const specials = '!@#$%^&*'
  const chars = [pick(letters), pick(letters), pick(letters), pick(letters), pick(digits), pick(digits), pick(specials), pick(specials)]
  for (let i = chars.length - 1; i > 0; i -= 1) {
    const j = randomInt(i + 1)
    ;[chars[i], chars[j]] = [chars[j], chars[i]]
  }
  return chars.join('')
}

export const generateUsername = (firstname, lastname) => `${firstname.charAt(0)}${lastname}`.toLowerCase().replace(/[^a-z0-9]/g, '')

export const toKey = (value) => {
  if (!value) return ''
  const date = new Date(value)
  const two = (number) => String(number).padStart(2, '0')
  return `${MONTHS[date.getMonth()]}-${two(date.getDate())}-${date.getFullYear()}:${two(date.getHours())}:${two(date.getMinutes())}:${two(date.getSeconds())}`
}

export const createUniqueUsername = async (db, firstname, lastname) => {
  const base = generateUsername(firstname, lastname)
  for (let attempt = 0; attempt < 200; attempt += 1) {
    const candidate = `${base}${randomInt(10, 100)}`
    const [rows] = await db.execute('SELECT Username FROM LoginCredential WHERE Username = ?', [candidate])
    if (rows.length === 0) return candidate
  }
  throw new Error('Could not create a unique username.')
}
