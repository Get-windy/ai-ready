/**
 * 한국어 언어 팩
 */
export default {
  // 공통
  common: {
    confirm: '확인',
    cancel: '취소',
    save: '저장',
    delete: '삭제',
    edit: '편집',
    add: '추가',
    search: '검색',
    reset: '초기화',
    submit: '제출',
    back: '뒤로',
    loading: '로딩 중...',
    noData: '데이터가 없습니다',
    success: '성공했습니다',
    failed: '실패했습니다',
    required: '필수 항목입니다',
    pleaseInput: '입력해 주세요',
    pleaseSelect: '선택해 주세요',
    all: '전체',
    enable: '활성화',
    disable: '비활성화',
    status: '상태',
    action: '작업',
    createTime: '생성일',
    updateTime: '수정일',
    remark: '비고',
    description: '설명',
    detail: '상세',
    export: '내보내기',
    import: '가져오기',
    refresh: '새로고침',
    more: '더보기',
    tips: '팁',
    warning: '경고',
    error: '오류',
    info: '정보'
  },
  
  // 네비게이션 메뉴
  menu: {
    dashboard: '대시보드',
    user: '사용자 관리',
    role: '역할 관리',
    permission: '권한 관리',
    product: '상품 관리',
    order: '주문 관리',
    customer: '고객 관리',
    inventory: '재고 관리',
    report: '리포트',
    settings: '시스템 설정',
    profile: '프로필',
    logout: '로그아웃'
  },
  
  // 로그인
  login: {
    title: 'AI-Ready 지기업',
    subtitle: '기업 관리 시스템',
    username: '사용자명',
    password: '비밀번호',
    rememberMe: '로그인 상태 유지',
    forgotPassword: '비밀번호를 잊으셨나요?',
    login: '로그인',
    register: '회원가입',
    loginSuccess: '로그인 성공',
    loginFailed: '로그인 실패',
    usernameRequired: '사용자명을 입력해 주세요',
    passwordRequired: '비밀번호를 입력해 주세요',
    captchaRequired: '캡차를 입력해 주세요'
  },
  
  // 사용자 관리
  user: {
    title: '사용자 관리',
    username: '사용자명',
    nickname: '닉네임',
    email: '이메일',
    phone: '전화번호',
    avatar: '아바타',
    department: '부서',
    position: '직위',
    role: '역할',
    lastLoginTime: '마지막 로그인',
    password: '비밀번호',
    confirmPassword: '비밀번호 확인',
    newPassword: '새 비밀번호',
    oldPassword: '기존 비밀번호',
    changePassword: '비밀번호 변경',
    resetPassword: '비밀번호 초기화',
    addUser: '사용자 추가',
    editUser: '사용자 편집',
    deleteUser: '사용자 삭제',
    deleteUserConfirm: '이 사용자를 삭제하시겠습니까?',
    userStatus: '사용자 상태'
  },
  
  // 역할 관리
  role: {
    title: '역할 관리',
    roleName: '역할명',
    roleCode: '역할 코드',
    roleDesc: '역할 설명',
    permissions: '권한 설정',
    addRole: '역할 추가',
    editRole: '역할 편집',
    deleteRole: '역할 삭제',
    deleteRoleConfirm: '이 역할을 삭제하시겠습니까?',
    assignPermission: '권한 할당'
  },
  
  // 상품 관리
  product: {
    title: '상품 관리',
    productName: '상품명',
    productCode: '상품 코드',
    category: '카테고리',
    price: '가격',
    stock: '재고',
    unit: '단위',
    image: '상품 이미지',
    addProduct: '상품 추가',
    editProduct: '상품 편집',
    deleteProduct: '상품 삭제'
  },
  
  // 주문 관리
  order: {
    title: '주문 관리',
    orderNo: '주문 번호',
    customer: '고객',
    totalAmount: '총 금액',
    orderStatus: '주문 상태',
    paymentStatus: '결제 상태',
    shippingStatus: '배송 상태',
    orderDate: '주문일',
    addOrder: '주문 추가',
    viewOrder: '주문 상세',
    cancelOrder: '주문 취소',
    confirmOrder: '주문 확인',
    pending: '대기 중',
    confirmed: '확인됨',
    shipped: '배송됨',
    completed: '완료',
    cancelled: '취소됨'
  },
  
  // 고객 관리
  customer: {
    title: '고객 관리',
    customerName: '고객명',
    contact: '담당자',
    phone: '전화번호',
    email: '이메일',
    address: '주소',
    level: '고객 등급',
    addCustomer: '고객 추가',
    editCustomer: '고객 편집',
    deleteCustomer: '고객 삭제'
  },
  
  // 재고 관리
  inventory: {
    title: '재고 관리',
    warehouse: '창고',
    quantity: '수량',
    inStock: '입고',
    outStock: '출고',
    stockCheck: '재고盘点',
    stockWarning: '재고 경고',
    minStock: '최소 재고',
    maxStock: '최대 재고'
  },
  
  // 시스템 설정
  settings: {
    title: '시스템 설정',
    basic: '기본 설정',
    security: '보안 설정',
    notification: '알림 설정',
    language: '언어',
    theme: '테마',
    timezone: '시간대',
    dateFormat: '날짜 형식'
  },
  
  // 검증 메시지
  validation: {
    required: '필수 항목입니다',
    email: '유효한 이메일을 입력해 주세요',
    phone: '유효한 전화번호를 입력해 주세요',
    url: '유효한 URL을 입력해 주세요',
    minLength: '최소 {min}자 이상 입력해 주세요',
    maxLength: '최대 {max}자 이하로 입력해 주세요',
    min: '{min} 이상이어야 합니다',
    max: '{max} 이하여야 합니다',
    range: '{min}에서 {max} 사이의 값을 입력해 주세요',
    password: '비밀번호는 6~20자의 영숫자 조합입니다',
    confirmPassword: '비밀번호가 일치하지 않습니다'
  },
  
  // 오류 메시지
  error: {
    networkError: '네트워크 연결에 실패했습니다',
    serverError: '서버 오류가 발생했습니다',
    unauthorized: '인증이 필요합니다. 다시 로그인해 주세요',
    forbidden: '접근 권한이 없습니다',
    notFound: '리소스를 찾을 수 없습니다',
    timeout: '요청 시간이 초과되었습니다',
    unknown: '알 수 없는 오류가 발생했습니다'
  },
  
  // 성공 메시지
  success: {
    save: '저장되었습니다',
    delete: '삭제되었습니다',
    update: '수정되었습니다',
    submit: '제출되었습니다',
    login: '로그인되었습니다',
    logout: '로그아웃되었습니다',
    upload: '업로드되었습니다',
    download: '다운로드되었습니다',
    import: '가져오기되었습니다',
    export: '내보내기되었습니다'
  }
}