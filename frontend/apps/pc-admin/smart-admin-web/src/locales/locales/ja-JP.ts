/**
 * 日本語言語パック
 */
export default {
  // 共通
  common: {
    confirm: '確認',
    cancel: 'キャンセル',
    save: '保存',
    delete: '削除',
    edit: '編集',
    add: '追加',
    search: '検索',
    reset: 'リセット',
    submit: '送信',
    back: '戻る',
    loading: '読み込み中...',
    noData: 'データがありません',
    success: '成功しました',
    failed: '失敗しました',
    required: '必須項目です',
    pleaseInput: '入力してください',
    pleaseSelect: '選択してください',
    all: 'すべて',
    enable: '有効',
    disable: '無効',
    status: 'ステータス',
    action: '操作',
    createTime: '作成日時',
    updateTime: '更新日時',
    remark: '備考',
    description: '説明',
    detail: '詳細',
    export: 'エクスポート',
    import: 'インポート',
    refresh: '更新',
    more: '詳細',
    tips: 'ヒント',
    warning: '警告',
    error: 'エラー',
    info: '情報'
  },
  
  // ナビゲーションメニュー
  menu: {
    dashboard: 'ダッシュボード',
    user: 'ユーザー管理',
    role: 'ロール管理',
    permission: '権限管理',
    product: '商品管理',
    order: '注文管理',
    customer: '顧客管理',
    inventory: '在庫管理',
    report: 'レポート',
    settings: 'システム設定',
    profile: 'プロフィール',
    logout: 'ログアウト'
  },
  
  // ログイン
  login: {
    title: 'AI-Ready 智企連',
    subtitle: '企業管理システム',
    username: 'ユーザー名',
    password: 'パスワード',
    rememberMe: 'ログイン状態を保持',
    forgotPassword: 'パスワードをお忘れですか？',
    login: 'ログイン',
    register: '登録',
    loginSuccess: 'ログインしました',
    loginFailed: 'ログインに失敗しました',
    usernameRequired: 'ユーザー名を入力してください',
    passwordRequired: 'パスワードを入力してください',
    captchaRequired: 'キャプチャを入力してください'
  },
  
  // ユーザー管理
  user: {
    title: 'ユーザー管理',
    username: 'ユーザー名',
    nickname: 'ニックネーム',
    email: 'メールアドレス',
    phone: '電話番号',
    avatar: 'アバター',
    department: '部署',
    position: '役職',
    role: '役割',
    lastLoginTime: '最終ログイン',
    password: 'パスワード',
    confirmPassword: 'パスワード確認',
    newPassword: '新しいパスワード',
    oldPassword: '現在のパスワード',
    changePassword: 'パスワード変更',
    resetPassword: 'パスワードリセット',
    addUser: 'ユーザー追加',
    editUser: 'ユーザー編集',
    deleteUser: 'ユーザー削除',
    deleteUserConfirm: 'このユーザーを削除しますか？',
    userStatus: 'ユーザーステータス'
  },
  
  // ロール管理
  role: {
    title: 'ロール管理',
    roleName: 'ロール名',
    roleCode: 'ロールコード',
    roleDesc: 'ロール説明',
    permissions: '権限設定',
    addRole: 'ロール追加',
    editRole: 'ロール編集',
    deleteRole: 'ロール削除',
    deleteRoleConfirm: 'このロールを削除しますか？',
    assignPermission: '権限割り当て'
  },
  
  // 商品管理
  product: {
    title: '商品管理',
    productName: '商品名',
    productCode: '商品コード',
    category: 'カテゴリ',
    price: '価格',
    stock: '在庫',
    unit: '単位',
    image: '商品画像',
    addProduct: '商品追加',
    editProduct: '商品編集',
    deleteProduct: '商品削除'
  },
  
  // 注文管理
  order: {
    title: '注文管理',
    orderNo: '注文番号',
    customer: '顧客',
    totalAmount: '合計金額',
    orderStatus: '注文ステータス',
    paymentStatus: '支払ステータス',
    shippingStatus: '配送ステータス',
    orderDate: '注文日',
    addOrder: '注文追加',
    viewOrder: '注文詳細',
    cancelOrder: '注文キャンセル',
    confirmOrder: '注文確認',
    pending: '保留中',
    confirmed: '確認済',
    shipped: '発送済',
    completed: '完了',
    cancelled: 'キャンセル済'
  },
  
  // 顧客管理
  customer: {
    title: '顧客管理',
    customerName: '顧客名',
    contact: '担当者',
    phone: '電話番号',
    email: 'メールアドレス',
    address: '住所',
    level: '顧客レベル',
    addCustomer: '顧客追加',
    editCustomer: '顧客編集',
    deleteCustomer: '顧客削除'
  },
  
  // 在庫管理
  inventory: {
    title: '在庫管理',
    warehouse: '倉庫',
    quantity: '数量',
    inStock: '入庫',
    outStock: '出庫',
    stockCheck: '在庫棚卸',
    stockWarning: '在庫警告',
    minStock: '最小在庫',
    maxStock: '最大在庫'
  },
  
  // システム設定
  settings: {
    title: 'システム設定',
    basic: '基本設定',
    security: 'セキュリティ設定',
    notification: '通知設定',
    language: '言語',
    theme: 'テーマ',
    timezone: 'タイムゾーン',
    dateFormat: '日付形式'
  },
  
  // バリデーションメッセージ
  validation: {
    required: '必須項目です',
    email: '有効なメールアドレスを入力してください',
    phone: '有効な電話番号を入力してください',
    url: '有効なURLを入力してください',
    minLength: '{min}文字以上で入力してください',
    maxLength: '{max}文字以内で入力してください',
    min: '{min}以上にしてください',
    max: '{max}以下にしてください',
    range: '{min}から{max}の間で入力してください',
    password: 'パスワードは6〜20文字の英数字混合です',
    confirmPassword: 'パスワードが一致しません'
  },
  
  // エラーメッセージ
  error: {
    networkError: 'ネットワーク接続に失敗しました',
    serverError: 'サーバーエラーが発生しました',
    unauthorized: '認証が必要です。再度ログインしてください',
    forbidden: 'アクセス権限がありません',
    notFound: 'リソースが見つかりません',
    timeout: 'リクエストがタイムアウトしました',
    unknown: '不明なエラーが発生しました'
  },
  
  // 成功メッセージ
  success: {
    save: '保存しました',
    delete: '削除しました',
    update: '更新しました',
    submit: '送信しました',
    login: 'ログインしました',
    logout: 'ログアウトしました',
    upload: 'アップロードしました',
    download: 'ダウンロードしました',
    import: 'インポートしました',
    export: 'エクスポートしました'
  }
}