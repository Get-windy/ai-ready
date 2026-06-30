package cn.aiedge.hr.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.hr.organization.HrPosition;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.employee.HrContract;
import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.attendance.HrLeaveRequest;
import cn.aiedge.hr.salary.HrSalaryStructure;
import cn.aiedge.hr.salary.HrSalaryPayment;
import cn.aiedge.hr.performance.HrPerformance;
import cn.aiedge.hr.service.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 人力资源管理控制器
 * 组织架构、员工、考勤、薪酬、绩效API
 */
@Tag(name = "人力资源管理", description = "HR模块API")
@RestController
@RequestMapping("/hr")
@RequiredArgsConstructor
public class HrController {

    private final HrPositionService positionService;
    private final HrEmployeeService employeeService;
    private final HrContractService contractService;
    private final HrAttendanceService attendanceService;
    private final HrLeaveRequestService leaveRequestService;
    private final HrSalaryStructureService salaryStructureService;
    private final HrSalaryPaymentService salaryPaymentService;
    private final HrPerformanceService performanceService;

    // ── 岗位管理 ────────────────────────────────────

    @Operation(summary = "分页查询岗位")
    @GetMapping("/positions/page")
    public Result<Page<HrPosition>> pagePositions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String positionName) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrPosition> page = new Page<>(pageNum, pageSize);
        return Result.success(positionService.pagePositions(page, tenantId, deptId, positionName));
    }

    @Operation(summary = "获取岗位详情")
    @GetMapping("/positions/{id}")
    public Result<HrPosition> getPosition(@PathVariable Long id) {
        return Result.success(positionService.getById(id));
    }

    @Operation(summary = "创建岗位")
    @PostMapping("/positions")
    public Result<Long> createPosition(@RequestBody HrPosition position) {
        position.setTenantId(SecurityUtils.getCurrentTenantId());
        positionService.save(position);
        return Result.success(position.getId());
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/positions/{id}")
    public Result<Void> updatePosition(@PathVariable Long id, @RequestBody HrPosition position) {
        position.setId(id);
        positionService.updateById(position);
        return Result.success();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/positions/{id}")
    public Result<Void> deletePosition(@PathVariable Long id) {
        positionService.removeById(id);
        return Result.success();
    }

    // ── 员工管理 ────────────────────────────────────

    @Operation(summary = "分页查询员工")
    @GetMapping("/employees/page")
    public Result<Page<HrEmployee>> pageEmployees(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) Integer status) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrEmployee> page = new Page<>(pageNum, pageSize);
        return Result.success(employeeService.pageEmployees(page, tenantId, deptId, employeeName, status));
    }

    @Operation(summary = "获取员工详情")
    @GetMapping("/employees/{id}")
    public Result<HrEmployee> getEmployee(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }

    @Operation(summary = "创建员工")
    @PostMapping("/employees")
    public Result<Long> createEmployee(@RequestBody HrEmployee employee) {
        employee.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(employeeService.createEmployee(employee));
    }

    @Operation(summary = "更新员工")
    @PutMapping("/employees/{id}")
    public Result<Void> updateEmployee(@PathVariable Long id, @RequestBody HrEmployee employee) {
        employee.setId(id);
        employeeService.updateEmployee(employee);
        return Result.success();
    }

    @Operation(summary = "更新员工状态")
    @PutMapping("/employees/{id}/status")
    public Result<Void> updateEmployeeStatus(@PathVariable Long id, @RequestParam Integer status) {
        employeeService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "获取员工合同列表")
    @GetMapping("/employees/{id}/contracts")
    public Result<List<HrContract>> getEmployeeContracts(@PathVariable Long id) {
        return Result.success(contractService.getByEmployeeId(id));
    }

    // ── 考勤管理 ────────────────────────────────────

    @Operation(summary = "分页查询考勤记录")
    @GetMapping("/attendance/page")
    public Result<Page<HrAttendance>> pageAttendance(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String month) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrAttendance> page = new Page<>(pageNum, pageSize);
        return Result.success(attendanceService.pageAttendances(page, tenantId, employeeId, month));
    }

    @Operation(summary = "上班打卡")
    @PostMapping("/attendance/clock-in")
    public Result<Void> clockIn(@RequestParam Long employeeId) {
        attendanceService.clockIn(employeeId);
        return Result.success();
    }

    @Operation(summary = "下班打卡")
    @PostMapping("/attendance/clock-out")
    public Result<Void> clockOut(@RequestParam Long employeeId) {
        attendanceService.clockOut(employeeId);
        return Result.success();
    }

    // ── 请假管理 ────────────────────────────────────

    @Operation(summary = "分页查询请假申请")
    @GetMapping("/leave/page")
    public Result<Page<HrLeaveRequest>> pageLeaveRequests(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer status) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrLeaveRequest> page = new Page<>(pageNum, pageSize);
        return Result.success(leaveRequestService.pageRequests(page, tenantId, employeeId, status));
    }

    @Operation(summary = "提交请假申请")
    @PostMapping("/leave")
    public Result<Long> submitLeaveRequest(@RequestBody HrLeaveRequest request) {
        request.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(leaveRequestService.submitRequest(request));
    }

    @Operation(summary = "批准请假")
    @PutMapping("/leave/{id}/approve")
    public Result<Void> approveLeave(@PathVariable Long id, @RequestParam(required = false) String comment) {
        Long approveId = SecurityUtils.getCurrentUserId();
        leaveRequestService.approve(id, approveId, comment);
        return Result.success();
    }

    @Operation(summary = "拒绝请假")
    @PutMapping("/leave/{id}/reject")
    public Result<Void> rejectLeave(@PathVariable Long id, @RequestParam(required = false) String comment) {
        Long approveId = SecurityUtils.getCurrentUserId();
        leaveRequestService.reject(id, approveId, comment);
        return Result.success();
    }

    // ── 薪资管理 ────────────────────────────────────

    @Operation(summary = "获取员工薪资结构")
    @GetMapping("/salary/structure/{employeeId}")
    public Result<HrSalaryStructure> getSalaryStructure(@PathVariable Long employeeId) {
        return Result.success(salaryStructureService.getEffectiveByEmployeeId(employeeId));
    }

    @Operation(summary = "创建薪资结构")
    @PostMapping("/salary/structure")
    public Result<Long> createSalaryStructure(@RequestBody HrSalaryStructure structure) {
        structure.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(salaryStructureService.createStructure(structure));
    }

    @Operation(summary = "分页查询薪资发放")
    @GetMapping("/salary/payment/page")
    public Result<Page<HrSalaryPayment>> pageSalaryPayments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String paymentMonth) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrSalaryPayment> page = new Page<>(pageNum, pageSize);
        return Result.success(salaryPaymentService.pagePayments(page, tenantId, employeeId, paymentMonth));
    }

    @Operation(summary = "确认发放薪资")
    @PutMapping("/salary/payment/{id}/confirm")
    public Result<Void> confirmSalaryPayment(@PathVariable Long id) {
        salaryPaymentService.confirmPayment(id);
        return Result.success();
    }

    @Operation(summary = "生成月度薪资")
    @PostMapping("/salary/payment/generate")
    public Result<Void> generateMonthlyPayment(@RequestParam String paymentMonth) {
        salaryPaymentService.generateMonthlyPayment(paymentMonth);
        return Result.success();
    }

    // ── 绩效管理 ────────────────────────────────────

    @Operation(summary = "分页查询绩效考核")
    @GetMapping("/performance/page")
    public Result<Page<HrPerformance>> pagePerformance(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String reviewPeriod) {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        Page<HrPerformance> page = new Page<>(pageNum, pageSize);
        return Result.success(performanceService.pageReviews(page, tenantId, employeeId, reviewPeriod));
    }

    @Operation(summary = "提交绩效考核")
    @PostMapping("/performance")
    public Result<Long> submitPerformance(@RequestBody HrPerformance performance) {
        performance.setTenantId(SecurityUtils.getCurrentTenantId());
        performance.setReviewerId(SecurityUtils.getCurrentUserId());
        return Result.success(performanceService.submitReview(performance));
    }

    @Operation(summary = "确认绩效考核")
    @PutMapping("/performance/{id}/confirm")
    public Result<Void> confirmPerformance(@PathVariable Long id) {
        performanceService.confirmReview(id);
        return Result.success();
    }
}