/**
 * LeaveDesk Production Database Helper
 * Standardizes MySQL query execution, transactions, and error diagnostics.
 */

const mysql = require('mysql2/promise');
const mysqlDb = require('./mysql-db');

class DBHelper {
  /**
   * Executes a parameterized query against the connection pool with standardized error logging.
   * @param {string} sql
   * @param {Array} params
   * @returns {Promise<Array>} rows
   */
  async query(sql, params = []) {
    if (!mysqlDb.pool) {
      throw new Error("MySQL connection pool is not initialized");
    }
    try {
      const [rows] = await mysqlDb.pool.query(sql, params);
      return rows;
    } catch (err) {
      console.error(`❌ [MySQL Execution Error]: ${err.sqlMessage || err.message}`);
      console.error(`Query: ${sql}`);
      if (params.length > 0) console.error(`Params:`, params);
      throw err;
    }
  }

  /**
   * Executes multiple operations within a managed ACID transaction.
   * Automatically commits on success and rolls back on failure.
   * @param {Function} callback - async (connection) => { ... }
   */
  async transaction(callback) {
    if (!mysqlDb.pool) {
      throw new Error("MySQL connection pool is not initialized");
    }
    const connection = await mysqlDb.pool.getConnection();
    try {
      await connection.beginTransaction();
      const result = await callback(connection);
      await connection.commit();
      return result;
    } catch (err) {
      await connection.rollback();
      console.error(`❌ [MySQL Transaction Aborted]: ${err.sqlMessage || err.message}`);
      throw err;
    } finally {
      connection.release();
    }
  }

  /**
   * Asynchronously writes to audit_logs without interrupting main execution.
   */
  async writeAudit({ actorName, actorRole, action, target, reason, details }) {
    const id = "AUD-" + Math.random().toString(36).substring(2, 8).toUpperCase();
    const sql = `
      INSERT INTO audit_logs (id, actor_name, actor_role, action, target, reason, details)
      VALUES (?, ?, ?, ?, ?, ?, ?)
    `;
    const params = [
      id,
      actorName || 'System',
      actorRole || 'EMPLOYEE',
      action || 'ACTION',
      target || 'SYSTEM',
      reason || 'State mutation',
      typeof details === 'object' ? JSON.stringify(details) : (details || '')
    ];

    try {
      await this.query(sql, params);
      return { id, timestamp: new Date().toISOString(), actorName, actorRole, action, target, reason, details };
    } catch (err) {
      console.error(`⚠️ [Audit Log Write Failed]: ${err.sqlMessage || err.message}`);
      return null;
    }
  }
}

module.exports = new DBHelper();
