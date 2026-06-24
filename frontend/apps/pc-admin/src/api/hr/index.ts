import request from '@/utils/request'
import type { PageResult } from '@/api/common'

// ── 岗位管理 ──

export interface HrPosition {
  id: number
  tenantId: number
  deptId: number
  positionCode: string
  positionName: string
  positionLevel: number
  responsibility: string
  quotaCount: number
  currentCount: number
  sort: number
  status: number
  createTime: string
}

export const hrPositionApi = {
  page(params: { pageNum?: number; pageSize?: number; deptId?: number; positionName?: string }): Promise<PageResult<HrPosition>> {
    return request.get('/hr/positions/page', { params })
  },
  getById(id: number): Promise<HrPosition> {
    return request.get(`/hr/positions/${id}`)
  },
  create(data: Partial<HrPosition>): Promise<number> {
    return request.post('/hr/positions', data)
  },
  update(id: number, data: Partial<HrPosition>): Promise<boolean> {
    return request.put(`/hr/positions/${id}`, data)
  },
  delete(id: number): Promise<boolean> {
    return request.delete(`/hr/positions/${id}`)
  }
}

// ── 员工管理 ──

export interface HrEmployee {
  id: number
  tenantId: number
  employeeNo: string
  employeeName: string
  deptId: number
  positionId: number
  gender: number
  birthDate: string
  phone: string
  email: string
  idCard: string
  education: number
  school: string
  major: string
  hireDate: string
  leaveDate: string
  employeeType: number
  status: number
  avatarUrl: string
  emergencyContact: string
  emergencyPhone: string
  remark: string
  createTime: string
}

export const hrEmployeeApi = {
  page(params: { pageNum?: number; pageSize?: number; deptId?: number; employeeName?: string; status?: number }): Promise<PageResult<HrEmployee>> {
    return request.get('/hr/employees/page', { params })
  },
  getById(id: number): Promise<HrEmployee> {
    return request.get(`/hr/employees/${id}`)
  },
  create(data: Partial<HrEmployee>): Promise<number> {
    return request.post('/hr/employees', data)
  },
  update(id: number, data: Partial<HrEmployee>): Promise<boolean> {
    return request.put(`/hr/employees/${id}`, data)
  },
  updateStatus(id: number, status: number): Promise<boolean> {
    return request.put(`/hr/employees/${id}/status`, null, { params: { status } })
  },
  getContracts(id: number): Promise<HrContract[]> {
    return request.get(`/hr/employees/${id}/contracts`)
  }
}

// ── 合同管理 ──

export interface HrContract {
  id: number
  employeeId: number
  contractType: number
  contractNo: string
  contractName: string
  startDate: string
  endDate: string
  salaryAmount: number
  signDate: string
  status: number
}

// ── 考勤管理 ──

export interface HrAttendance {
  id: number
  employeeId: number
  attendanceDate: string
  clockInTime: string
  clockOutTime: string
  status: string
  lateMinutes: number
  earlyMinutes: number
  workHours: number
}

export const hrAttendanceApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number; month?: string }): Promise<PageResult<HrAttendance>> {
    return request.get('/hr/attendance/page', { params })
  },
  clockIn(employeeId: number): Promise<boolean> {
    return request.post('/hr/attendance/clock-in', null, { params: { employeeId } })
  },
  clockOut(employeeId: number): Promise<boolean> {
    return request.post('/hr/attendance/clock-out', null, { params: { employeeId } })
  }
}

// ── 请假管理 ──

export interface HrLeaveRequest {
  id: number
  employeeId: number
  leaveType: string
  startDate: string
  endDate: string
  days: number
  reason: string
  status: number
  approveId: number
  approveComment: string
  approveTime: string
}

export const hrLeaveRequestApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number; status?: number }): Promise<PageResult<HrLeaveRequest>> {
    return request.get('/hr/leave/page', { params })
  },
  submit(data: Partial<HrLeaveRequest>): Promise<number> {
    return request.post('/hr/leave', data)
  },
  approve(id: number, comment?: string): Promise<boolean> {
    return request.put(`/hr/leave/${id}/approve`, null, { params: { comment } })
  },
  reject(id: number, comment?: string): Promise<boolean> {
    return request.put(`/hr/leave/${id}/reject`, null, { params: { comment } })
  }
}

// ── 薪资管理 ──

export interface HrSalaryStructure {
  id: number
  employeeId: number
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
  status: number
}

export interface HrSalaryPayment {
  id: number
  employeeId: number
  paymentMonth: string
  baseAmount: number
  performanceAmount: number
  allowanceAmount: number
  overtimeAmount: number
  deductAmount: number
  socialDeduct: number
  fundDeduct: number
  taxDeduct: number
  actualAmount: number
  paymentDate: string
  status: number
}

export const hrSalaryApi = {
  getStructure(employeeId: number): Promise<HrSalaryStructure> {
    return request.get(`/hr/salary/structure/${employeeId}`)
  },
  createStructure(data: Partial<HrSalaryStructure>): Promise<number> {
    return request.post('/hr/salary/structure', data)
  },
  pagePayments(params: { pageNum?: number; pageSize?: number; employeeId?: number; paymentMonth?: string }): Promise<PageResult<HrSalaryPayment>> {
    return request.get('/hr/salary/payment/page', { params })
  },
  confirmPayment(id: number): Promise<boolean> {
    return request.put(`/hr/salary/payment/${id}/confirm`)
  }
}

// ── 绩效管理 ──

export interface HrPerformance {
  id: number
  employeeId: number
  reviewPeriod: string
  reviewType: string
  score: number
  level: string
  attitudeScore: number
  abilityScore: number
  achievementScore: number
  comment: string
  reviewerId: number
  reviewerName: string
  reviewTime: string
  status: number
}

export const hrPerformanceApi = {
  page(params: { pageNum?: number; pageSize?: number; employeeId?: number; reviewPeriod?: string }): Promise<PageResult<HrPerformance>> {
    return request.get('/hr/performance/page', { params })
  },
  submit(data: Partial<HrPerformance>): Promise<number> {
    return request.post('/hr/performance', data)
  },
  confirm(id: number): Promise<boolean> {
    return request.put(`/hr/performance/${id}/confirm`)
  }
}

// ── 枚举映射 ──

export const GENDER_MAP: Record<number, string> = { 0: '未知', 1: '男', 2: '女' }
export const EDUCATION_MAP: Record<number, string> = { 1: '小学', 2: '初中', 3: '高中', 4: '大专', 5: '本科', 6: '硕士', 7: '博士' }
export const EMPLOYEE_TYPE_MAP: Record<number, string> = { 1: '全职', 2: '兼职', 3: '实习', 4: '外包' }
export const EMPLOYEE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '离职', color: 'error' },
  1: { text: '在职', color: 'success' },
  2: { text: '试用', color: 'warning' }
}
export const POSITION_LEVEL_MAP: Record<number, string> = { 1: '高管', 2: '中层', 3: '基层', 4: '普通' }
export const LEAVE_TYPE_MAP: Record<string, string> = { ANNUAL: '年假', SICK: '病假', PERSONAL: '事假', MATERNITY: '产假', MARRIAGE: '婚假' }
export const LEAVE_STATUS_MAP: Record<number, { text: string; color: string }> = {
  0: { text: '待审批', color: 'warning' },
  1: { text: '已批准', color: 'success' },
  2: { text: '已拒绝', color: 'error' },
  3: { text: '已撤销', color: 'default' }
}
export const ATTENDANCE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  NORMAL: { text: '正常', color: 'success' },
  LATE: { text: '迟到', color: 'warning' },
  EARLY: { text: '早退', color: 'warning' },
  ABSENT: { text: '缺勤', color: 'error' }
}
export const PERFORMANCE_LEVEL_MAP: Record<string, { text: string; color: string }> = {
  S: { text: '优秀', color: 'success' },
  A: { text: '良好', color: 'processing' },
  B: { text: '合格', color: 'default' },
  C: { text: '待改进', color: 'warning' },
  D: { text: '不合格', color: 'error' }
}