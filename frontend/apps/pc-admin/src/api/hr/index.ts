import request from '@/utils/request'
import type { PageResult } from '@/api/common'

/**
 * 人力资源模块 API
 *
 * ⚠️ 路径一律写**相对路径**（`/hr/...`），绝不写 `/api/hr/...`：
 * `utils/request.ts` 的 axios 实例已设 `baseURL = '/api'`，而 vite proxy 无 rewrite，
 * 再补一层 `/api` 会合成 `/api/api/hr/...` 直接 404。
 * （后端 `HrController` 的类级前缀已由裸 `/hr` 修正为 `/api/hr`；前端不需要任何补偿。）
 */

// ── 岗位管理（HR 岗位主数据 hr_position） ──

export interface HrPosition {
  id: number | string
  tenantId: number
  deptId: number
  deptName?: string
  positionCode: string
  positionName: string
  positionLevel: number
  responsibility: string
  quotaCount: number
  currentCount: number
  overQuota?: boolean
  sort: number
  status: number
  remark?: string
  createTime: string
}

export interface HrPositionQuery {
  pageNum?: number
  pageSize?: number
  deptId?: number
  positionName?: string
  positionCode?: string
  positionLevel?: number
  status?: number
}

export const hrPositionApi = {
  page(params: HrPositionQuery): Promise<PageResult<HrPosition>> {
    return request.get('/hr/positions/page', { params })
  },
  list(): Promise<HrPosition[]> {
    return request.get('/hr/positions/list')
  },
  listByDept(deptId: number): Promise<HrPosition[]> {
    return request.get(`/hr/positions/by-dept/${deptId}`)
  },
  getById(id: number | string): Promise<HrPosition> {
    return request.get(`/hr/positions/${id}`)
  },
  nextCode(): Promise<string> {
    return request.get('/hr/positions/next-code')
  },
  create(data: Partial<HrPosition>): Promise<number> {
    return request.post('/hr/positions', data)
  },
  update(id: number | string, data: Partial<HrPosition>): Promise<boolean> {
    return request.put(`/hr/positions/${id}`, data)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/positions/${id}`)
  },
  stat(params?: { deptId?: number }): Promise<Record<string, any>> {
    return request.get('/hr/positions/stat', { params })
  },
}

// ── 员工管理 ──

export interface HrEmployee {
  id: number | string
  tenantId: number
  employeeNo: string
  employeeName: string
  deptId: number
  deptName?: string
  positionId: number
  positionName?: string
  userId?: number
  userLoginName?: string
  gender: number
  birthDate: string
  phone: string
  email: string
  idCard: string
  education: number
  school: string
  major: string
  hireDate: string
  regularDate?: string
  leaveDate: string
  resignType?: number
  resignReason?: string
  employeeType: number
  status: number
  avatarUrl: string
  emergencyContact: string
  emergencyPhone: string
  hometownAddress: string
  currentAddress: string
  remark: string
  workYears?: number
  createTime: string
}

export interface HrEmployeeQuery {
  pageNum?: number
  pageSize?: number
  deptId?: number
  positionId?: number
  keyword?: string
  employeeName?: string
  employeeNo?: string
  phone?: string
  status?: number
  employeeType?: number
  gender?: number
  education?: number
  hireDateStart?: string
  hireDateEnd?: string
  leaveDateStart?: string
  leaveDateEnd?: string
  sortField?: string
  sortOrder?: string
}

export const hrEmployeeApi = {
  page(params: HrEmployeeQuery): Promise<PageResult<HrEmployee>> {
    return request.get('/hr/employees/page', { params })
  },
  getById(id: number | string): Promise<HrEmployee> {
    return request.get(`/hr/employees/${id}`)
  },
  nextNo(): Promise<string> {
    return request.get('/hr/employees/next-no')
  },
  create(data: Partial<HrEmployee>): Promise<number> {
    return request.post('/hr/employees', data)
  },
  update(id: number | string, data: Partial<HrEmployee>): Promise<boolean> {
    return request.put(`/hr/employees/${id}`, data)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/employees/${id}`)
  },
  batchRemove(ids: (number | string)[]): Promise<boolean> {
    return request.delete('/hr/employees/batch', { data: ids })
  },
  updateStatus(id: number | string, status: number): Promise<boolean> {
    return request.put(`/hr/employees/${id}/status`, null, { params: { status } })
  },
  regularize(id: number | string, params: { regularDate?: string; remark?: string }): Promise<boolean> {
    return request.post(`/hr/employees/${id}/regularize`, null, { params })
  },
  resign(id: number | string, params: { lastWorkDate?: string; resignType?: number; resignReason?: string }): Promise<boolean> {
    return request.post(`/hr/employees/${id}/resign`, null, { params })
  },
  contracts(id: number | string): Promise<HrContract[]> {
    return request.get(`/hr/employees/${id}/contracts`)
  },
  stat(params?: { deptId?: number }): Promise<Record<string, any>> {
    return request.get('/hr/employees/stat', { params })
  },
  changes(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; changeType?: string; dateFrom?: string; dateTo?: string }): Promise<PageResult<HrEmployeeChange>> {
    return request.get('/hr/employee-changes/page', { params })
  },
}

// ── 人事异动 ──

export interface HrEmployeeChange {
  id: number | string
  employeeId: number | string
  employeeNo: string
  employeeName: string
  changeType: string
  effectiveDate: string
  beforeJson: string
  afterJson: string
  reason: string
  operatorId: number
  operatorName: string
  remark: string
  createTime: string
}

export const CHANGE_TYPE_MAP: Record<string, string> = {
  ENTRY: '入职',
  REGULAR: '转正',
  TRANSFER: '调岗',
  SALARY_ADJUST: '调薪',
  RESIGN: '离职',
  REHIRE: '复职',
  UPDATE: '信息变更',
}

// ── 合同管理 ──

export interface HrContract {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  contractType: number
  contractNo: string
  contractName: string
  startDate: string
  endDate: string
  trialDateEnd?: string
  salaryAmount: number
  signDate: string
  status: number
  terminateReason?: string
  remark?: string
  createTime?: string
}

export const CONTRACT_TYPE_MAP: Record<number, string> = { 1: '固定期限', 2: '无固定期限', 3: '试用期' }
export const CONTRACT_STATUS_MAP: Record<number, string> = { 0: '待签', 1: '生效', 2: '到期', 3: '终止' }

export const hrContractApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; contractNo?: string; contractType?: number; status?: number }): Promise<PageResult<HrContract>> {
    return request.get('/hr/contracts/page', { params })
  },
  listByEmployee(employeeId: number | string): Promise<HrContract[]> {
    return request.get(`/hr/employees/${employeeId}/contracts`)
  },
  nextNo(): Promise<string> {
    return request.get('/hr/contracts/next-no')
  },
  expiring(days = 30): Promise<HrContract[]> {
    return request.get('/hr/contracts/expiring', { params: { days } })
  },
  create(data: Partial<HrContract>): Promise<number> {
    return request.post('/hr/contracts', data)
  },
  update(id: number | string, data: Partial<HrContract>): Promise<boolean> {
    return request.put(`/hr/contracts/${id}`, data)
  },
  updateStatus(id: number | string, status: number): Promise<boolean> {
    return request.put(`/hr/contracts/${id}/status`, null, { params: { status } })
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/contracts/${id}`)
  },
}

// ── 考勤管理 ──

export interface HrAttendance {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  deptName?: string
  attendanceDate: string
  clockInTime: string
  clockOutTime: string
  status: string
  lateMinutes: number
  earlyMinutes: number
  workHours: number
  leaveRequestId?: number
  remark?: string
}

export interface HrAttendanceRule {
  id?: number | string
  workStartTime: string
  workEndTime: string
  lateGraceMinutes: number
  earlyGraceMinutes: number
  standardWorkHours: number
  autoAbsent: number
  remark?: string
}

export const hrAttendanceApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; deptId?: number; month?: string; status?: string; dateStart?: string; dateEnd?: string }): Promise<PageResult<HrAttendance>> {
    return request.get('/hr/attendance/page', { params })
  },
  getById(id: number | string): Promise<HrAttendance> {
    return request.get(`/hr/attendance/${id}`)
  },
  clockIn(employeeId: number | string): Promise<boolean> {
    return request.post('/hr/attendance/clock-in', null, { params: { employeeId } })
  },
  clockOut(employeeId: number | string): Promise<boolean> {
    return request.post('/hr/attendance/clock-out', null, { params: { employeeId } })
  },
  create(data: Partial<HrAttendance>): Promise<number> {
    return request.post('/hr/attendance', data)
  },
  update(id: number | string, data: Partial<HrAttendance>): Promise<boolean> {
    return request.put(`/hr/attendance/${id}`, data)
  },
  recalculate(id: number | string): Promise<boolean> {
    return request.put(`/hr/attendance/${id}/recalculate`)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/attendance/${id}`)
  },
  batchRemove(ids: (number | string)[]): Promise<boolean> {
    return request.delete('/hr/attendance/batch', { data: ids })
  },
  stat(params: { month?: string; deptId?: number; employeeId?: number | string }): Promise<Record<string, any>> {
    return request.get('/hr/attendance/stat', { params })
  },
  getRule(): Promise<HrAttendanceRule> {
    return request.get('/hr/attendance/rule')
  },
  saveRule(data: Partial<HrAttendanceRule>): Promise<HrAttendanceRule> {
    return request.put('/hr/attendance/rule', data)
  },
}

// ── 请假管理 ──

export interface HrLeaveRequest {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  deptName?: string
  leaveType: string
  startDate: string
  endDate: string
  days: number
  reason: string
  status: number
  approveId: number
  approveName?: string
  approveComment: string
  approveTime: string
}

export interface HrLeaveQuota {
  id?: number | string
  leaveType: string
  year: number
  quotaDays: number
  remark?: string
}

export const hrLeaveRequestApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; deptId?: number; status?: number; leaveType?: string; startDateFrom?: string; startDateTo?: string }): Promise<PageResult<HrLeaveRequest>> {
    return request.get('/hr/leave/page', { params })
  },
  getById(id: number | string): Promise<HrLeaveRequest> {
    return request.get(`/hr/leave/${id}`)
  },
  listByEmployee(employeeId: number | string): Promise<HrLeaveRequest[]> {
    return request.get(`/hr/leave/by-employee/${employeeId}`)
  },
  submit(data: Partial<HrLeaveRequest>): Promise<number> {
    return request.post('/hr/leave', data)
  },
  update(id: number | string, data: Partial<HrLeaveRequest>): Promise<boolean> {
    return request.put(`/hr/leave/${id}`, data)
  },
  approve(id: number | string, comment?: string): Promise<boolean> {
    return request.put(`/hr/leave/${id}/approve`, null, { params: { comment } })
  },
  reject(id: number | string, comment?: string): Promise<boolean> {
    return request.put(`/hr/leave/${id}/reject`, null, { params: { comment } })
  },
  cancel(id: number | string): Promise<boolean> {
    return request.put(`/hr/leave/${id}/cancel`)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/leave/${id}`)
  },
  balance(employeeId: number | string, year?: number): Promise<Record<string, any>> {
    return request.get('/hr/leave/balance', { params: { employeeId, year } })
  },
  quotas(year?: number): Promise<HrLeaveQuota[]> {
    return request.get('/hr/leave/quota', { params: { year } })
  },
  saveQuota(data: Partial<HrLeaveQuota>): Promise<HrLeaveQuota> {
    return request.put('/hr/leave/quota', data)
  },
  stat(params: { year?: number; deptId?: number }): Promise<Record<string, any>> {
    return request.get('/hr/leave/stat', { params })
  },
}

// ── 薪资管理 ──

export interface HrSalaryStructure {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  deptName?: string
  baseSalary: number
  performanceSalary: number
  positionAllowance: number
  transportAllowance: number
  mealAllowance: number
  housingAllowance: number
  otherAllowance: number
  socialBase: number
  fundBase: number
  effectiveDate: string
  expiryDate?: string
  status: number
  remark?: string
}

export interface HrSalaryPayment {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  deptName?: string
  paymentMonth: string
  baseAmount: number
  performanceAmount: number
  allowanceAmount: number
  overtimeAmount: number
  deductAmount: number
  socialDeduct: number
  fundDeduct: number
  taxDeduct: number
  grossAmount: number
  actualAmount: number
  paymentDate: string
  status: number
  workHours?: number
  absentDays?: number
  remark?: string
}

export const hrSalaryApi = {
  pageStructures(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; status?: number }): Promise<PageResult<HrSalaryStructure>> {
    return request.get('/hr/salary/structure/page', { params })
  },
  getStructure(employeeId: number | string): Promise<HrSalaryStructure> {
    return request.get(`/hr/salary/structure/${employeeId}`)
  },
  listStructures(employeeId: number | string): Promise<HrSalaryStructure[]> {
    return request.get(`/hr/salary/structure/by-employee/${employeeId}`)
  },
  createStructure(data: Partial<HrSalaryStructure>): Promise<number> {
    return request.post('/hr/salary/structure', data)
  },
  updateStructure(id: number | string, data: Partial<HrSalaryStructure>): Promise<boolean> {
    return request.put(`/hr/salary/structure/${id}`, data)
  },
  removeStructure(id: number | string): Promise<boolean> {
    return request.delete(`/hr/salary/structure/${id}`)
  },
  pagePayments(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; deptId?: number; paymentMonth?: string; status?: number; keyword?: string }): Promise<PageResult<HrSalaryPayment>> {
    return request.get('/hr/salary/payment/page', { params })
  },
  getPayment(id: number | string): Promise<{ payment: HrSalaryPayment; structure: HrSalaryStructure }> {
    return request.get(`/hr/salary/payment/${id}`)
  },
  confirmPayment(id: number | string): Promise<boolean> {
    return request.put(`/hr/salary/payment/${id}/confirm`)
  },
  batchConfirm(ids: (number | string)[]): Promise<boolean> {
    return request.put('/hr/salary/payment/batch-confirm', ids)
  },
  revokePayment(id: number | string): Promise<boolean> {
    return request.put(`/hr/salary/payment/${id}/revoke`)
  },
  removePayment(id: number | string): Promise<boolean> {
    return request.delete(`/hr/salary/payment/${id}`)
  },
  generateMonthlyPayment(paymentMonth: string): Promise<Record<string, any>> {
    return request.post('/hr/salary/payment/generate', null, { params: { paymentMonth } })
  },
  stat(params: { paymentMonth?: string; deptId?: number }): Promise<Record<string, any>> {
    return request.get('/hr/salary/payment/stat', { params })
  },
}

// ── 绩效管理 ──

export interface HrPerformance {
  id: number | string
  tenantId?: number
  employeeId: number | string
  employeeName?: string
  employeeNo?: string
  deptName?: string
  reviewPeriod: string
  reviewType: string
  score: number
  level: string
  attitudeScore: number
  abilityScore: number
  achievementScore: number
  performanceCoefficient: number
  comment: string
  reviewerId: number
  reviewerName: string
  reviewTime: string
  status: number
  remark?: string
}

export const hrPerformanceApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number | string; deptId?: number; reviewPeriod?: string; reviewType?: string; level?: string; status?: number }): Promise<PageResult<HrPerformance>> {
    return request.get('/hr/performance/page', { params })
  },
  getById(id: number | string): Promise<HrPerformance> {
    return request.get(`/hr/performance/${id}`)
  },
  submit(data: Partial<HrPerformance>): Promise<number> {
    return request.post('/hr/performance', data)
  },
  update(id: number | string, data: Partial<HrPerformance>): Promise<boolean> {
    return request.put(`/hr/performance/${id}`, data)
  },
  confirm(id: number | string): Promise<boolean> {
    return request.put(`/hr/performance/${id}/confirm`)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/performance/${id}`)
  },
  stat(params: { reviewPeriod?: string; deptId?: number }): Promise<Record<string, any>> {
    return request.get('/hr/performance/stat', { params })
  },
}

// ── 招聘管理 ──

export interface HrRecruitment {
  id: number | string
  positionId: number
  positionName: string
  deptId: number
  deptName: string
  headcount: number
  channel: string
  urgency: number
  status: number
  requiredEducation: number
  requiredExperience: string
  salaryMin: number
  salaryMax: number
  description: string
  requirements: string
  publisherId: number
  publisherName: string
  publishDate: string
  expireDate: string
  applicantCount: number
  hiredCount: number
  remark: string
  createTime: string
}

export interface HrCandidate {
  id: number | string
  recruitmentId: number | string
  name: string
  gender: number
  phone: string
  email: string
  birthDate: string
  education: number
  school: string
  major: string
  experience: string
  currentCompany: string
  currentPosition: string
  expectedSalary: number
  source: string
  resumeUrl: string
  status: number
  interviewerId: number
  interviewerName: string
  interviewTime: string
  interviewComment: string
  rating: number
  remark: string
  createTime: string
}

export const hrRecruitmentApi = {
  page(params: { pageNum?: number; pageSize?: number; status?: number; positionName?: string; channel?: string; startDate?: string; endDate?: string }): Promise<PageResult<HrRecruitment>> {
    return request.get('/hr/recruitment/page', { params })
  },
  list(params: { status?: number; positionName?: string; channel?: string; startDate?: string; endDate?: string }): Promise<HrRecruitment[]> {
    return request.get('/hr/recruitment/list', { params })
  },
  stat(params?: { status?: number }): Promise<Record<string, any>> {
    return request.get('/hr/recruitment/stat', { params })
  },
  getById(id: number | string): Promise<HrRecruitment> {
    return request.get(`/hr/recruitment/${id}`)
  },
  create(data: Partial<HrRecruitment>): Promise<number> {
    return request.post('/hr/recruitment', data)
  },
  update(id: number | string, data: Partial<HrRecruitment>): Promise<boolean> {
    return request.put(`/hr/recruitment/${id}`, data)
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/recruitment/${id}`)
  },
  updateStatus(id: number | string, status: number): Promise<boolean> {
    return request.put(`/hr/recruitment/${id}/status`, null, { params: { status } })
  },
}

export const hrCandidateApi = {
  page(params: { pageNum?: number; pageSize?: number; recruitmentId?: number | string; name?: string; status?: number }): Promise<PageResult<HrCandidate>> {
    return request.get('/hr/candidate/page', { params })
  },
  stat(params?: { recruitmentId?: number | string }): Promise<Record<string, any>> {
    return request.get('/hr/candidate/stat', { params })
  },
  getById(id: number | string): Promise<HrCandidate> {
    return request.get(`/hr/candidate/${id}`)
  },
  create(data: Partial<HrCandidate>): Promise<number> {
    return request.post('/hr/candidate', data)
  },
  update(id: number | string, data: Partial<HrCandidate>): Promise<boolean> {
    return request.put(`/hr/candidate/${id}`, data)
  },
  updateStatus(id: number | string, status: number): Promise<boolean> {
    return request.put(`/hr/candidate/${id}/status`, null, { params: { status } })
  },
  recordInterview(id: number | string, params: { interviewComment?: string; rating?: number }): Promise<boolean> {
    return request.put(`/hr/candidate/${id}/interview`, null, { params })
  },
  hire(id: number | string, data?: Partial<HrEmployee>): Promise<number> {
    return request.post(`/hr/candidate/${id}/hire`, data || {})
  },
  remove(id: number | string): Promise<boolean> {
    return request.delete(`/hr/candidate/${id}`)
  },
}

// ── 枚举映射（模块内唯一真源） ──

export const GENDER_MAP: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
export const EDUCATION_MAP: Record<number, string> = { 1: '小学', 2: '初中', 3: '高中', 4: '大专', 5: '本科', 6: '硕士', 7: '博士' }
export const EMPLOYEE_TYPE_MAP: Record<number, string> = { 1: '全职', 2: '兼职', 3: '实习', 4: '外包' }
export const EMPLOYEE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '离职', color: 'error' },
  1: { text: '在职', color: 'success' },
  2: { text: '试用', color: 'warning' },
}
export const POSITION_LEVEL_MAP: Record<number, string> = { 1: '高管', 2: '中层', 3: '基层', 4: '普通' }
export const LEAVE_TYPE_MAP: Record<string, string> = { ANNUAL: '年假', SICK: '病假', PERSONAL: '事假', MATERNITY: '产假', MARRIAGE: '婚假' }
export const LEAVE_TYPE_OPTIONS = [
  { label: '年假', value: 'ANNUAL' },
  { label: '病假', value: 'SICK' },
  { label: '事假', value: 'PERSONAL' },
  { label: '产假', value: 'MATERNITY' },
  { label: '婚假', value: 'MARRIAGE' },
]
export const LEAVE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审批', color: 'warning' },
  1: { text: '已批准', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
  3: { text: '已撤销', color: 'default' },
}
export const ATTENDANCE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  NORMAL: { text: '正常', color: 'success' },
  LATE: { text: '迟到', color: 'warning' },
  EARLY: { text: '早退', color: 'orange' },
  ABSENT: { text: '缺勤', color: 'error' },
  LEAVE: { text: '休假', color: 'blue' },
}
export const PERFORMANCE_LEVEL_MAP: Record<string, { text: string; color: string }> = {
  S: { text: '优秀', color: 'success' },
  A: { text: '良好', color: 'processing' },
  B: { text: '合格', color: 'default' },
  C: { text: '待改进', color: 'warning' },
  D: { text: '不合格', color: 'error' },
}
export const PERFORMANCE_TYPE_MAP: Record<string, string> = { MONTHLY: '月度', QUARTERLY: '季度', YEARLY: '年度' }
export const PERFORMANCE_TYPE_OPTIONS = [
  { label: '月度', value: 'MONTHLY' },
  { label: '季度', value: 'QUARTERLY' },
  { label: '年度', value: 'YEARLY' },
]
export const PERFORMANCE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待考核', color: 'default' },
  1: { text: '已考核', color: 'processing' },
  2: { text: '已确认', color: 'success' },
}
export const SALARY_PAYMENT_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待发放', color: 'warning' },
  1: { text: '已发放', color: 'success' },
  2: { text: '已撤销', color: 'default' },
}
export const RESIGN_TYPE_MAP: Record<number, string> = {
  1: '主动离职', 2: '协商解除', 3: '辞退', 4: '合同到期', 5: '退休', 6: '其他',
}
export const RESIGN_TYPE_OPTIONS = Object.entries(RESIGN_TYPE_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
export const RECRUITMENT_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审批', color: 'default' },
  1: { text: '招聘中', color: 'success' },
  2: { text: '已暂停', color: 'warning' },
  3: { text: '已完成', color: 'processing' },
  4: { text: '已关闭', color: 'error' },
}
export const CANDIDATE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '简历筛选', color: 'default' },
  1: { text: '初试', color: 'processing' },
  2: { text: '复试', color: 'processing' },
  3: { text: '终面', color: 'processing' },
  4: { text: '待录用', color: 'warning' },
  5: { text: '已录用', color: 'success' },
  6: { text: '已拒绝', color: 'error' },
  7: { text: '已入职', color: 'success' },
}
export const CANDIDATE_STATUS_OPTIONS = Object.entries(CANDIDATE_STATUS_MAP).map(([k, v]) => ({ label: v.text, value: Number(k) }))
export const RECRUITMENT_CHANNEL_MAP: Record<string, string> = {
  ONLINE: '网络招聘',
  HEADHUNTER: '猎头',
  REFERRAL: '内部推荐',
  CAMPUS: '校园招聘',
  OTHER: '其他',
}
export const RECRUITMENT_CHANNEL_OPTIONS = Object.entries(RECRUITMENT_CHANNEL_MAP).map(([k, v]) => ({ label: v, value: k }))
export const URGENCY_MAP: Record<number, string> = { 1: '普通', 2: '紧急', 3: '特急' }
export const URGENCY_OPTIONS = Object.entries(URGENCY_MAP).map(([k, v]) => ({ label: v, value: Number(k) }))
export const CANDIDATE_SOURCE_MAP: Record<string, string> = RECRUITMENT_CHANNEL_MAP
