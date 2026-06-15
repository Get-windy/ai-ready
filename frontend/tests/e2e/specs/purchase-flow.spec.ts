import { test, expect } from '../fixtures/auth.fixture'
import { setupAuthApiMocks, setAuthToken } from '../fixtures/auth.fixture'

test.describe('采购流程', () => {
  test.beforeEach(async ({ mockedPage }) => {
    await setupAuthApiMocks(mockedPage, 'admin')
    await mockedPage.goto('/login')
    await mockedPage.waitForLoadState('domcontentloaded')
    await setAuthToken(mockedPage, 'admin')
    await mockedPage.goto('/erp/purchase')
    await mockedPage.waitForLoadState('networkidle')
  })

  test('采购订单列表页展示', async ({ mockedPage }) => {
    // 页面标题
    await expect(mockedPage.locator('h1, h2').first()).toBeVisible()
    // vxe-table（使用 vxe-table 组件）
    await expect(mockedPage.locator('.vxe-table').first()).toBeVisible()
  })

  test('搜索采购订单', async ({ mockedPage }) => {
    const searchInput = mockedPage.locator('input[placeholder*="搜索"], input[placeholder*="订单"], input[placeholder*="编号"]').first()
    if (await searchInput.isVisible()) {
      await searchInput.fill('PO-2024')
      await mockedPage.locator('button').filter({ hasText: /查询|搜索/ }).first().click()
      await mockedPage.waitForTimeout(500)
    }
  })

  test('采购订单详情页展示', async ({ mockedPage }) => {
    await mockedPage.goto('/purchase/order/1')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('text=采购订单详情').or(mockedPage.locator('[class*=detail]')).first()).toBeVisible()
  })

  test('采购询价-报价-比价流程页面导航', async ({ mockedPage }) => {
    // 导航到询价页面
    await mockedPage.goto('/erp/purchase/inquiry')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('button').filter({ hasText: /新增|新建|创建/ }).first()).toBeVisible()

    // 验证页面结构（vxe-table）
    await expect(mockedPage.locator('.vxe-table').first()).toBeVisible()
  })

  test('采购换货模块', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/purchase-exchange')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('button').filter({ hasText: /新增|换货/ })).toBeVisible()
  })
})
