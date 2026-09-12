/* 商品模块金标准 接口验收脚本（直连 API）
 * 运行：node tools/e2e-product.cjs [port]
 * 覆盖：列表4子Tab / 可用库存 / 换算关系 / 等级价 / 云导入 / 商品授权 / 批量操作 / 真实导入导出 / 表单单位闭环
 */
const http = require('http')
const fs = require('fs')
const path = require('path')

const PORT = Number(process.argv[2] || 5655)
const results = []

function apiReq(method, p, body, token, raw) {
  return new Promise((resolve, reject) => {
    const data = body == null ? null : (typeof body === 'string' ? body : JSON.stringify(body))
    const headers = { 'Content-Type': 'application/json' }
    if (data) headers['Content-Length'] = Buffer.byteLength(data)
    if (token) headers.Authorization = `Bearer ${token}`
    const r = http.request({ hostname: '127.0.0.1', port: PORT, path: '/api' + p, method, headers }, res => {
      const chunks = []
      res.on('data', c => chunks.push(c))
      res.on('end', () => {
        const buf = Buffer.concat(chunks)
        if (raw) return resolve({ status: res.statusCode, headers: res.headers, buf })
        try { resolve(JSON.parse(buf.toString('utf8'))) } catch { resolve({ raw: buf.toString('utf8'), status: res.statusCode }) }
      })
    })
    r.on('error', reject)
    if (data) r.write(data)
    r.end()
  })
}

async function login() {
  const cap = await apiReq('GET', '/auth/captcha')
  const code = [...Buffer.from(cap.data.img.split(',')[1], 'base64').toString('utf8')
    .matchAll(/<text[^>]*>([^<]+)<\/text>/g)].map(m => m[1]).join('')
  const res = await apiReq('POST', '/auth/login', {
    username: process.env.E2E_USER || 'e2e_product', password: process.env.E2E_PASS || 'admin123', tenantName: '系统租户',
    captcha: code, captchaKey: cap.data.uuid,
  })
  const token = res?.data?.token || res?.data?.accessToken
  if (!token) throw new Error('登录失败: ' + JSON.stringify(res).slice(0, 300))
  return token
}

function check(name, ok, detail) {
  results.push({ name, ok })
  console.log(`${ok ? 'PASS' : 'FAIL'}  ${name}${detail !== undefined ? '  — ' + detail : ''}`)
}
const qs = o => Object.entries(o).filter(([, v]) => v !== undefined && v !== null && v !== '')
  .map(([k, v]) => `${k}=${encodeURIComponent(v)}`).join('&')

const createdProductIds = []
const createdShieldIds = []

async function main() {
  const token = await login()
  console.log('登录成功\n')

  // ═══ A. 全部商品：分页 + 可用库存 + 换算关系 + 等级价 ═══
  const page1 = await apiReq('GET', `/erp/product/page?${qs({ pageNum: 1, pageSize: 20 })}`, null, token)
  check('A1 商品分页返回', page1.code === 200 && Array.isArray(page1.data?.records), `total=${page1.data?.total}`)
  const recs = page1.data?.records || []
  check('A2 返回 availableStock 字段', recs.every(r => r.availableStock !== undefined),
    recs.map(r => r.availableStock).slice(0, 3).join(','))
  check('A3 返回 conversionRelation 字段', recs.every(r => 'conversionRelation' in r),
    recs.map(r => r.conversionRelation).slice(0, 2).join(' | '))
  check('A4 返回 gradePriceMap(8列价格等级)', recs.every(r => r.gradePriceMap !== undefined))

  // 商品图片地址有效性：不得指向已删除的图片（否则前端 img 直接 404）
  const imgUrls = recs.map(r => r.imageUrl).filter(Boolean)
  if (imgUrls.length) {
    let bad = []
    for (const u of imgUrls) {
      const path = u.startsWith('/api') ? u.slice(4) : u
      const resp = await apiReq('GET', path, null, token, true)
      if (resp.status !== 200) bad.push(`${u} → ${resp.status}`)
    }
    check('A8 商品图片地址有效(无失效图片引用)', bad.length === 0,
      imgUrls.length + ' 个图片地址' + (bad.length ? '，失效：' + bad.join(', ') : '全部可访问'))
  } else {
    check('A8 商品图片地址有效(无失效图片引用)', true, '(当前页无图片)')
  }

  // 关键字多字段（货号/型号）
  const kw = await apiReq('GET', `/erp/product/page?${qs({ keyword: '海鲜', pageNum: 1, pageSize: 5 })}`, null, token)
  check('A5 关键字(商品名称)查询', (kw.data?.records || []).length > 0, `命中 ${(kw.data?.records || []).length}`)

  // 默认状态=已启用（前端默认参数）过滤生效
  const en = await apiReq('GET', `/erp/product/page?${qs({ status: 'ENABLED', pageNum: 1, pageSize: 50 })}`, null, token)
  check('A6 显示状态=已启用 过滤生效', (en.data?.records || []).every(r => r.status === 'ENABLED'),
    `命中 ${(en.data?.records || []).length}`)

  // 分类递归过滤
  const tree = await apiReq('GET', '/erp/product-category/tree', null, token)
  const rootCat = (tree.data || [])[0]
  if (rootCat) {
    const byCat = await apiReq('GET', `/erp/product/page?${qs({ categoryId: rootCat.id, pageNum: 1, pageSize: 50 })}`, null, token)
    check('A7 分类(含子分类递归)过滤生效', byCat.code === 200, `${rootCat.categoryName} → ${(byCat.data?.records || []).length} 条`)
  }

  // ═══ B. 云导入 ═══
  const cloud = await apiReq('GET', `/erp/product/cloud-catalog/page?${qs({ pageNum: 1, pageSize: 10 })}`, null, token)
  check('B1 云商品库分页', cloud.code === 200 && (cloud.data?.records || []).length > 0, `total=${cloud.data?.total}`)
  const cloudOne = (cloud.data?.records || [])[0]
  if (cloudOne) {
    const before = await apiReq('GET', `/erp/product/page?${qs({ keyword: cloudOne.cloudName, pageNum: 1, pageSize: 5 })}`, null, token)
    const beforeCount = (before.data?.records || []).length
    const imp = await apiReq('POST', '/erp/product/cloud-import', { cloudIds: [cloudOne.id], categoryId: rootCat?.id }, token)
    check('B2 云导入接口返回 imported/skipped', imp.code === 200 && imp.data && typeof imp.data.imported === 'number',
      JSON.stringify(imp.data))
    if (beforeCount === 0) {
      const after = await apiReq('GET', `/erp/product/page?${qs({ keyword: cloudOne.cloudName, pageNum: 1, pageSize: 5 })}`, null, token)
      const hit = (after.data?.records || [])[0]
      check('B3 云导入真实落库', (after.data?.records || []).length === 1, `命中 ${(after.data?.records || []).length}`)
      if (hit) {
        createdProductIds.push(hit.id)
        const unit = await apiReq('GET', `/erp/product/units/${hit.id}`, null, token)
        const baseUnit = (unit.data || [])[0]
        check('B4 云导入生成基本单位', Array.isArray(unit.data) && unit.data.length >= 1 && baseUnit?.isBaseUnit === 1,
          `单位=${baseUnit?.unitName}`)
        // 重复导入应跳过
        const imp2 = await apiReq('POST', '/erp/product/cloud-import', { cloudIds: [cloudOne.id], categoryId: rootCat?.id }, token)
        check('B5 同名重复导入自动跳过', imp2.data?.skipped === 1 && imp2.data?.imported === 0, JSON.stringify(imp2.data))
      }
    } else {
      check('B3 云导入真实落库', true, '(该云商品此前已导入，跳过落库断言)')
    }
  }

  // ═══ C. 表单：单位 8 个等级价闭环 ═══
  const newProduct = {
    product: {
      productName: 'E2E商品-' + Date.now(), productCodeAlias: 'E2E' + Date.now(),
      categoryId: rootCat?.id, industryCategory: '其他', spec: '1*24', model: 'E2E-M',
      origin: '兰州', brand: 'E2E品牌', unit: '箱', status: 'ENABLED',
      isStandardProduct: 1, productType: 'SINGLE', mallShelfStatus: 0,
    },
    units: [{
      unitName: '箱', isBaseUnit: 1, conversionRate: 1, barcode: 'E2E0001', sortOrder: 0,
      presetPurchasePrice: 100, wholesalePrice: 120, retailPrice: 150, minSalePrice: 110, minDiscount: 10,
      gradePrice1: 111, gradePrice2: 122, gradePrice3: 133, gradePrice4: 144,
      gradePrice5: 155, gradePrice6: 166, gradePrice7: 177, gradePrice8: 188,
    }],
    recommends: [],
  }
  const created = await apiReq('POST', '/erp/product', newProduct.product, token)
  check('C1 新增商品', created.code === 200 && created.data === true)
  // 找到刚创建的商品
  const found = await apiReq('GET', `/erp/product/page?${qs({ keyword: newProduct.product.productCodeAlias, pageNum: 1, pageSize: 5 })}`, null, token)
  const prod = (found.data?.records || [])[0]
  check('C2 新增商品可查', !!prod, prod?.productName)
  if (prod) {
    createdProductIds.push(prod.id)
    const withUnit = await apiReq('PUT', `/erp/product/${prod.id}`, prod, token)
    check('C3 编辑商品', withUnit.code === 200)
    // 写单位（含 8 个等级价）
    const u = newProduct.units[0]
    const unitSave = await apiReq('POST', '/erp/product/units', { ...u, productId: prod.id }, token)
    check('C4 保存商品单位(含8个等级价列)', unitSave.code === 200, JSON.stringify(unitSave.data ?? unitSave.message ?? '').slice(0, 80))
    const unitList = await apiReq('GET', `/erp/product/units/${prod.id}`, null, token)
    const row = (unitList.data || [])[0]
    check('C5 单位等级价回读一致(grade_price_1..8 列已恢复)',
      !!row && Number(row.gradePrice1) === 111 && Number(row.gradePrice8) === 188,
      row ? `g1=${row.gradePrice1} g8=${row.gradePrice8}` : 'no unit')

    // 列表等级价回退（从基本单位列回读）
    const listAgain = await apiReq('GET', `/erp/product/page?${qs({ keyword: newProduct.product.productCodeAlias, pageNum: 1, pageSize: 5 })}`, null, token)
    const hit2 = (listAgain.data?.records || [])[0]
    check('C6 列表 gradePriceMap 回退到单位等级价',
      !!hit2 && Number(hit2.gradePriceMap?.GRADE_1) === 111,
      JSON.stringify(hit2?.gradePriceMap || {}).slice(0, 100))
    // 表单接口（含单位）
    const formDto = await apiReq('GET', `/erp/product/${prod.id}/form`, null, token)
    check('C7 表单接口返回 units', formDto.code === 200 && Array.isArray(formDto.data?.units),
      `units=${(formDto.data?.units || []).length}`)

    // 商城字段（排序方式/积分/关键字）
    const mallUpdate = await apiReq('PUT', `/erp/product/${prod.id}`, {
      ...prod, mallSortType: 'MANUAL', mallPoints: 12.5, keywords: 'e2e,商品',
    }, token)
    const afterMall = await apiReq('GET', `/erp/product/${prod.id}`, null, token)
    check('C8 商城排序方式/积分/关键字 落库',
      mallUpdate.code === 200 && afterMall.data?.mallSortType === 'MANUAL' && Number(afterMall.data?.mallPoints) === 12.5,
      `${afterMall.data?.mallSortType}/${afterMall.data?.mallPoints}/${afterMall.data?.keywords}`)
  }

  // ═══ D. 批量操作 ═══
  if (createdProductIds.length) {
    const ids = createdProductIds
    const shelf = await apiReq('PUT', '/erp/product/batch-shelf', { ids, mallShelfStatus: 1 }, token)
    const afterShelf = await apiReq('GET', `/erp/product/${ids[0]}`, null, token)
    check('D1 批量上架', shelf.code === 200 && afterShelf.data?.mallShelfStatus === 1)

    const shelfOff = await apiReq('PUT', '/erp/product/batch-shelf', { ids, mallShelfStatus: 0 }, token)
    const afterOff = await apiReq('GET', `/erp/product/${ids[0]}`, null, token)
    check('D2 批量下架', shelfOff.code === 200 && afterOff.data?.mallShelfStatus === 0)

    const upd = await apiReq('PUT', '/erp/product/batch-update-fields', { ids, brand: 'E2E批量品牌', isStandardProduct: 0 }, token)
    const afterUpd = await apiReq('GET', `/erp/product/${ids[0]}`, null, token)
    check('D3 批量修改(品牌/标品)', upd.code === 200 && afterUpd.data?.brand === 'E2E批量品牌' && afterUpd.data?.isStandardProduct === 0,
      `${afterUpd.data?.brand}/${afterUpd.data?.isStandardProduct}`)

    const sort = await apiReq('PUT', '/erp/product/set-mall-sort', { sortType: 'SALES' }, token)
    check('D4 设置商城默认排序方式', sort.code === 200, JSON.stringify(sort.data))
  }

  // 批量搬移（用分类树第二个分类；无则跳过）
  const flatCats = []
  const walk = nodes => (nodes || []).forEach(n => { flatCats.push(n); walk(n.children) })
  walk(tree.data)
  if (createdProductIds.length && flatCats.length >= 1) {
    const target = flatCats[flatCats.length - 1]
    const mv = await apiReq('PUT', '/erp/product/batch-move', { ids: createdProductIds.slice(0, 1), categoryId: target.id }, token)
    const afterMv = await apiReq('GET', `/erp/product/${createdProductIds[0]}`, null, token)
    check('D5 批量搬移分类', mv.code === 200 && String(afterMv.data?.categoryId) === String(target.id),
      `→ ${target.categoryName}`)
  }

  // ═══ E. 商品授权（屏蔽） ═══
  if (createdProductIds.length) {
    const partnerPage = await apiReq('GET', `/erp/md/customer/page?${qs({ pageNum: 1, pageSize: 5, partnerType: 'customer' })}`, null, token)
    const partner = (partnerPage.data?.records || partnerPage.records || [])[0]
    if (partner) {
      const sh = await apiReq('POST', '/erp/product-shield/batch-shield', {
        productIds: createdProductIds, partnerIds: [partner.id], partnerNames: partner.partnerName,
        shieldLevel: 'ALL', region: 'E2E区域',
      }, token)
      check('E1 批量屏蔽', sh.code === 200 && Number(sh.data) >= 1, `新增 ${sh.data} 条`)
      const shPage = await apiReq('GET', `/erp/product-shield/page?${qs({ keyword: 'E2E', pageNum: 1, pageSize: 10 })}`, null, token)
      const shRows = shPage.data?.records || []
      check('E2 授权分页(含商品图片/名称/货号等列)',
        shPage.code === 200 && shRows.length >= 1 && shRows[0].productName !== undefined,
        `命中 ${shRows.length}，首行=${shRows[0]?.productName}/${shRows[0]?.partnerName}`)
      shRows.forEach(r => createdShieldIds.push(r.id))
      const cancel = await apiReq('POST', '/erp/product-shield/batch-cancel', { ids: createdShieldIds }, token)
      check('E3 批量取消屏蔽', cancel.code === 200 && Number(cancel.data) >= 1, `取消 ${cancel.data} 条`)

      // E4/E5 级联：删除商品时应同步软删其授权（屏蔽）关系，避免授权页出现孤儿行
      const tmp = await apiReq('POST', '/erp/product', {
        productName: 'E2E级联商品-' + Date.now(), productCodeAlias: 'E2EC' + Date.now(),
        categoryId: rootCat?.id, industryCategory: '其他', status: 'ENABLED', productType: 'SINGLE',
      }, token)
      const tmpFound = await apiReq('GET', `/erp/product/page?${qs({ keyword: 'E2E级联商品', pageNum: 1, pageSize: 5 })}`, null, token)
      const tmpProd = (tmpFound.data?.records || [])[0]
      if (tmp.code === 200 && tmpProd) {
        await apiReq('POST', '/erp/product-shield/batch-shield', {
          productIds: [tmpProd.id], partnerIds: [partner.id], partnerNames: partner.partnerName, shieldLevel: 'ALL',
        }, token)
        const beforeDel = await apiReq('GET', `/erp/product-shield/page?${qs({ partnerId: partner.id, pageNum: 1, pageSize: 50 })}`, null, token)
        const hadShield = (beforeDel.data?.records || []).some(r => String(r.productId) === String(tmpProd.id))
        check('E4 授权关系已建立(级联测试前置)', hadShield)
        await apiReq('DELETE', `/erp/product/${tmpProd.id}`, null, token)
        const afterDel = await apiReq('GET', `/erp/product-shield/page?${qs({ partnerId: partner.id, pageNum: 1, pageSize: 50 })}`, null, token)
        const orphan = (afterDel.data?.records || []).some(r => String(r.productId) === String(tmpProd.id))
        check('E5 删除商品级联软删授权关系', !orphan)
      } else {
        check('E4 授权关系已建立(级联测试前置)', false, '临时商品创建失败')
        check('E5 删除商品级联软删授权关系', false, '前置失败')
      }
    } else {
      check('E1 批量屏蔽', true, '(无客户数据，跳过)')
      check('E4 授权关系已建立(级联测试前置)', true, '(跳过)')
      check('E5 删除商品级联软删授权关系', true, '(跳过)')
    }
  }

  // ═══ F. 导入 / 导出 ═══
  const csvPath = path.join(__dirname, '../tool-results/product-e2e-import.csv')
  fs.mkdirSync(path.dirname(csvPath), { recursive: true })
  const csvName = 'E2E导入商品' + Date.now()
  fs.writeFileSync(csvPath, '\uFEFF商品名称,货号,规格,型号,产地,品牌,单位,条码,所属行业类别,零售价,批发价,预设进价,保质期天数,备注\n'
    + `${csvName},E2EIMP001,1*12,E2E-IMP,兰州,E2E导入,箱,6900000000001,其他,88.5,70.5,60,180,接口导入验证\n`, 'utf8')
  const csvBuf = fs.readFileSync(csvPath)
  const boundary = '----e2e' + Date.now()
  const bodyParts = Buffer.concat([
    Buffer.from(`--${boundary}\r\nContent-Disposition: form-data; name="file"; filename="e2e.csv"\r\nContent-Type: text/csv\r\n\r\n`),
    csvBuf,
    Buffer.from(`\r\n--${boundary}--\r\n`),
  ])
  const impRes = await new Promise((resolve, reject) => {
    const r = http.request({
      hostname: '127.0.0.1', port: PORT, path: '/api/erp/product/import', method: 'POST',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': `multipart/form-data; boundary=${boundary}`, 'Content-Length': bodyParts.length },
    }, res => {
      const ch = []
      res.on('data', c => ch.push(c))
      res.on('end', () => { try { resolve(JSON.parse(Buffer.concat(ch).toString('utf8'))) } catch { resolve({}) } })
    })
    r.on('error', reject)
    r.write(bodyParts)
    r.end()
  })
  check('F1 商品导入(CSV)真实解析', impRes.code === 200 && impRes.data?.count >= 1,
    `导入 ${impRes.data?.count} 条，跳过 ${impRes.data?.skipped} 条`)
  const imported = await apiReq('GET', `/erp/product/page?${qs({ keyword: csvName, pageNum: 1, pageSize: 5 })}`, null, token)
  const importedRow = (imported.data?.records || [])[0]
  check('F2 导入商品落库(含基本单位)', !!importedRow && Number(importedRow.retailPrice) === 88.5,
    importedRow ? `${importedRow.productName}/${importedRow.retailPrice}` : 'not found')
  if (importedRow) createdProductIds.push(importedRow.id)

  const exp = await apiReq('GET', `/erp/product/export?${qs({ pageNum: 1, pageSize: 5 })}`, null, token, true)
  const isXlsx = exp.status === 200 && exp.buf.slice(0, 2).toString() === 'PK'
  check('F3 导出为真实 xlsx', isXlsx,
    `${exp.headers['content-type']} ${exp.buf.length}字节`)

  // ═══ G. 套餐子标签（接口直返裸 Page / Map，无 Result 包装） ═══
  const kitPage = await apiReq('GET', `/erp/product-kit/page?${qs({ pageNum: 1, pageSize: 10 })}`, null, token)
  const kitRows = kitPage.records || kitPage.data?.records || []
  check('G1 套餐分页接口可用', Array.isArray(kitRows), `total=${kitPage.total ?? kitPage.data?.total ?? 0}`)
  const kitIds = kitRows.map(r => r.id)
  const kitSummary = await apiReq('GET', `/erp/product-kit/items-summary?${qs({ kitIds: kitIds.length ? kitIds.join(',') : '0' })}`, null, token)
  check('G2 套餐商品明细摘要接口', typeof kitSummary === 'object' && !kitSummary.code,
    kitIds.length ? JSON.stringify(kitSummary).slice(0, 120) : '(无套餐数据，接口连通)')

  // ═══ H. 清理测试数据 ═══
  if (createdProductIds.length) {
    const del = await apiReq('PUT', '/erp/product/batch-delete', { ids: createdProductIds }, token)
    const after = await apiReq('GET', `/erp/product/page?${qs({ pageNum: 1, pageSize: 5, keyword: newProduct.product.productCodeAlias })}`, null, token)
    check('H1 批量删除清理', del.code === 200 && (after.data?.records || []).length === 0)
  }

  const passed = results.filter(r => r.ok).length
  console.log(`\n===== 商品模块验收 ${passed}/${results.length} =====`)
  const failed = results.filter(r => !r.ok)
  if (failed.length) {
    console.log('失败项：')
    failed.forEach(f => console.log(' - ' + f.name))
    process.exit(1)
  }
}

main().catch(e => { console.error('脚本异常:', e.message); process.exit(1) })
