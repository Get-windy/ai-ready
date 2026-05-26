export default {
  // 通用
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    add: '新增',
    search: '搜索',
    reset: '重置',
    submit: '提交',
    back: '返回',
    loading: '加载中...',
    noData: '暂无数据',
    success: '操作成功',
    failed: '操作失败',
    required: '此项为必填项',
    pleaseInput: '请输入',
    pleaseSelect: '请选择',
    all: '全部',
    enable: '启用',
    disable: '禁用',
    status: '状态',
    action: '操作',
    createTime: '创建时间',
    updateTime: '更新时间',
    remark: '备注',
    description: '描述',
    detail: '详情',
    export: '导出',
    import: '导入',
    refresh: '刷新',
    more: '更多',
    tips: '提示',
    warning: '警告',
    error: '错误',
    info: '信息'
  },
  
  // 导航菜单
  menu: {
    dashboard: '仪表盘',
    user: '用户管理',
    role: '角色管理',
    permission: '权限管理',
    product: '产品管理',
    order: '订单管理',
    customer: '客户管理',
    inventory: '库存管理',
    report: '报表中心',
    settings: '系统设置',
    profile: '个人中心',
    logout: '退出登录'
  },
  
  // 登录
  login: {
    title: 'AI-Ready 智企连',
    subtitle: '企业管理系统',
    username: '用户名',
    password: '密码',
    rememberMe: '记住我',
    forgotPassword: '忘记密码？',
    login: '登录',
    register: '注册',
    loginSuccess: '登录成功',
    loginFailed: '登录失败',
    usernameRequired: '请输入用户名',
    passwordRequired: '请输入密码',
    captchaRequired: '请输入验证码'
  },
  
  // 用户管理
  user: {
    title: '用户管理',
    username: '用户名',
    nickname: '昵称',
    email: '邮箱',
    phone: '手机号',
    avatar: '头像',
    department: '部门',
    position: '职位',
    role: '角色',
    lastLoginTime: '最后登录时间',
    password: '密码',
    confirmPassword: '确认密码',
    newPassword: '新密码',
    oldPassword: '原密码',
    changePassword: '修改密码',
    resetPassword: '重置密码',
    addUser: '新增用户',
    editUser: '编辑用户',
    deleteUser: '删除用户',
    deleteUserConfirm: '确定要删除该用户吗？',
    userStatus: '用户状态'
  },
  
  // 角色管理
  role: {
    title: '角色管理',
    roleName: '角色名称',
    roleCode: '角色编码',
    roleDesc: '角色描述',
    permissions: '权限配置',
    addRole: '新增角色',
    editRole: '编辑角色',
    deleteRole: '删除角色',
    deleteRoleConfirm: '确定要删除该角色吗？',
    assignPermission: '分配权限'
  },
  
  // 产品管理
  product: {
    title: '产品管理',
    productName: '产品名称',
    productCode: '产品编码',
    category: '产品分类',
    price: '价格',
    stock: '库存',
    unit: '单位',
    image: '产品图片',
    addProduct: '新增产品',
    editProduct: '编辑产品',
    deleteProduct: '删除产品'
  },
  
  // 订单管理
  order: {
    title: '订单管理',
    orderNo: '订单编号',
    customer: '客户',
    totalAmount: '总金额',
    orderStatus: '订单状态',
    paymentStatus: '支付状态',
    shippingStatus: '发货状态',
    orderDate: '下单日期',
    addOrder: '新增订单',
    viewOrder: '查看订单',
    cancelOrder: '取消订单',
    confirmOrder: '确认订单',
    pending: '待处理',
    confirmed: '已确认',
    shipped: '已发货',
    completed: '已完成',
    cancelled: '已取消'
  },
  
  // 客户管理
  customer: {
    title: '客户管理',
    customerName: '客户名称',
    contact: '联系人',
    phone: '联系电话',
    email: '邮箱',
    address: '地址',
    level: '客户等级',
    addCustomer: '新增客户',
    editCustomer: '编辑客户',
    deleteCustomer: '删除客户'
  },
  
  // 库存管理
  inventory: {
    title: '库存管理',
    warehouse: '仓库',
    quantity: '数量',
    inStock: '入库',
    outStock: '出库',
    stockCheck: '库存盘点',
    stockWarning: '库存预警',
    minStock: '最小库存',
    maxStock: '最大库存'
  },
  
  // 系统设置
  settings: {
    title: '系统设置',
    basic: '基础设置',
    security: '安全设置',
    notification: '通知设置',
    language: '语言',
    theme: '主题',
    timezone: '时区',
    dateFormat: '日期格式'
  },
  
  // 验证消息
  validation: {
    required: '此项为必填项',
    email: '请输入有效的邮箱地址',
    phone: '请输入有效的手机号码',
    url: '请输入有效的URL',
    minLength: '长度不能少于{min}个字符',
    maxLength: '长度不能超过{max}个字符',
    min: '不能小于{min}',
    max: '不能大于{max}',
    range: '必须在{min}和{max}之间',
    password: '密码长度6-20位，需包含字母和数字',
    confirmPassword: '两次密码输入不一致'
  },
  
  // 错误消息
  error: {
    networkError: '网络连接失败，请检查网络',
    serverError: '服务器错误，请稍后重试',
    unauthorized: '未授权，请重新登录',
    forbidden: '没有权限访问',
    notFound: '资源不存在',
    timeout: '请求超时，请稍后重试',
    unknown: '未知错误'
  },
  
  // 成功消息
  success: {
    save: '保存成功',
    delete: '删除成功',
    update: '更新成功',
    submit: '提交成功',
    login: '登录成功',
    logout: '退出成功',
    upload: '上传成功',
    download: '下载成功',
    import: '导入成功',
    export: '导出成功'
  }
}