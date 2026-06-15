import { test, expect } from '../fixtures/auth.fixture'
import { setupAuthApiMocks, setAuthToken } from '../fixtures/auth.fixture'

test.describe('库存管理流程', () => {
  test.beforeEach(async ({ mockedPage }) => {
    await setupAuthApiMocks(mockedPage, 'admin')
    await mockedPage.goto('/login')
    await mockedPage.waitForLoadState('domcontentloaded')
    await setAuthToken(mockedPage, 'admin')
    await mockedPage.goto('/erp/stock')
    await mockedPage.waitForLoadState('networkidle')
  })

  test('库存列表页展示', async ({ mockedPage }) => {
    await expect(mockedPage.locator('h1, h2').first()).toBeVisible()
    await expect(mockedPage.locator('.vxe-table').first()).toBeVisible()
  })

  test('搜索库存', async ({ mockedPage }) => {
    const searchInput = mockedPage.locator('input[placeholder*="商品"]').first()
    if (await searchInput.isVisible()) {
      await searchInput.fill('P001')
      await mockedPage.locator('.ant-btn-primary').first().click()
      await mockedPage.waitForTimeout(500)
    }
  })

  test('入库管理页面展示', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/stock-in')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('button').filter({ hasText: /入库|新增/ }).first()).toBeVisible()
  })

  test('库存盘点页面展示', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/stocktake')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('button').filter({ hasText: /盘点|新增/ }).first()).toBeVisible()
  })

  test('库存预警展示', async ({ mockedPage }) => {
    await mockedPage.goto('/dashboard')
    await mockedPage.waitForLoadState('networkidle')
    const alertSection = mockedPage.locator('text=库存预警').first()
    await expect(alertSection).toBeVisible({ timeout: 10000 })
  })

  test('智能补货页面展示', async ({ mockedPage }) => {
    await mockedPage.goto('/erp/stock/replenishment')
    await mockedPage.waitForLoadState('networkidle')
    await expect(mockedPage.locator('button').filter({ hasText: /生成|补货/ }).first()).toBeVisible()
  })
})
