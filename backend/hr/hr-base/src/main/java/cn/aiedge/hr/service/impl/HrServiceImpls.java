package cn.aiedge.hr.service.impl;

import cn.aiedge.hr.organization.HrPosition;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.employee.HrContract;
import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.attendance.HrLeaveRequest;
import cn.aiedge.hr.salary.HrSalaryStructure;
import cn.aiedge.hr.salary.HrSalaryPayment;
import cn.aiedge.hr.performance.HrPerformance;
import cn.aiedge.hr.*;
import cn.aiedge.hr.service.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

// ── 岗位服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrPositionServiceImpl extends ServiceImpl<HrPositionMapper, HrPosition> implements HrPositionService {
    @Override
    public Page<HrPosition> pagePositions(Page<HrPosition> page, Long tenantId, Long deptId, String positionName) {
        LambdaQueryWrapper<HrPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrPosition::getTenantId, tenantId)
               .eq(deptId != null, HrPosition::getDeptId, deptId)
               .like(positionName != null, HrPosition::getPositionName, positionName)
               .orderByAsc(HrPosition::getSort);
        return page(page, wrapper);
    }
    @Override
    public List<HrPosition> getByDeptId(Long deptId) {
        return baseMapper.selectByDeptId(deptId);
    }
}

// ── 员工服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrEmployeeServiceImpl extends ServiceImpl<HrEmployeeMapper, HrEmployee> implements HrEmployeeService {
    @Override
    public Page<HrEmployee> pageEmployees(Page<HrEmployee> page, Long tenantId, Long deptId, String employeeName, Integer status) {
        LambdaQueryWrapper<HrEmployee> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrEmployee::getTenantId, tenantId)
               .eq(deptId != null, HrEmployee::getDeptId, deptId)
               .like(employeeName != null, HrEmployee::getEmployeeName, employeeName)
               .eq(status != null, HrEmployee::getStatus, status)
               .orderByDesc(HrEmployee::getCreateTime);
        return page(page, wrapper);
    }
    @Override
    public HrEmployee getByEmployeeNo(String employeeNo) {
        return baseMapper.selectByEmployeeNo(employeeNo);
    }
    @Override
    @Transactional
    public Long createEmployee(HrEmployee employee) {
        employee.setStatus(2); // 试用期
        employee.setCreateTime(LocalDateTime.now());
        save(employee);
        log.info("创建员工: employeeNo={}, name={}", employee.getEmployeeNo(), employee.getEmployeeName());
        return employee.getId();
    }
    @Override
    @Transactional
    public void updateEmployee(HrEmployee employee) {
        employee.setUpdateTime(LocalDateTime.now());
        updateById(employee);
    }
    @Override
    @Transactional
    public void updateStatus(Long id, Integer status) {
        baseMapper.updateStatus(id, status);
        if (status == 0) {
            // 离职时更新离职日期
            HrEmployee emp = new HrEmployee();
            emp.setId(id);
            emp.setLeaveDate(LocalDate.now());
            emp.setUpdateTime(LocalDateTime.now());
            updateById(emp);
        }
        log.info("更新员工状态: id={}, status={}", id, status);
    }
    @Override
    public List<HrEmployee> getByDeptId(Long deptId) {
        return baseMapper.selectByDeptId(deptId);
    }
}

// ── 合同服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrContractServiceImpl extends ServiceImpl<HrContractMapper, HrContract> implements HrContractService {
    @Override
    public List<HrContract> getByEmployeeId(Long employeeId) {
        return baseMapper.selectByEmployeeId(employeeId);
    }
    @Override
    @Transactional
    public Long createContract(HrContract contract) {
        contract.setCreateTime(LocalDateTime.now());
        contract.setStatus(1); // 生效
        save(contract);
        return contract.getId();
    }
}

// ── 考勤服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrAttendanceServiceImpl extends ServiceImpl<HrAttendanceMapper, HrAttendance> implements HrAttendanceService {
    @Override
    public Page<HrAttendance> pageAttendances(Page<HrAttendance> page, Long tenantId, Long employeeId, String month) {
        LambdaQueryWrapper<HrAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrAttendance::getTenantId, tenantId)
               .eq(employeeId != null, HrAttendance::getEmployeeId, employeeId)
               .like(month != null, HrAttendance::getAttendanceDate, month)
               .orderByDesc(HrAttendance::getAttendanceDate);
        return page(page, wrapper);
    }
    @Override
    public List<HrAttendance> getByEmployeeAndMonth(Long employeeId, String month) {
        return baseMapper.selectByEmployeeAndMonth(employeeId, month);
    }
    @Override
    @Transactional
    public void clockIn(Long employeeId) {
        HrAttendance attendance = new HrAttendance();
        attendance.setEmployeeId(employeeId);
        attendance.setAttendanceDate(LocalDate.now());
        attendance.setClockInTime(LocalTime.now());
        attendance.setStatus("NORMAL");
        attendance.setCreateTime(LocalDateTime.now());
        save(attendance);
        log.info("上班打卡: employeeId={}", employeeId);
    }
    @Override
    @Transactional
    public void clockOut(Long employeeId) {
        LambdaQueryWrapper<HrAttendance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrAttendance::getEmployeeId, employeeId)
               .eq(HrAttendance::getAttendanceDate, LocalDate.now());
        HrAttendance attendance = getOne(wrapper);
        if (attendance != null) {
            attendance.setClockOutTime(LocalTime.now());
            attendance.setUpdateTime(LocalDateTime.now());
            updateById(attendance);
            log.info("下班打卡: employeeId={}", employeeId);
        }
    }
}

// ── 请假服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrLeaveRequestServiceImpl extends ServiceImpl<HrLeaveRequestMapper, HrLeaveRequest> implements HrLeaveRequestService {
    @Override
    public Page<HrLeaveRequest> pageRequests(Page<HrLeaveRequest> page, Long tenantId, Long employeeId, Integer status) {
        LambdaQueryWrapper<HrLeaveRequest> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrLeaveRequest::getTenantId, tenantId)
               .eq(employeeId != null, HrLeaveRequest::getEmployeeId, employeeId)
               .eq(status != null, HrLeaveRequest::getStatus, status)
               .orderByDesc(HrLeaveRequest::getCreateTime);
        return page(page, wrapper);
    }
    @Override
    @Transactional
    public Long submitRequest(HrLeaveRequest request) {
        request.setStatus(0); // 待审批
        request.setCreateTime(LocalDateTime.now());
        save(request);
        log.info("提交请假申请: employeeId={}, type={}", request.getEmployeeId(), request.getLeaveType());
        return request.getId();
    }
    @Override
    @Transactional
    public void approve(Long id, Long approveId, String comment) {
        HrLeaveRequest request = new HrLeaveRequest();
        request.setId(id);
        request.setStatus(1);
        request.setApproveId(approveId);
        request.setApproveComment(comment);
        request.setApproveTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
        log.info("批准请假: id={}", id);
    }
    @Override
    @Transactional
    public void reject(Long id, Long approveId, String comment) {
        HrLeaveRequest request = new HrLeaveRequest();
        request.setId(id);
        request.setStatus(2);
        request.setApproveId(approveId);
        request.setApproveComment(comment);
        request.setApproveTime(LocalDateTime.now());
        request.setUpdateTime(LocalDateTime.now());
        updateById(request);
        log.info("拒绝请假: id={}", id);
    }
}

// ── 薪资结构服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrSalaryStructureServiceImpl extends ServiceImpl<HrSalaryStructureMapper, HrSalaryStructure> implements HrSalaryStructureService {
    @Override
    public HrSalaryStructure getEffectiveByEmployeeId(Long employeeId) {
        return baseMapper.selectEffectiveByEmployeeId(employeeId);
    }
    @Override
    @Transactional
    public Long createStructure(HrSalaryStructure structure) {
        structure.setStatus(1);
        structure.setEffectiveDate(LocalDate.now());
        structure.setCreateTime(LocalDateTime.now());
        save(structure);
        return structure.getId();
    }
}

// ── 薪资发放服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrSalaryPaymentServiceImpl extends ServiceImpl<HrSalaryPaymentMapper, HrSalaryPayment> implements HrSalaryPaymentService {
    @Override
    public Page<HrSalaryPayment> pagePayments(Page<HrSalaryPayment> page, Long tenantId, Long employeeId, String paymentMonth) {
        LambdaQueryWrapper<HrSalaryPayment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrSalaryPayment::getTenantId, tenantId)
               .eq(employeeId != null, HrSalaryPayment::getEmployeeId, employeeId)
               .eq(paymentMonth != null, HrSalaryPayment::getPaymentMonth, paymentMonth)
               .orderByDesc(HrSalaryPayment::getPaymentMonth);
        return page(page, wrapper);
    }
    @Override
    @Transactional
    public void generateMonthlyPayment(String paymentMonth) {
        // TODO: 实现薪资计算逻辑
        log.info("生成月度薪资: month={}", paymentMonth);
    }
    @Override
    @Transactional
    public void confirmPayment(Long id) {
        HrSalaryPayment payment = new HrSalaryPayment();
        payment.setId(id);
        payment.setStatus(1);
        payment.setPaymentDate(LocalDate.now());
        payment.setUpdateTime(LocalDateTime.now());
        updateById(payment);
        log.info("确认发放薪资: id={}", id);
    }
}

// ── 绩效服务实现 ──
@Slf4j
@Service
@RequiredArgsConstructor
class HrPerformanceServiceImpl extends ServiceImpl<HrPerformanceMapper, HrPerformance> implements HrPerformanceService {
    @Override
    public Page<HrPerformance> pageReviews(Page<HrPerformance> page, Long tenantId, Long employeeId, String reviewPeriod) {
        LambdaQueryWrapper<HrPerformance> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(HrPerformance::getTenantId, tenantId)
               .eq(employeeId != null, HrPerformance::getEmployeeId, employeeId)
               .eq(reviewPeriod != null, HrPerformance::getReviewPeriod, reviewPeriod)
               .orderByDesc(HrPerformance::getReviewPeriod);
        return page(page, wrapper);
    }
    @Override
    @Transactional
    public Long submitReview(HrPerformance performance) {
        performance.setStatus(1);
        performance.setReviewTime(LocalDateTime.now());
        performance.setCreateTime(LocalDateTime.now());
        save(performance);
        return performance.getId();
    }
    @Override
    @Transactional
    public void confirmReview(Long id) {
        HrPerformance perf = new HrPerformance();
        perf.setId(id);
        perf.setStatus(2);
        perf.setUpdateTime(LocalDateTime.now());
        updateById(perf);
        log.info("确认绩效考核: id={}", id);
    }
}