import { test as base, type Page, type BrowserContext } from '@playwright/test';

/**
 * 模拟用户凭据
 */
const ADMIN_CREDENTIALS = {
  username: 'admin',
  password: 'Admin@123',
  token: 'mock-admin-token-abc123',
  userId: 1,
};

const USER_CREDENTIALS = {
  username: 'user',
  password: 'User@123',
  token: 'mock-user-token-xyz789',
  userId: 2,
};

/**
 * 标准 API 响应格式
 */
function apiResponse<T>(data: T, code = 200, message = 'success') {
  return {
    status: 200,
    contentType: 'application/json',
    body: JSON.stringify({ code, message, data, timestamp: Date.now() }),
  };
}

/**
 * 模拟菜单数据
 */
function buildMenuData() {
  return [
    {
      id: 1,
      parentId: 0,
      menuName: '工作台',
      menuCode: 'dashboard',
      menuType: 1,
      path: '/dashboard',
      component: 'dashboard/index',
      routeName: 'Dashboard',
      icon: 'DashboardOutlined',
      sort: 1,
      isExternal: 0,
      isCache: 1,
      visible: 1,
      status: 1,
    },
    {
      id: 2,
      parentId: 0,
      menuName: '系统管理',
      menuCode: 'system',
      menuType: 0,
      path: '/system',
      icon: 'SettingOutlined',
      sort: 10,
      isExternal: 0,
      isCache: 0,
      visible: 1,
      status: 1,
      children: [
        {
          id: 21,
          parentId: 2,
          menuName: '用户管理',
          menuCode: 'system:user',
          menuType: 1,
          path: '/system/user',
          component: 'system/user/index',
          routeName: 'SystemUser',
          icon: 'TeamOutlined',
          sort: 1,
          isExternal: 0,
          isCache: 0,
          visible: 1,
          status: 1,
        },
        {
          id: 22,
          parentId: 2,
          menuName: '角色管理',
          menuCode: 'system:role',
          menuType: 1,
          path: '/system/role',
          component: 'system/role/index',
          routeName: 'SystemRole',
          icon: 'SafetyOutlined',
          sort: 2,
          isExternal: 0,
          isCache: 0,
          visible: 1,
          status: 1,
        },
        {
          id: 23,
          parentId: 2,
          menuName: '菜单管理',
          menuCode: 'system:menu',
          menuType: 1,
          path: '/system/menu',
          component: 'system/menu/index',
          routeName: 'SystemMenu',
          icon: 'UnorderedListOutlined',
          sort: 3,
          isExternal: 0,
          isCache: 0,
          visible: 1,
          status: 1,
        },
      ],
    },
    {
      id: 3,
      parentId: 0,
      menuName: '采购管理',
      menuCode: 'purchase',
      menuType: 1,
      path: '/purchase',
      component: 'purchase/index',
      routeName: 'Purchase',
      icon: 'ShoppingCartOutlined',
      sort: 20,
      isExternal: 0,
      isCache: 1,
      visible: 1,
      status: 1,
    },
    {
      id: 4,
      parentId: 0,
      menuName: '销售管理',
      menuCode: 'sale',
      menuType: 1,
      path: '/sale',
      component: 'sale/index',
      routeName: 'Sale',
      icon: 'ShoppingOutlined',
      sort: 30,
      isExternal: 0,
      isCache: 1,
      visible: 1,
      status: 1,
    },
  ];
}

/**
 * 模拟用户列表数据
 */
function buildUserListData(pageNum = 1, pageSize = 10, filter?: { username?: string }) {
  const allUsers = [
    { id: 1, username: 'admin', nickname: '管理员', email: 'admin@example.com', phone: '13800138000', gender: 1, userType: 0, status: 0, createTime: '2024-01-01 10:00:00' },
    { id: 2, username: 'user1', nickname: '张三', email: 'zhangsan@example.com', phone: '13800138001', gender: 1, userType: 2, status: 0, createTime: '2024-01-02 10:00:00' },
    { id: 3, username: 'user2', nickname: '李四', email: 'lisi@example.com', phone: '13800138002', gender: 2, userType: 2, status: 0, createTime: '2024-01-03 10:00:00' },
    { id: 4, username: 'user3', nickname: '王五', email: 'wangwu@example.com', phone: '13800138003', gender: 1, userType: 1, status: 1, createTime: '2024-01-04 10:00:00' },
    { id: 5, username: 'user4', nickname: '赵六', email: 'zhaoliu@example.com', phone: '13800138004', gender: 2, userType: 2, status: 0, createTime: '2024-01-05 10:00:00' },
    { id: 6, username: 'user5', nickname: '孙七', email: 'sunqi@example.com', phone: '13800138005', gender: 1, userType: 2, status: 0, createTime: '2024-01-06 10:00:00' },
    { id: 7, username: 'user6', nickname: '周八', email: 'zhouba@example.com', phone: '13800138006', gender: 2, userType: 2, status: 1, createTime: '2024-01-07 10:00:00' },
    { id: 8, username: 'user7', nickname: '吴九', email: 'wujiu@example.com', phone: '13800138007', gender: 1, userType: 2, status: 0, createTime: '2024-01-08 10:00:00' },
    { id: 9, username: 'user8', nickname: '郑十', email: 'zhengshi@example.com', phone: '13800138008', gender: 2, userType: 1, status: 0, createTime: '2024-01-09 10:00:00' },
    { id: 10, username: 'user9', nickname: '钱十一', email: 'qianshiyi@example.com', phone: '13800138009', gender: 1, userType: 2, status: 0, createTime: '2024-01-10 10:00:00' },
    { id: 11, username: 'user10', nickname: '冯十二', email: 'fengshier@example.com', phone: '13800138010', gender: 2, userType: 2, status: 0, createTime: '2024-01-11 10:00:00' },
    { id: 12, username: 'user11', nickname: '陈十三', email: 'chenshisan@example.com', phone: '13800138011', gender: 1, userType: 2, status: 0, createTime: '2024-01-12 10:00:00' },
  ];

  let filtered = allUsers;
  if (filter?.username) {
    filtered = allUsers.filter((u) => u.username.includes(filter.username!));
  }

  const start = (pageNum - 1) * pageSize;
  const records = filtered.slice(start, start + pageSize);

  return {
    records,
    total: filtered.length,
    current: pageNum,
    size: pageSize,
    pages: Math.ceil(filtered.length / pageSize),
  };
}

/**
 * 为认证相关 API 设置 Mock 响应
 * 包括登录、登出、用户信息、菜单等
 */
export async function setupAuthApiMocks(page: Page, userType: 'admin' | 'user' = 'admin'): Promise<void> {
  const creds = userType === 'admin' ? ADMIN_CREDENTIALS : USER_CREDENTIALS;
  const permissions = userType === 'admin'
    ? ['*']
    : ['dashboard:view', 'user:view'];
  const roles = userType === 'admin' ? ['admin'] : ['user'];
  const nickname = userType === 'admin' ? '管理员' : '普通用户';

  // Mock 登录接口
  await page.route('**/api/auth/login', (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      // 验证凭据
      if (body.username === creds.username && body.password === creds.password) {
        return route.fulfill(
          apiResponse({
            token: creds.token,
            tokenName: 'Authorization',
            userId: creds.userId,
          }),
        );
      }
      // 凭据错误 — 使用 code 500 (业务错误) 而非 401，避免触发 handleUnauthorized
      return route.fulfill(
        apiResponse(null, 500, '用户名或密码错误'),
      );
    }
    return route.continue();
  });

  // Mock 登出接口
  await page.route('**/api/auth/logout', (route) => {
    return route.fulfill(apiResponse(null));
  });

  // Mock 获取用户信息接口
  await page.route('**/api/auth/userinfo', (route) => {
    return route.fulfill(
      apiResponse({
        userId: creds.userId,
        username: creds.username,
        nickname,
        email: `${creds.username}@example.com`,
        phone: '13800138000',
        avatar: '',
        permissions,
        roles,
      }),
    );
  });

  // Mock 菜单接口（路由守卫和布局都会调用）
  await page.route('**/api/menu/user/client/pc-admin*', (route) => {
    return route.fulfill(apiResponse(buildMenuData()));
  });

  // Mock 用户列表分页接口
  await page.route('**/api/user/page*', (route) => {
    const url = new URL(route.request().url());
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1', 10);
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10', 10);
    const username = url.searchParams.get('username') || undefined;
    return route.fulfill(apiResponse(buildUserListData(pageNum, pageSize, { username })));
  });

  // Mock 批量删除用户（必须在 **/api/user/* 之前注册）
  await page.route('**/api/user/batch', (route) => {
    return route.fulfill(apiResponse(true));
  });

  // Mock 创建用户 (POST /api/user)
  await page.route('**/api/user', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // Mock 更新/删除单个用户 (PUT|DELETE /api/user/:id)
  // 注意：必须在 **/api/user/batch 之后注册，否则会拦截批量操作
  await page.route('**/api/user/*', (route) => {
    const method = route.request().method();
    if (method === 'PUT' || method === 'DELETE') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // Mock 角色列表
  await page.route('**/api/role/page*', (route) => {
    return route.fulfill(
      apiResponse({
        records: [
          { id: 1, roleName: '管理员', roleCode: 'admin' },
          { id: 2, roleName: '普通用户', roleCode: 'user' },
        ],
        total: 2,
        current: 1,
        size: 100,
        pages: 1,
      }),
    );
  });
}

/**
 * 通过 UI 登录
 * 导航到登录页面，填写表单并提交
 */
export async function loginViaUi(
  page: Page,
  username: string,
  password: string,
): Promise<void> {
  await page.goto('/login');
  // 等待登录表单渲染
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 });

  // 填写用户名
  await page.fill('input[placeholder="请输入用户名"]', username);

  // 填写密码
  await page.fill('input[placeholder="请输入密码"]', password);

  // 填写验证码（4位）
  await page.fill('input[placeholder="请输入验证码"]', 'ABCD');

  // 点击登录按钮
  await page.click('button[type="submit"]');

  // 等待跳转到 dashboard
  await page.waitForURL('**/dashboard**', { timeout: 15_000 });
}

/**
 * 在 localStorage 中直接设置 token（绕过登录 UI）
 */
export async function setAuthToken(page: Page, userType: 'admin' | 'user' = 'admin'): Promise<void> {
  const creds = userType === 'admin' ? ADMIN_CREDENTIALS : USER_CREDENTIALS;
  await page.evaluate((token) => {
    localStorage.setItem('token', token);
    localStorage.setItem('tenantId', '1');
  }, creds.token);
}

/**
 * 清除认证状态
 */
export async function clearAuthToken(page: Page): Promise<void> {
  await page.evaluate(() => {
    localStorage.removeItem('token');
    localStorage.removeItem('tenantId');
    localStorage.removeItem('user-store');
  });
}

// ============================
// Playwright 自定义 Fixture
// ============================

type AuthFixtures = {
  /** 已设置 API Mock + 已登录的管理员页面 */
  adminPage: Page;
  /** 已设置 API Mock + 已登录的普通用户页面 */
  userPage: Page;
  /** 已设置 API Mock 但未登录的页面 */
  mockedPage: Page;
};

/**
 * 扩展后的测试对象，内置 Auth Fixture
 */
export const test = base.extend<AuthFixtures>({
  mockedPage: async ({ page }, use) => {
    // 设置所有 API Mock，但不登录
    await setupAuthApiMocks(page, 'admin');
    await use(page);
  },

  adminPage: async ({ page }, use) => {
    // 设置 API Mock
    await setupAuthApiMocks(page, 'admin');
    // 设置 token 并导航到首页
    await setAuthToken(page, 'admin');
    await page.goto('/dashboard');
    // 等待布局渲染完成
    await page.waitForSelector('.basic-layout', { timeout: 15_000 });
    await use(page);
  },

  userPage: async ({ page }, use) => {
    // 设置 API Mock
    await setupAuthApiMocks(page, 'user');
    // 设置 token 并导航到首页
    await setAuthToken(page, 'user');
    await page.goto('/dashboard');
    // 等待布局渲染完成
    await page.waitForSelector('.basic-layout', { timeout: 15_000 });
    await use(page);
  },
});

export { expect } from '@playwright/test';
export { ADMIN_CREDENTIALS, USER_CREDENTIALS };
