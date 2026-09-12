#!/usr/bin/env node
/* 极简 JDBC SQL 执行器（避免安装 node pg）：
 *   node tools/sql.cjs "select 1"
 *   node tools/sql.cjs -f query.sql
 */
const { spawnSync } = require('child_process')
const fs = require('fs')
const path = require('path')
const os = require('os')

const DRIVER = 'C:/Users/Administrator/.m2/repository/org/postgresql/postgresql/42.7.11/postgresql-42.7.11.jar'
const URL = process.env.DB_URL || 'jdbc:postgresql://localhost:5432/devdb'
const USER = process.env.DB_USER || 'devuser'
const PWD = process.env.DB_PASSWORD || 'devuser123'

let sql
if (process.argv[2] === '-f') sql = fs.readFileSync(process.argv[3], 'utf8')
else sql = process.argv.slice(2).join(' ')

const tmp = path.join(os.tmpdir(), `SqlRunner${Date.now()}.java`)
const src = `
import java.sql.*;
public class SqlRunner {
  public static void main(String[] a) throws Exception {
    String sql = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(a[0])), "UTF-8");
    Class.forName("org.postgresql.Driver");
    try (Connection c = DriverManager.getConnection(a[1], a[2], a[3]);
         Statement st = c.createStatement()) {
      boolean hasRs = st.execute(sql);
      if (hasRs) {
        try (ResultSet rs = st.getResultSet()) {
          ResultSetMetaData m = rs.getMetaData();
          int n = m.getColumnCount();
          StringBuilder h = new StringBuilder();
          for (int i = 1; i <= n; i++) h.append(i > 1 ? " | " : "").append(m.getColumnLabel(i));
          System.out.println(h);
          System.out.println("-".repeat(Math.min(h.length(), 200)));
          int rows = 0;
          while (rs.next() && rows < 500) {
            StringBuilder b = new StringBuilder();
            for (int i = 1; i <= n; i++) b.append(i > 1 ? " | " : "").append(rs.getString(i));
            System.out.println(b);
            rows++;
          }
          System.out.println("(" + rows + " rows)");
        }
      } else {
        System.out.println("OK, updated " + st.getUpdateCount());
      }
    }
  }
}
`
fs.writeFileSync(tmp, src, 'utf8')
const sqlFile = tmp.replace('.java', '.sql')
fs.writeFileSync(sqlFile, sql, 'utf8')

const r = spawnSync('java', [
  '-Dfile.encoding=UTF-8', '-Dstdout.encoding=UTF-8', '-Dstderr.encoding=UTF-8',
  '-cp', DRIVER, tmp, sqlFile, URL, USER, PWD,
], { encoding: 'utf8' })
if (r.stdout) process.stdout.write(r.stdout)
if (r.stderr) process.stderr.write(r.stderr)
try { fs.unlinkSync(tmp); fs.unlinkSync(sqlFile) } catch {}
process.exit(r.status || 0)
