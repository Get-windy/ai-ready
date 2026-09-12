// 实体字段 vs 数据库列 一致性核查（金标准：实体声明的列必须真实存在，否则 page/save 必 500）
const fs = require('fs')
const path = require('path')
const { Client } = require(path.join(__dirname, '../../frontend/apps/pc-admin/node_modules/pg'))

const SRC = 'backend/erp/erp-sales/src/main/java/cn/aiedge/erp/sale'
const PAIRS = [
  [`${SRC}/saleexchange/entity/SaleExchange.java`, 'erp_sale_exchange'],
  [`${SRC}/saleexchange/entity/SaleExchangeItem.java`, 'erp_sale_exchange_item'],
  [`${SRC}/returnDoc/entity/SaleReturnDoc.java`, 'erp_sale_return_doc'],
  [`${SRC}/returnDoc/entity/SaleReturnDocItem.java`, 'erp_sale_return_doc_item'],
  [`${SRC}/entity/SaleOrder.java`, 'erp_sale_order'],
  [`${SRC}/entity/SaleOrderItem.java`, 'erp_sale_order_item'],
  [`${SRC}/outbound/entity/SaleOutbound.java`, 'erp_sale_outbound'],
  [`${SRC}/outbound/entity/SaleOutboundItem.java`, 'erp_sale_outbound_item'],
]

function camelToSnake(s) {
  return s.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toLowerCase()
}
// 解析实体：跳过 @TableField(exist = false) 的字段
function parseFields(file) {
  const src = fs.readFileSync(file, 'utf8')
  const lines = src.split(/\r?\n/)
  const out = []
  let pendingExistFalse = false
  for (const line of lines) {
    if (/@TableField\s*\(\s*exist\s*=\s*false/.test(line)) { pendingExistFalse = true; continue }
    const m = line.match(/^\s*private\s+[\w<>,.\s\[\]]+\s+(\w+)\s*;/)
    if (m) {
      if (!pendingExistFalse) out.push(m[1])
      pendingExistFalse = false
    } else if (/^\s*private\s/.test(line)) {
      // 多行声明等，忽略
    } else if (!/^\s*(\/\/|\*|\/\*|@)/.test(line) && line.trim()) {
      pendingExistFalse = false
    }
  }
  return out
}

;(async () => {
  const client = new Client({ host: 'localhost', port: 5432, database: 'devdb', user: 'devuser', password: 'devuser123' })
  await client.connect()
  for (const [file, table] of PAIRS) {
    if (!fs.existsSync(file)) { console.log(`SKIP missing ${file}`); continue }
    const fields = parseFields(file)
    const res = await client.query(
      `SELECT column_name FROM information_schema.columns WHERE table_name=$1`, [table])
    const cols = new Set(res.rows.map(r => r.column_name))
    const missing = fields.filter(f => !cols.has(camelToSnake(f)))
    console.log(`\n### ${table}  实体字段=${fields.length} 表列=${cols.size} 缺失=${missing.length}`)
    if (missing.length) console.log('  MISSING: ' + missing.map(camelToSnake).join(', '))
  }
  await client.end()
})().catch(e => { console.error(e); process.exit(1) })
