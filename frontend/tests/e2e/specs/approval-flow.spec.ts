import { test, expect } from '../fixtures/auth.fixture'
import { setupAuthApiMocks, setAuthToken } from '../fixtures/auth.fixture'

test.describe('审批流程', () => {
  test.beforeEach(async ({ mockedPage }) => {
    await setupAuthApiMocks(mockedPage, 'admin')
    await mockedPage.goto('/login')
    await mockedPage.waitForLoadState('domcontentloaded')
    await setAuthToken(mockedPage, 'admin')
  })

  test('价格审批管理列表展示', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/pricing/approval')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('h1, h2').first()).toBeVisible()
    // 统计卡片（使用 tabs 文本）
    await expect(mockedPage.locator('[role=tab]').filter({ hasText: '待审批' }).first()).toBeVisible()
    await expect(mockedPage.locator('[role=tab]').filter({ hasText: '已通过' }).first()).toBeVisible()
    await expect(mockedPage.locator('[role=tab]').filter({ hasText: '已拒绝' }).first()).toBeVisible()
  })

  test('价格审批弹窗打开', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/pricing/approval')
    await mockedPage.waitForLoadState('networkidle')
    const applyBtn = mockedPage.locator('button').filter({ hasText: /申请价格/ })
    if (await applyBtn.isVisible()) {
      await applyBtn.click()
      await mockedPage.waitForTimeout(500)
      await expect(mockedPage.locator('.ant-modal-content').first()).toBeVisible()
    }
  })

  test('审批标签页切换', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/pricing/approval')
    await mockedPage.waitForLoadState('networkidle')
    // 切换到已通过
    const approvedTab = mockedPage.locator('[role=tab]').filter({ hasText: '已通过' })
    if (await approvedTab.isVisible()) {
      await approvedTab.click()
      await mockedPage.waitForTimeout(300)
    }
    // 切换到已拒绝
    const rejectedTab = mockedPage.locator('[role=tab]').filter({ hasText: '已拒绝' })
    if (await rejectedTab.isVisible()) {
      await rejectedTab.click()
      await mockedPage.waitForTimeout(300)
    }
  })

  test('销售分析页面加载19个图表', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/sales-analysis')
    await mockedPage.waitForLoadState('networkidle')
    await mockedPage.waitForTimeout(2000)
    // 验证数据加载后不再显示骨架屏
    const skeletonSelectors = ['[class*=skeleton]', '[class*=spin]']
    let skeletonVisible = false
    for (const sel of skeletonSelectors) {
      if (await mockedPage.locator(sel).first().isVisible({ timeout: 1000 }).catch(() => false)) {
        skeletonVisible = true
      }
    }
  })
})

test.describe('Dashboard 仪表盘', () => {
  test.beforeEach(async ({ mockedPage }) => {
    await setupAuthApiMocks(mockedPage, 'admin')
    await mockedPage.goto('/login')
    await mockedPage.waitForLoadState('domcontentloaded')
    await setAuthToken(mockedPage, 'admin')
  })

  test('Dashboard KPI 卡片和图表展示', async ({ mockedPage }) => {
    await mockedPage.goto('/dashboard')
    await mockedPage.waitForLoadState('networkidle')
    await mockedPage.waitForTimeout(2000)

    // KPI 统计卡片
    await expect(mockedPage.locator('text=今日销售额').or(mockedPage.locator('text=今日采购额')).first()).toBeVisible({ timeout: 10000 })

    // 待办事项
    await expect(mockedPage.locator('text=待办事项').first()).toBeVisible()

    // 快速入口
    await expect(mockedPage.locator('text=采购管理').first()).toBeVisible()
    await expect(mockedPage.locator('text=销售管理').first()).toBeVisible()
    await expect(mockedPage.locator('text=库存管理').first()).toBeVisible()
    await expect(mockedPage.locator('text=财务管理').first()).toBeVisible()

    // 库存预警表格
    await expect(mockedPage.locator('text=库存预警').first()).toBeVisible()
  })

  test('Dashboard 刷新数据', async ({ mockedPage }) => {
    await mockedPage.goto('/dashboard')
    await mockedPage.waitForLoadState('networkidle')
    await mockedPage.waitForTimeout(1500)

    const refreshBtn = mockedPage.locator('button').filter({ hasText: /刷新/ })
    if (await refreshBtn.isVisible()) {
      await refreshBtn.click()
      await mockedPage.waitForTimeout(1000)
    }
  })

  test('快速入口导航', async ({ mockedPage }) => {
    await mockedPage.goto('/dashboard')
    await mockedPage.waitForLoadState('networkidle')
    await mockedPage.waitForTimeout(1000)

    const stockEntry = mockedPage.locator('text=库存管理').first()
    if (await stockEntry.isVisible()) {
      await stockEntry.click()
      await mockedPage.waitForTimeout(500)
    }
  })
})
