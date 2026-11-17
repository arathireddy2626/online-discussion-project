import mysql from 'mysql2/promise'
import { poolConfig } from './config.js'

export const pool = mysql.createPool(poolConfig)
