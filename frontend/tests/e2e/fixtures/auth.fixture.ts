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
      menuName: '采购管理',
      menuCode: 'purchase',
      menuType: 1,
      path: '/purchase',
      component: 'purchase/index',
      routeName: 'Purchase',
      icon: 'ShoppingCartOutlined',
      sort: 10,
      isExternal: 0,
      isCache: 0,
      visible: 1,
      status: 1,
    },
    {
      id: 3,
      parentId: 0,
      menuName: '销售管理',
      menuCode: 'sale',
      menuType: 1,
      path: '/sale',
      component: 'sale/index',
      routeName: 'Sale',
      icon: 'ShoppingOutlined',
      sort: 20,
      isExternal: 0,
      isCache: 0,
      visible: 1,
      status: 1,
    },
    {
      id: 4,
      parentId: 0,
      menuName: '系统管理',
      menuCode: 'system',
      menuType: 0,
      path: '/system',
      icon: 'SettingOutlined',
      sort: 50,
      isExternal: 0,
      isCache: 0,
      visible: 1,
      status: 1,
      children: [
        {
          id: 41,
          parentId: 4,
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
          id: 42,
          parentId: 4,
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
          id: 43,
          parentId: 4,
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
  ];
}

/**
 * 模拟用户列表数据
 */
function buildUserListData(pageNum = 1, pageSize = 10, filter?: { username?: string }) {
  // 生成 25 条用户数据以保证分页测试（20 条/页时有 2 页）
  const allUsers = Array.from({ length: 25 }, (_, i) => {
    const id = i + 1;
    const username = id === 1 ? 'admin' : `user${id - 1}`;
    const nicknames = ['管理员','张三','李四','王五','赵六','孙七','周八','吴九','郑十','钱十一','冯十二','陈十三','刘十四','黄十五','杨十六','周十七','吴十八','徐十九','孙二十','马二一','朱二二','胡二三','郭二四','林二五'];
    const status = [0,0,0,1,0,0,1,0,0,0,0,0,0,1,0,0,1,0,0,0,0,0,0,0,0];
    const emails = ['admin@example.com','zhangsan@example.com','lisi@example.com','wangwu@example.com','zhaoliu@example.com','sunqi@example.com','zhouba@example.com','wujiu@example.com','zhengshi@example.com','qianshiyi@example.com','fengshier@example.com','chenshisan@example.com','liushisi@example.com','huangshiwu@example.com','yangshiliu@example.com','zhoushiqi@example.com','wushiba@example.com','xushijiu@example.com','sunnershi@example.com','maeryi@example.com','zhuerer@example.com','husaner@example.com','guoersi@example.com','linerwu@example.com'];
    const userTypes = [0,2,2,1,2,2,2,2,1,2,2,2,2,1,2,2,2,2,2,2,2,2,2,2,2];
    const phones = Array.from({ length: 25 }, (_, j) => `13800138${String(j).padStart(3, '0')}`);
    const month = String(Math.floor((id - 1) / 25 * 11) + 1).padStart(2, '0');
    const day = String(((id - 1) % 28) + 1).padStart(2, '0');
    return {
      id,
      username,
      nickname: nicknames[i],
      email: emails[i],
      phone: phones[i],
      gender: i % 2 + 1,
      userType: userTypes[i],
      status: status[i],
      createTime: `2024-${month}-${day} 10:00:00`,
    };
  });

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

  // Mock 验证码接口（测试环境固定返回 ABCD）
  await page.route('**/api/auth/captcha', (route) => {
    return route.fulfill(
      apiResponse({
        uuid: 'test-captcha-uuid-123',
        img: 'data:image/svg+xml;base64,<svg xmlns="http://www.w3.org/2000/svg" width="120" height="40"><rect width="100%" height="100%" fill="#f0f0f0"/><text x="60" y="25" font-family="Arial" font-size="20" text-anchor="middle">ABCD</text></svg>',
      }),
    );
  });

  // Mock 登录接口（接受任何验证码，后端不验证）
  await page.route('**/api/auth/login', (route) => {
    if (route.request().method() === 'POST') {
      const body = route.request().postDataJSON();
      // 验证凭据（忽略 tenantName，因为 mock 不验证租户）
      if (body.username === creds.username && body.password === creds.password) {
        return route.fulfill(
          apiResponse({
            token: creds.token,
            tokenName: 'Authorization',
            userId: creds.userId,
            tenantId: 1,
            tenantName: body.tenantName || '默认租户',
            tenants: [{ id: 1, tenantName: '默认租户', tenantCode: 'SYSTEM', status: 1 }],
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
        id: creds.userId,
        username: creds.username,
        nickname,
        email: `${creds.username}@example.com`,
        phone: '13800138000',
        avatar: '',
        userType: userType === 'admin' ? 0 : 2,
        tenantId: 1,
        permissions,
        roles,
        billTypes: [],
        passwordExpired: false,
      }),
    );
  });

  // Mock Token 验证接口（路由守卫会检查 token 有效性）
  await page.route('**/api/auth/check', (route) => {
    return route.fulfill(
      apiResponse({ valid: true, userId: creds.userId, tokenTimeout: 36000 }),
    );
  });

  // Mock Token 刷新接口（axios 401拦截器会尝试刷新）
  await page.route('**/api/auth/refresh', (route) => {
    return route.fulfill(
      apiResponse({ token: creds.token + '-refreshed', tokenName: 'Authorization' }),
    );
  });

  // Mock 租户有效模块接口（getUserInfo 中调用）
  await page.route('**/api/tenant-module/valid-codes*', (route) => {
    return route.fulfill(apiResponse(['sale', 'purchase', 'warehouse', 'finance', 'customer', 'system', 'dashboard']));
  });

  // Mock SSE 通知连接（登录后自动建立）
  await page.route('**/api/sse/notifications*', (route) => {
    // SSE 使用 EventStream 格式，但 mock 返回 JSON 会导致客户端报错
    // 这里返回空数据以阻止实际请求报 401，SSE 客户端会自动重连
    return route.fulfill({
      status: 200,
      contentType: 'text/event-stream',
      body: 'data: {"type":"connected"}\n\n',
    });
  });

  // Mock 通知相关接口（多个端点，阻止 401 触发登出）
  await page.route('**/api/notification/**', (route) => {
    return route.fulfill(apiResponse({ count: 0, records: [], total: 0 }));
  });

  // Mock 租户列表查询（登录后获取）
  await page.route('**/api/auth/tenants*', (route) => {
    return route.fulfill(apiResponse([{ id: 1, tenantName: '默认租户', tenantCode: 'SYSTEM', status: 1 }]));
  });

  // Mock 菜单接口（路由守卫和布局都会调用）
  await page.route('**/api/menu/user/client/pc-admin*', (route) => {
    return route.fulfill(apiResponse(buildMenuData()));
  });

  // ── 用户管理 API ──
  // 注意：Playwright 使用 LIFO 匹配，最后注册的 route 优先级最高。
  // 因此通配路由 **/api/user/*（最通用）必须最先注册，具体路由在后。
  //
  // 注册顺序（由低到高优先级）：
  //   1. **/api/user/*  (通用 PUT|DELETE 处理, GET continue → 被 page* 拦截)
  //   2. **/api/user    (POST 创建用户)
  //   3. **/api/user/batch (批量删除)
  //   4. **/api/user/page* (分页列表, 最高优先级)

  // 通用用户路由（最先注册，优先级最低）
  await page.route('**/api/user/*', (route) => {
    const method = route.request().method();
    if (method === 'PUT' || method === 'DELETE') {
      return route.fulfill(apiResponse(true));
    }
    // GET 请求会被更具体的 route（page*, batch）拦截，此处继续到服务端
    return route.continue();
  });

  // Mock 创建用户 (POST /api/user)
  await page.route('**/api/user', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // Mock 批量删除用户
  await page.route('**/api/user/batch', (route) => {
    return route.fulfill(apiResponse(true));
  });

  // Mock 用户列表分页接口（最高优先级，最后注册）
  await page.route('**/api/user/page*', (route) => {
    const url = new URL(route.request().url());
    const pageNum = parseInt(url.searchParams.get('pageNum') || '1', 10);
    const pageSize = parseInt(url.searchParams.get('pageSize') || '10', 10);
    const username = url.searchParams.get('username') || undefined;
    return route.fulfill(apiResponse(buildUserListData(pageNum, pageSize, { username })));
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

  // Mock 字典接口（用户管理页面使用 USER_TYPE 字典）
  await page.route('**/api/dict/**', (route) => {
    return route.fulfill(apiResponse([
      { code: '0', name: '系统用户' },
      { code: '1', name: '企业管理员' },
      { code: '2', name: '普通用户' },
    ]));
  });

  // ============================================================
  // Mock ERP 业务接口
  // ============================================================

  // Mock 仪表盘统计数据（精确路径优先）
  await page.route('**/api/dashboard/stats*', (route) => {
    return route.fulfill(apiResponse({
      todaySales: 125000,
      todayPurchase: 86000,
      pendingOrders: 12,
      lowStockItems: 3,
      monthlySales: 2850000,
      monthlyPurchase: 1920000,
      orderCount: 156,
      customerCount: 89,
    }));
  });
  await page.route('**/api/dashboard/trend*', (route) => {
    return route.fulfill(apiResponse({
      dates: ['2024-01', '2024-02', '2024-03', '2024-04', '2024-05', '2024-06'],
      sales: [120000, 135000, 148000, 152000, 168000, 175000],
      purchases: [80000, 95000, 102000, 110000, 118000, 125000],
    }));
  });
  await page.route('**/api/dashboard/todos*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, title: '审批采购订单 #PO-2024-001', status: 'pending', priority: 'high' },
        { id: 2, title: '确认入库单 #IN-2024-001', status: 'pending', priority: 'normal' },
        { id: 3, title: '审核价格审批 #P-2024-001', status: 'pending', priority: 'high' },
      ],
      total: 3,
    }));
  });
  await page.route('**/api/dashboard/alerts*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productName: '碳钢螺栓 M12', stock: 5, minStock: 20, unit: '个' },
        { id: 2, productName: '304不锈钢管', stock: 3, minStock: 15, unit: '根' },
      ],
      total: 2,
    }));
  });

  // Mock 价格审批接口
  await page.route('**/api/erp/pricing/approval/statistics*', (route) => {
    return route.fulfill(apiResponse({
      pendingCount: 5,
      approvedCount: 12,
      rejectedCount: 3,
      totalCount: 20,
    }));
  });
  await page.route('**/api/erp/pricing/approval/pending*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, applicantName: '张三', productName: '碳钢螺栓', applyAmount: 15000, status: 'pending', applyTime: '2024-06-01' },
        { id: 2, applicantName: '李四', productName: '不锈钢管', applyAmount: 28000, status: 'pending', applyTime: '2024-06-02' },
      ],
      total: 2,
    }));
  });
  await page.route('**/api/erp/pricing/approval/list/**', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, applicantName: '张三', productName: '碳钢螺栓', applyAmount: 15000, status: 'approved', applyTime: '2024-06-01' },
        { id: 2, applicantName: '李四', productName: '不锈钢管', applyAmount: 28000, status: 'rejected', applyTime: '2024-06-02' },
      ],
      total: 2,
    }));
  });
  await page.route('**/api/erp/pricing/approval/**', (route) => {
    return route.fulfill(apiResponse({
      id: 1, applicantName: '张三', productName: '碳钢螺栓', applyAmount: 15000, status: 'pending',
      applyTime: '2024-06-01', remark: '市场价上涨，需要调整',
    }));
  });

  // Mock 采购订单接口
  await page.route('**/api/erp/purchase/order/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, orderNo: 'PO-2024-001', supplierName: '供应商A', totalAmount: 150000, status: 'pending', createTime: '2024-06-01' },
        { id: 2, orderNo: 'PO-2024-002', supplierName: '供应商B', totalAmount: 86000, status: 'approved', createTime: '2024-06-02' },
        { id: 3, orderNo: 'PO-2024-003', supplierName: '供应商C', totalAmount: 220000, status: 'completed', createTime: '2024-06-03' },
      ],
      total: 3,
      current: 1,
      size: 10,
      pages: 1,
    }));
  });
  await page.route('**/api/erp/purchase/order/**/items*', (route) => {
    return route.fulfill(apiResponse([
      { id: 1, productName: '碳钢螺栓 M12', quantity: 100, unitPrice: 15, totalPrice: 1500 },
      { id: 2, productName: '304不锈钢管', quantity: 50, unitPrice: 280, totalPrice: 14000 },
    ]));
  });
  await page.route('**/api/erp/purchase/order/**', (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, orderNo: 'PO-2024-001', supplierName: '供应商A', totalAmount: 150000,
        status: 'pending', createTime: '2024-06-01', remark: '急单',
        items: [
          { id: 1, productName: '碳钢螺栓 M12', quantity: 100, unitPrice: 15 },
          { id: 2, productName: '304不锈钢管', quantity: 50, unitPrice: 280 },
        ],
      }));
    }
    return route.fulfill(apiResponse(true));
  });

  // Mock 采购入库接口
  await page.route('**/api/erp/purchase/inbound/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, inboundNo: 'IN-2024-001', orderNo: 'PO-2024-001', supplierName: '供应商A', status: 'pending', createTime: '2024-06-05' },
        { id: 2, inboundNo: 'IN-2024-002', orderNo: 'PO-2024-002', supplierName: '供应商B', status: 'completed', createTime: '2024-06-06' },
      ],
      total: 2,
      current: 1,
      size: 10,
      pages: 1,
    }));
  });
  await page.route('**/api/erp/purchase/inbound/**', (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, inboundNo: 'IN-2024-001', orderNo: 'PO-2024-001', supplierName: '供应商A', status: 'pending',
        items: [{ id: 1, productName: '碳钢螺栓 M12', expectedQty: 100, actualQty: 100 }],
      }));
    }
    return route.fulfill(apiResponse(true));
  });

  // Mock 采购询价接口
  await page.route('**/api/erp/purchase/inquiry/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, inquiryNo: 'RFQ-2024-001', title: '碳钢螺栓询价', status: 'pending', createTime: '2024-06-01' },
        { id: 2, inquiryNo: 'RFQ-2024-002', title: '不锈钢管询价', status: 'sent', createTime: '2024-06-02' },
      ],
      total: 2,
      current: 1,
      size: 10,
      pages: 1,
    }));
  });
  await page.route('**/api/erp/purchase/inquiry/**', (route) => {
    return route.fulfill(apiResponse({
      id: 1, inquiryNo: 'RFQ-2024-001', title: '碳钢螺栓询价', status: 'pending',
      items: [{ id: 1, productName: '碳钢螺栓 M12', quantity: 100 }],
    }));
  });

  // Mock 采购换货接口
  await page.route('**/api/erp/purchase/exchange/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, exchangeNo: 'EX-2024-001', orderNo: 'PO-2024-001', status: 'pending', createTime: '2024-06-10' },
        { id: 2, exchangeNo: 'EX-2024-002', orderNo: 'PO-2024-002', status: 'approved', createTime: '2024-06-11' },
      ],
      total: 2,
      current: 1,
      size: 10,
      pages: 1,
    }));
  });
  await page.route('**/api/erp/purchase/exchange/**', (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, exchangeNo: 'EX-2024-001', orderNo: 'PO-2024-001', status: 'pending',
        reason: '规格不符', items: [{ id: 1, productName: '碳钢螺栓 M12', quantity: 50 }],
      }));
    }
    return route.fulfill(apiResponse(true));
  });

  // Mock 库存接口
  await page.route('**/api/erp/stock/export*', (route) => {
    return route.fulfill({ status: 200, contentType: 'application/octet-stream', body: '' });
  });
  await page.route('**/api/erp/stock/**/logs*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productName: '碳钢螺栓 M12', changeType: '入库', quantity: 100, operator: '管理员', operateTime: '2024-06-01' },
      ],
      total: 1,
    }));
  });
  await page.route('**/api/erp/stock/**', (route) => {
    const method = route.request().method();
    if (method === 'GET' || method === 'PUT') {
      return route.fulfill(apiResponse({
        id: 1, productName: '碳钢螺栓 M12', sku: 'CS-BOLT-M12', category: '紧固件',
        stock: 500, minStock: 100, maxStock: 1000, unit: '个', warehouseName: '主仓库',
        price: 15.5, status: 'normal',
      }));
    }
    return route.fulfill(apiResponse(true));
  });

  // Mock 库存盘点接口
  await page.route('**/api/erp/stock/check/**/items*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productName: '碳钢螺栓 M12', expectedQty: 500, actualQty: 498, difference: -2 },
        { id: 2, productName: '304不锈钢管', expectedQty: 50, actualQty: 50, difference: 0 },
      ],
      total: 2,
    }));
  });
  await page.route('**/api/erp/stock/check/**', (route) => {
    const method = route.request().method();
    if (method === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, checkNo: 'CHK-2024-001', warehouseName: '主仓库', status: 'in_progress',
        createTime: '2024-06-10', checkerName: '管理员',
      }));
    }
    return route.fulfill(apiResponse(true));
  });
  await page.route('**/api/erp/warehouse/list*', (route) => {
    return route.fulfill(apiResponse([
      { id: 1, warehouseName: '主仓库', warehouseCode: 'WH-001', status: 1 },
      { id: 2, warehouseName: '副仓库', warehouseCode: 'WH-002', status: 1 },
    ]));
  });

  // Mock 产品列表接口
  await page.route('**/api/erp/product/list*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productName: '碳钢螺栓 M12', productCode: 'CS-BOLT-M12', category: '紧固件', unit: '个', price: 15.5 },
        { id: 2, productName: '304不锈钢管', productCode: 'SS-PIPE-304', category: '管材', unit: '根', price: 280 },
      ],
      total: 2,
    }));
  });
  await page.route('**/api/erp/product/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productName: '碳钢螺栓 M12', productCode: 'CS-BOLT-M12', category: '紧固件', unit: '个', price: 15.5, stock: 500 },
        { id: 2, productName: '304不锈钢管', productCode: 'SS-PIPE-304', category: '管材', unit: '根', price: 280, stock: 50 },
      ],
      total: 2,
      current: 1,
      size: 10,
      pages: 1,
    }));
  });
  await page.route('**/api/erp/product/**', (route) => {
    return route.fulfill(apiResponse({
      id: 1, productName: '碳钢螺栓 M12', productCode: 'CS-BOLT-M12', category: '紧固件',
      unit: '个', price: 15.5, stock: 500, minStock: 100, status: 'active',
    }));
  });

  // Mock 往来单位接口
  await page.route('**/api/erp/partner/list*', (route) => {
    return route.fulfill(apiResponse([
      { id: 1, partnerName: '供应商A', partnerCode: 'SUP-001', type: 'supplier', contactPerson: '张三', phone: '13800138001' },
      { id: 2, partnerName: '供应商B', partnerCode: 'SUP-002', type: 'supplier', contactPerson: '李四', phone: '13800138002' },
    ]));
  });

  // Mock 库存仓库接口
  await page.route('**/api/erp/stock/warehouses*', (route) => {
    return route.fulfill(apiResponse([
      { id: 1, warehouseName: '主仓库', warehouseCode: 'WH-001' },
      { id: 2, warehouseName: '副仓库', warehouseCode: 'WH-002' },
    ]));
  });

  // Mock 供应商接口
  await page.route('**/api/supplier/**', (route) => {
    return route.fulfill(apiResponse({
      records: [{ id: 1, supplierName: '供应商A', supplierCode: 'SUP-001', contactPerson: '张三', phone: '13800138001', status: 1 }],
      total: 1,
    }));
  });

  // ============================================================
  // Mock 商城管理 API
  // ============================================================

  // 商城配置
  await page.route('**/api/erp/mall/admin/config', (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, tenantId: 1, shopName: '测试商城', shopLogo: '', shopDesc: '测试商城描述',
        themeColor: '#1890ff', paymentMethods: 'offline', enableRegister: 1, enableAutoAudit: 0,
        minOrderAmount: 0, freeShippingAmount: 0, freightAmount: 10, status: 1
      }));
    }
    if (route.request().method() === 'PUT') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // 商城用户审核（分页）
  await page.route('**/api/erp/mall/admin/user/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, username: 'zhangsan', companyName: '测试公司A', nickname: '张三', phone: '13800138001', source: '注册', auditStatus: 0, status: 1, createTime: '2024-06-01 10:00:00' },
        { id: 2, username: 'lisi', companyName: '测试公司B', nickname: '李四', phone: '13800138002', source: '后台添加', auditStatus: 1, status: 1, createTime: '2024-06-02 11:00:00' },
      ],
      total: 2, current: 1, size: 20, pages: 1
    }));
  });

  // 商城用户审核操作
  await page.route('**/api/erp/mall/admin/user/*/approve', (route) => {
    return route.fulfill(apiResponse(true));
  });
  await page.route('**/api/erp/mall/admin/user/*/reject', (route) => {
    return route.fulfill(apiResponse(true));
  });
  await page.route('**/api/erp/mall/admin/user/*/status', (route) => {
    return route.fulfill(apiResponse(true));
  });

  // 商城订单通配路由（最先注册，优先级最低，仅限 GET）
  await page.route('**/api/erp/mall/admin/order/*', (route) => {
    if (route.request().method() === 'GET') {
      return route.fulfill(apiResponse({
        id: 1, orderNo: 'MALL-2024-0001', customerName: '张三', totalAmount: 1500, payAmount: 1500,
        orderStatus: 'PAID', consignee: '张三', phone: '13800138001', address: '测试地址',
        paymentStatus: 'PAID', createTime: '2024-06-01 10:00:00', remark: '',
        orderItems: [{ id: 1, productId: 'P001', productName: '测试商品A', price: 150, quantity: 10, subtotal: 1500 }]
      }));
    }
    return route.fulfill(apiResponse(true));
  });

  // 商城订单导出（仅在 order/* 通配之前注册会被拦截，但通配已先注册，此处为兜底）
  await page.route('**/api/erp/mall/admin/order/export', (route) => {
    return route.fulfill({ status: 200, contentType: 'application/octet-stream', body: '' });
  });

  // 商城订单操作（优先级高于通配）
  await page.route('**/api/erp/mall/admin/order/*/approve', (route) => {
    return route.fulfill(apiResponse(true));
  });
  await page.route('**/api/erp/mall/admin/order/*/reject', (route) => {
    return route.fulfill(apiResponse(true));
  });

  // 商城订单分页（最高优先级，最后注册）
  await page.route('**/api/erp/mall/admin/order/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, orderNo: 'MALL-2024-0001', customerName: '张三', totalAmount: 1500, payAmount: 1500, orderStatus: 'PAID', consignee: '张三', createTime: '2024-06-01 10:00:00' },
        { id: 2, orderNo: 'MALL-2024-0002', customerName: '李四', totalAmount: 2800, payAmount: 0, orderStatus: 'PENDING_PAYMENT', consignee: '李四', createTime: '2024-06-02 11:00:00' },
        { id: 3, orderNo: 'MALL-2024-0003', customerName: '王五', totalAmount: 580, payAmount: 580, orderStatus: 'COMPLETED', consignee: '王五', createTime: '2024-06-03 12:00:00' },
      ],
      total: 3, current: 1, size: 20, pages: 1
    }));
  });

  // 商城商品（分页）
  await page.route('**/api/erp/mall/admin/product/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productId: 'P001', productName: '测试商品A', imageUrl: '', salePrice: 150, marketPrice: 180, categoryName: '分类一', status: 'ON_SHELF', stockQuantity: 100, salesCount: 50 },
        { id: 2, productId: 'P002', productName: '测试商品B', imageUrl: '', salePrice: 280, marketPrice: 320, categoryName: '分类二', status: 'OFF_SHELF', stockQuantity: 200, salesCount: 30 },
        { id: 3, productId: 'P003', productName: '测试商品C', imageUrl: '', salePrice: 58, marketPrice: 68, categoryName: '分类一', status: 'ON_SHELF', stockQuantity: 500, salesCount: 200 },
      ],
      total: 3, current: 1, size: 20, pages: 1
    }));
  });

  // 商城商品 CRUD（先注册通配 route，优先级最低）
  await page.route('**/api/erp/mall/admin/product/*', (route) => {
    const method = route.request().method();
    if (method === 'PUT' || method === 'DELETE') {
      return route.fulfill(apiResponse(true));
    }
    if (method === 'GET') {
      // 单商品详情，不分页
      return route.fulfill(apiResponse({
        id: 1, productId: 'P001', productName: '测试商品A', imageUrl: '', salePrice: 150, marketPrice: 180,
        categoryName: '分类一', status: 'ON_SHELF', stockQuantity: 100, salesCount: 50, description: '测试商品描述'
      }));
    }
    return route.continue();
  });

  // 商城商品导出
  await page.route('**/api/erp/mall/admin/product/export', (route) => {
    return route.fulfill({ status: 200, contentType: 'application/octet-stream', body: '' });
  });

  // 商城商品创建
  await page.route('**/api/erp/mall/admin/product', (route) => {
    if (route.request().method() === 'POST') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // 商城商品分页（最高优先级，最后注册）
  await page.route('**/api/erp/mall/admin/product/page*', (route) => {
    return route.fulfill(apiResponse({
      records: [
        { id: 1, productId: 'P001', productName: '测试商品A', imageUrl: '', salePrice: 150, marketPrice: 180, categoryName: '分类一', status: 'ON_SHELF', stockQuantity: 100, salesCount: 50 },
        { id: 2, productId: 'P002', productName: '测试商品B', imageUrl: '', salePrice: 280, marketPrice: 320, categoryName: '分类二', status: 'OFF_SHELF', stockQuantity: 200, salesCount: 30 },
        { id: 3, productId: 'P003', productName: '测试商品C', imageUrl: '', salePrice: 58, marketPrice: 68, categoryName: '分类一', status: 'ON_SHELF', stockQuantity: 500, salesCount: 200 },
      ],
      total: 3, current: 1, size: 20, pages: 1
    }));
  });

  // 商城轮播图
  await page.route('**/api/erp/mall/admin/banner', (route) => {
    const method = route.request().method();
    if (method === 'GET') {
      return route.fulfill(apiResponse([
        { id: 1, title: '轮播图1', imageUrl: 'https://via.placeholder.com/1200x400', linkType: 'none', linkValue: '', sortOrder: 1, status: 1 },
        { id: 2, title: '轮播图2', imageUrl: 'https://via.placeholder.com/1200x400', linkType: 'product', linkValue: 'P001', sortOrder: 2, status: 1 },
      ]));
    }
    if (method === 'POST') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });
  await page.route('**/api/erp/mall/admin/banner/*', (route) => {
    const method = route.request().method();
    if (method === 'PUT' || method === 'DELETE') {
      return route.fulfill(apiResponse(true));
    }
    return route.continue();
  });

  // Mock 缓存刷新标记（将之前的 dashboard catch-all 替换为精确路径，
  // 避免 dashboard 子路径（如 stats/trend/todos/alerts）被旧 catch-all 拦截
  // 注意：需要移除或覆盖上面已注册的更精确的 dashboard 路由。
  // 由于 Playwright route 注册顺序为后注册优先，dashboard 精确路径已先注册，
  // 这个旧 catch-all 会被精确路径覆盖，但保留为未匹配子路径的兜底
  await page.route('**/api/dashboard/**', (route) => {
    return route.fulfill(apiResponse({
      todaySales: 125000, todayPurchase: 86000, pendingOrders: 12, lowStockItems: 3,
    }));
  });
}

/**
 * 通过 UI 登录（带重试）
 * 导航到登录页面，填写表单并提交
 */
export async function loginViaUi(
  page: Page,
  username: string,
  password: string,
  tenantName: string = '系统租户',
): Promise<void> {
  const maxRetries = 2;
  for (let attempt = 1; attempt <= maxRetries; attempt++) {
    try {
      await attemptLogin(page, username, password, tenantName);
      return; // 成功
    } catch (err) {
      if (attempt === maxRetries) throw err;
      console.warn(`[loginViaUi] 第 ${attempt} 次尝试失败，重试中...`);
      await page.waitForTimeout(2000);
      // 清除可能残留的状态
      await page.evaluate(() => {
        localStorage.clear();
        sessionStorage.clear();
      }).catch(() => {});
    }
  }
}

async function attemptLogin(
  page: Page,
  username: string,
  password: string,
  tenantName: string,
): Promise<void> {
  await page.goto('/login');

  // 等待登录表单渲染
  await page.waitForSelector('input[placeholder="请输入用户名"]', { timeout: 15_000 });

  // 等待验证码图片加载完成（真实后端会生成 SVG 验证码）
  await page.waitForSelector('.captcha-image img', { timeout: 10_000 }).catch(() => {
    console.warn('[loginViaUi] 验证码图片未加载，继续尝试登录');
  });

  // 填写租户名称
  const tenantInput = page.locator('input[placeholder="请输入租户名称"]');
  if (await tenantInput.isVisible({ timeout: 3000 }).catch(() => false)) {
    await tenantInput.fill(tenantName);
  }

  // 填写用户名
  await page.fill('input[placeholder="请输入用户名"]', username);

  // 填写密码
  await page.fill('input[placeholder="请输入密码"]', password);

  // 填写验证码（4位，后端不校验验证码值）
  const captchaInput = page.locator('input[placeholder="请输入验证码"]');
  if (await captchaInput.isVisible({ timeout: 3000 }).catch(() => false)) {
    await captchaInput.fill('ABCD');
  }

  // 点击登录按钮
  await page.click('button[type="submit"]');

  // 等待跳转到 dashboard（先等待导航，再等待布局）
  await page.waitForURL('**/dashboard**', { timeout: 30_000 });

  // 等待布局渲染完毕（防止路由守卫异步验证 token 后再踢回登录页）
  // 可能因路由守卫重定向导致元素短暂消失，用较长时间等待
  await page.waitForSelector('.basic-layout', { timeout: 25_000 });
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
    // 先导航到同源页面，然后设置 token，再导航到目标页
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    await setAuthToken(page, 'admin');
    await page.goto('/dashboard');
    // 等待布局渲染完成
    await page.waitForSelector('.basic-layout', { timeout: 15_000 });
    await use(page);
  },

  userPage: async ({ page }, use) => {
    // 设置 API Mock
    await setupAuthApiMocks(page, 'user');
    // 先导航到同源页面，然后设置 token，再导航到目标页
    await page.goto('/login');
    await page.waitForLoadState('domcontentloaded');
    await setAuthToken(page, 'user');
    await page.goto('/dashboard');
    // 等待布局渲染完成
    await page.waitForSelector('.basic-layout', { timeout: 15_000 });
    await use(page);
  },
});

export { expect } from '@playwright/test';
export { ADMIN_CREDENTIALS, USER_CREDENTIALS };
