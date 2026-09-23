package cn.aiedge.hr.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.common.permission.RequiresPermission;
import cn.aiedge.hr.attendance.HrAttendance;
import cn.aiedge.hr.attendance.HrAttendanceRule;
import cn.aiedge.hr.attendance.HrLeaveQuota;
import cn.aiedge.hr.attendance.HrLeaveRequest;
import cn.aiedge.hr.change.HrEmployeeChange;
import cn.aiedge.hr.employee.HrContract;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.organization.HrPosition;
import cn.aiedge.hr.performance.HrPerformance;
import cn.aiedge.hr.salary.HrSalaryPayment;
import cn.aiedge.hr.salary.HrSalaryStructure;
import cn.aiedge.hr.service.*;
import cn.dev33.satoken.annotation.SaCheckLogin;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 人力资源管理控制器
 *
 * <p><b>路径约定</b>：类级前缀必须是 <code>/api/hr</code>（与 `HrRecruitmentController`、
 * `HrCandidateController` 一致）。前端 axios 的 `baseURL = '/api'` 且 vite proxy 无 rewrite，
 * 故此前写成裸前缀 `/hr` 会让本控制器下**全部端点 404**，整个 HR 模块在浏览器里不可用。</p>
 *
 * <p><b>鉴权</b>：类级 `@SaCheckLogin` + 方法级 `@RequiresPermission("hr:xxx")`。
 * 薪资相关端点单独用 `hr:salary:*` 收口——**薪资保密是 HR 系统的基本要求**，
 * 不能像修复前那样「任何登录用户都能拉全员工资」。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "人力资源管理", description = "HR模块API：岗位/员工/合同/考勤/请假/薪资/绩效")
@RestController
@RequestMapping("/api/hr")
@RequiredArgsConstructor
@SaCheckLogin
public class HrController {

    private final HrPositionService positionService;
    private final HrEmployeeService employeeService;
    private final HrContractService contractService;
    private final HrAttendanceService attendanceService;
    private final HrLeaveRequestService leaveRequestService;
    private final HrSalaryStructureService salaryStructureService;
    private final HrSalaryPaymentService salaryPaymentService;
    private final HrPerformanceService performanceService;

    // ══════════════════ 岗位管理 ══════════════════

    @Operation(summary = "分页查询岗位")
    @GetMapping("/positions/page")
    @RequiresPermission("hr:position:list")
    public Result<Page<HrPosition>> pagePositions(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String positionName,
            @RequestParam(required = false) String positionCode,
            @RequestParam(required = false) Integer positionLevel,
            @RequestParam(required = false) Integer status) {
        Page<HrPosition> page = new Page<>(pageNum, pageSize);
        return Result.success(positionService.pagePositions(page, SecurityUtils.getCurrentTenantId(),
                deptId, positionName, positionCode, positionLevel, status));
    }

    @Operation(summary = "岗位下拉列表（仅启用）")
    @GetMapping("/positions/list")
    @RequiresPermission("hr:position:list")
    public Result<List<HrPosition>> listPositions() {
        return Result.success(positionService.listEnabled());
    }

    @Operation(summary = "按部门查询岗位")
    @GetMapping("/positions/by-dept/{deptId}")
    @RequiresPermission("hr:position:list")
    public Result<List<HrPosition>> listPositionsByDept(@PathVariable Long deptId) {
        return Result.success(positionService.getByDeptId(deptId));
    }

    @Operation(summary = "获取岗位详情")
    @GetMapping("/positions/{id}")
    @RequiresPermission("hr:position:list")
    public Result<HrPosition> getPosition(@PathVariable Long id) {
        return Result.success(positionService.getById(id));
    }

    @Operation(summary = "生成岗位编码")
    @GetMapping("/positions/next-code")
    @RequiresPermission("hr:position:create")
    public Result<String> nextPositionCode() {
        return Result.success(positionService.nextPositionCode());
    }

    @Operation(summary = "创建岗位")
    @PostMapping("/positions")
    @RequiresPermission("hr:position:create")
    public Result<Long> createPosition(@RequestBody HrPosition position) {
        position.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(positionService.createPosition(position));
    }

    @Operation(summary = "更新岗位")
    @PutMapping("/positions/{id}")
    @RequiresPermission("hr:position:update")
    public Result<Void> updatePosition(@PathVariable Long id, @RequestBody HrPosition position) {
        position.setId(id);
        positionService.updatePosition(position);
        return Result.success();
    }

    @Operation(summary = "删除岗位")
    @DeleteMapping("/positions/{id}")
    @RequiresPermission("hr:position:delete")
    public Result<Void> deletePosition(@PathVariable Long id) {
        positionService.deletePosition(id);
        return Result.success();
    }

    @Operation(summary = "岗位统计（编制/在岗/超编）")
    @GetMapping("/positions/stat")
    @RequiresPermission("hr:position:list")
    public Result<Map<String, Object>> positionStat(@RequestParam(required = false) Long deptId) {
        return Result.success(positionService.statistics(deptId));
    }

    // ══════════════════ 员工管理 ══════════════════

    @Operation(summary = "分页查询员工")
    @GetMapping("/employees/page")
    @RequiresPermission("hr:employee:list")
    public Result<Page<HrEmployee>> pageEmployees(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long positionId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String employeeName,
            @RequestParam(required = false) String employeeNo,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer employeeType,
            @RequestParam(required = false) Integer gender,
            @RequestParam(required = false) Integer education,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hireDateEnd,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveDateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate leaveDateEnd,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        HrEmployeeService.HrEmployeeQuery query = new HrEmployeeService.HrEmployeeQuery()
                .setTenantId(SecurityUtils.getCurrentTenantId())
                .setDeptId(deptId).setPositionId(positionId).setKeyword(keyword)
                .setEmployeeName(employeeName).setEmployeeNo(employeeNo).setPhone(phone)
                .setStatus(status).setEmployeeType(employeeType).setGender(gender).setEducation(education)
                .setHireDateStart(hireDateStart).setHireDateEnd(hireDateEnd)
                .setLeaveDateStart(leaveDateStart).setLeaveDateEnd(leaveDateEnd)
                .setSortField(sortField).setSortOrder(sortOrder);
        Page<HrEmployee> page = new Page<>(pageNum, pageSize);
        return Result.success(employeeService.pageEmployees(page, query));
    }

    @Operation(summary = "生成员工工号")
    @GetMapping("/employees/next-no")
    @RequiresPermission("hr:employee:create")
    public Result<String> nextEmployeeNo() {
        return Result.success(employeeService.nextEmployeeNo());
    }

    @Operation(summary = "获取员工详情")
    @GetMapping("/employees/{id}")
    @RequiresPermission("hr:employee:list")
    public Result<HrEmployee> getEmployee(@PathVariable Long id) {
        return Result.success(employeeService.getById(id));
    }

    @Operation(summary = "创建员工")
    @PostMapping("/employees")
    @RequiresPermission("hr:employee:create")
    public Result<Long> createEmployee(@RequestBody HrEmployee employee) {
        employee.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(employeeService.createEmployee(employee));
    }

    @Operation(summary = "更新员工")
    @PutMapping("/employees/{id}")
    @RequiresPermission("hr:employee:update")
    public Result<Void> updateEmployee(@PathVariable Long id, @RequestBody HrEmployee employee) {
        employee.setId(id);
        employeeService.updateEmployee(employee);
        return Result.success();
    }

    @Operation(summary = "删除员工")
    @DeleteMapping("/employees/{id}")
    @RequiresPermission("hr:employee:delete")
    public Result<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.removeEmployee(id);
        return Result.success();
    }

    @Operation(summary = "批量删除员工")
    @DeleteMapping("/employees/batch")
    @RequiresPermission("hr:employee:delete")
    public Result<Void> batchDeleteEmployees(@RequestBody List<Long> ids) {
        employeeService.batchRemove(ids);
        return Result.success();
    }

    @Operation(summary = "更新员工状态（转正/离职）")
    @PutMapping("/employees/{id}/status")
    @RequiresPermission("hr:employee:status")
    public Result<Void> updateEmployeeStatus(@PathVariable Long id, @RequestParam Integer status) {
        employeeService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "员工转正")
    @PostMapping("/employees/{id}/regularize")
    @RequiresPermission("hr:employee:status")
    public Result<Void> regularizeEmployee(@PathVariable Long id,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate regularDate,
                                           @RequestParam(required = false) String remark) {
        employeeService.regularize(id, regularDate, remark);
        return Result.success();
    }

    @Operation(summary = "员工离职")
    @PostMapping("/employees/{id}/resign")
    @RequiresPermission("hr:employee:status")
    public Result<Void> resignEmployee(@PathVariable Long id,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate lastWorkDate,
                                       @RequestParam(required = false) Integer resignType,
                                       @RequestParam(required = false) String resignReason) {
        employeeService.resign(id, lastWorkDate, resignType, resignReason);
        return Result.success();
    }

    @Operation(summary = "员工台账统计")
    @GetMapping("/employees/stat")
    @RequiresPermission("hr:employee:list")
    public Result<Map<String, Object>> employeeStat(@RequestParam(required = false) Long deptId) {
        return Result.success(employeeService.statistics(deptId));
    }

    @Operation(summary = "人事异动记录")
    @GetMapping("/employee-changes/page")
    @RequiresPermission("hr:employee:list")
    public Result<Page<HrEmployeeChange>> pageEmployeeChanges(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String changeType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        Page<HrEmployeeChange> page = new Page<>(pageNum, pageSize);
        return Result.success(employeeService.pageChanges(page, employeeId, changeType, dateFrom, dateTo));
    }

    // ══════════════════ 合同管理 ══════════════════

    @Operation(summary = "分页查询劳动合同")
    @GetMapping("/contracts/page")
    @RequiresPermission("hr:contract:list")
    public Result<Page<HrContract>> pageContracts(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) Integer contractType,
            @RequestParam(required = false) Integer status) {
        Page<HrContract> page = new Page<>(pageNum, pageSize);
        return Result.success(contractService.pageContracts(page, SecurityUtils.getCurrentTenantId(),
                employeeId, contractNo, contractType, status));
    }

    @Operation(summary = "获取员工合同列表")
    @GetMapping("/employees/{id}/contracts")
    @RequiresPermission("hr:contract:list")
    public Result<List<HrContract>> getEmployeeContracts(@PathVariable Long id) {
        return Result.success(contractService.getByEmployeeId(id));
    }

    @Operation(summary = "生成合同编号")
    @GetMapping("/contracts/next-no")
    @RequiresPermission("hr:contract:create")
    public Result<String> nextContractNo() {
        return Result.success(contractService.nextContractNo());
    }

    @Operation(summary = "即将到期合同")
    @GetMapping("/contracts/expiring")
    @RequiresPermission("hr:contract:list")
    public Result<List<HrContract>> listExpiringContracts(@RequestParam(defaultValue = "30") Integer days) {
        return Result.success(contractService.listExpiring(days));
    }

    @Operation(summary = "创建劳动合同")
    @PostMapping("/contracts")
    @RequiresPermission("hr:contract:create")
    public Result<Long> createContract(@RequestBody HrContract contract) {
        return Result.success(contractService.createContract(contract));
    }

    @Operation(summary = "更新劳动合同")
    @PutMapping("/contracts/{id}")
    @RequiresPermission("hr:contract:update")
    public Result<Void> updateContract(@PathVariable Long id, @RequestBody HrContract contract) {
        contract.setId(id);
        contractService.updateContract(contract);
        return Result.success();
    }

    @Operation(summary = "变更合同状态")
    @PutMapping("/contracts/{id}/status")
    @RequiresPermission("hr:contract:update")
    public Result<Void> updateContractStatus(@PathVariable Long id, @RequestParam Integer status) {
        contractService.updateContractStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除劳动合同")
    @DeleteMapping("/contracts/{id}")
    @RequiresPermission("hr:contract:delete")
    public Result<Void> deleteContract(@PathVariable Long id) {
        contractService.deleteContract(id);
        return Result.success();
    }

    // ══════════════════ 考勤管理 ══════════════════

    @Operation(summary = "分页查询考勤记录")
    @GetMapping("/attendance/page")
    @RequiresPermission("hr:attendance:list")
    public Result<Page<HrAttendance>> pageAttendance(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateStart,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateEnd) {
        Page<HrAttendance> page = new Page<>(pageNum, pageSize);
        return Result.success(attendanceService.pageAttendances(page, SecurityUtils.getCurrentTenantId(),
                employeeId, month, status, deptId, dateStart, dateEnd));
    }

    @Operation(summary = "获取考勤详情")
    @GetMapping("/attendance/{id}")
    @RequiresPermission("hr:attendance:list")
    public Result<HrAttendance> getAttendance(@PathVariable Long id) {
        return Result.success(attendanceService.getById(id));
    }

    @Operation(summary = "上班打卡")
    @PostMapping("/attendance/clock-in")
    @RequiresPermission("hr:attendance:clock")
    public Result<Void> clockIn(@RequestParam Long employeeId) {
        attendanceService.clockIn(employeeId);
        return Result.success();
    }

    @Operation(summary = "下班打卡")
    @PostMapping("/attendance/clock-out")
    @RequiresPermission("hr:attendance:clock")
    public Result<Void> clockOut(@RequestParam Long employeeId) {
        attendanceService.clockOut(employeeId);
        return Result.success();
    }

    @Operation(summary = "创建考勤记录（补卡/更正）")
    @PostMapping("/attendance")
    @RequiresPermission("hr:attendance:update")
    public Result<Long> createAttendance(@RequestBody HrAttendance attendance) {
        return Result.success(attendanceService.createAttendance(attendance));
    }

    @Operation(summary = "修改考勤记录")
    @PutMapping("/attendance/{id}")
    @RequiresPermission("hr:attendance:update")
    public Result<Void> updateAttendance(@PathVariable Long id, @RequestBody HrAttendance attendance) {
        attendance.setId(id);
        attendanceService.updateAttendance(attendance);
        return Result.success();
    }

    @Operation(summary = "重算考勤（迟到/早退/工时）")
    @PutMapping("/attendance/{id}/recalculate")
    @RequiresPermission("hr:attendance:update")
    public Result<Void> recalculateAttendance(@PathVariable Long id) {
        attendanceService.recalculate(id);
        return Result.success();
    }

    @Operation(summary = "删除考勤记录")
    @DeleteMapping("/attendance/{id}")
    @RequiresPermission("hr:attendance:delete")
    public Result<Void> deleteAttendance(@PathVariable Long id) {
        attendanceService.deleteAttendance(id);
        return Result.success();
    }

    @Operation(summary = "批量删除考勤记录")
    @DeleteMapping("/attendance/batch")
    @RequiresPermission("hr:attendance:delete")
    public Result<Void> batchDeleteAttendance(@RequestBody List<Long> ids) {
        attendanceService.batchDelete(ids);
        return Result.success();
    }

    @Operation(summary = "考勤统计")
    @GetMapping("/attendance/stat")
    @RequiresPermission("hr:attendance:list")
    public Result<Map<String, Object>> attendanceStat(
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Long employeeId) {
        return Result.success(attendanceService.statistics(month, deptId, employeeId));
    }

    @Operation(summary = "读取考勤规则")
    @GetMapping("/attendance/rule")
    @RequiresPermission("hr:attendance:list")
    public Result<HrAttendanceRule> getAttendanceRule() {
        return Result.success(attendanceService.getRule());
    }

    @Operation(summary = "保存考勤规则")
    @PutMapping("/attendance/rule")
    @RequiresPermission("hr:attendance:rule")
    public Result<HrAttendanceRule> saveAttendanceRule(@RequestBody HrAttendanceRule rule) {
        return Result.success(attendanceService.saveRule(rule));
    }

    // ══════════════════ 请假管理 ══════════════════

    @Operation(summary = "分页查询请假申请")
    @GetMapping("/leave/page")
    @RequiresPermission("hr:leave:list")
    public Result<Page<HrLeaveRequest>> pageLeaveRequests(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String leaveType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDateTo) {
        Page<HrLeaveRequest> page = new Page<>(pageNum, pageSize);
        return Result.success(leaveRequestService.pageRequests(page, SecurityUtils.getCurrentTenantId(),
                employeeId, status, leaveType, deptId, startDateFrom, startDateTo));
    }

    @Operation(summary = "获取请假详情")
    @GetMapping("/leave/{id}")
    @RequiresPermission("hr:leave:list")
    public Result<HrLeaveRequest> getLeaveRequest(@PathVariable Long id) {
        return Result.success(leaveRequestService.getById(id));
    }

    @Operation(summary = "按员工查询请假记录")
    @GetMapping("/leave/by-employee/{employeeId}")
    @RequiresPermission("hr:leave:list")
    public Result<List<HrLeaveRequest>> listLeaveByEmployee(@PathVariable Long employeeId) {
        return Result.success(leaveRequestService.listByEmployee(employeeId));
    }

    @Operation(summary = "提交请假申请")
    @PostMapping("/leave")
    @RequiresPermission("hr:leave:create")
    public Result<Long> submitLeaveRequest(@RequestBody HrLeaveRequest request) {
        request.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(leaveRequestService.submitRequest(request));
    }

    @Operation(summary = "修改请假申请")
    @PutMapping("/leave/{id}")
    @RequiresPermission("hr:leave:update")
    public Result<Void> updateLeaveRequest(@PathVariable Long id, @RequestBody HrLeaveRequest request) {
        request.setId(id);
        leaveRequestService.updateRequest(request);
        return Result.success();
    }

    @Operation(summary = "批准请假")
    @PutMapping("/leave/{id}/approve")
    @RequiresPermission("hr:leave:approve")
    public Result<Void> approveLeave(@PathVariable Long id, @RequestParam(required = false) String comment) {
        leaveRequestService.approve(id, SecurityUtils.getCurrentUserId(), comment);
        return Result.success();
    }

    @Operation(summary = "拒绝请假")
    @PutMapping("/leave/{id}/reject")
    @RequiresPermission("hr:leave:approve")
    public Result<Void> rejectLeave(@PathVariable Long id, @RequestParam(required = false) String comment) {
        leaveRequestService.reject(id, SecurityUtils.getCurrentUserId(), comment);
        return Result.success();
    }

    @Operation(summary = "撤销请假")
    @PutMapping("/leave/{id}/cancel")
    @RequiresPermission("hr:leave:update")
    public Result<Void> cancelLeave(@PathVariable Long id) {
        leaveRequestService.cancel(id);
        return Result.success();
    }

    @Operation(summary = "删除请假单")
    @DeleteMapping("/leave/{id}")
    @RequiresPermission("hr:leave:delete")
    public Result<Void> deleteLeave(@PathVariable Long id) {
        leaveRequestService.deleteRequest(id);
        return Result.success();
    }

    @Operation(summary = "假期余额")
    @GetMapping("/leave/balance")
    @RequiresPermission("hr:leave:list")
    public Result<Map<String, Object>> leaveBalance(@RequestParam Long employeeId,
                                                    @RequestParam(required = false) Integer year) {
        return Result.success(leaveRequestService.balance(employeeId, year));
    }

    @Operation(summary = "假期额度清单")
    @GetMapping("/leave/quota")
    @RequiresPermission("hr:leave:list")
    public Result<List<HrLeaveQuota>> listLeaveQuota(@RequestParam(required = false) Integer year) {
        return Result.success(leaveRequestService.listQuotas(year));
    }

    @Operation(summary = "保存假期额度")
    @PutMapping("/leave/quota")
    @RequiresPermission("hr:leave:quota")
    public Result<HrLeaveQuota> saveLeaveQuota(@RequestBody HrLeaveQuota quota) {
        return Result.success(leaveRequestService.saveQuota(quota));
    }

    @Operation(summary = "请假统计")
    @GetMapping("/leave/stat")
    @RequiresPermission("hr:leave:list")
    public Result<Map<String, Object>> leaveStat(@RequestParam(required = false) Integer year,
                                                 @RequestParam(required = false) Long deptId) {
        return Result.success(leaveRequestService.statistics(year, deptId));
    }

    // ══════════════════ 薪资管理 ══════════════════

    @Operation(summary = "分页查询薪资结构")
    @GetMapping("/salary/structure/page")
    @RequiresPermission("hr:salary:list")
    public Result<Page<HrSalaryStructure>> pageSalaryStructures(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Integer status) {
        Page<HrSalaryStructure> page = new Page<>(pageNum, pageSize);
        return Result.success(salaryStructureService.pageStructures(page,
                SecurityUtils.getCurrentTenantId(), employeeId, status));
    }

    @Operation(summary = "获取员工薪资结构（生效中）")
    @GetMapping("/salary/structure/{employeeId}")
    @RequiresPermission("hr:salary:list")
    public Result<HrSalaryStructure> getSalaryStructure(@PathVariable Long employeeId) {
        return Result.success(salaryStructureService.getEffectiveByEmployeeId(employeeId));
    }

    @Operation(summary = "按员工查询薪资结构历史")
    @GetMapping("/salary/structure/by-employee/{employeeId}")
    @RequiresPermission("hr:salary:list")
    public Result<List<HrSalaryStructure>> listSalaryStructures(@PathVariable Long employeeId) {
        return Result.success(salaryStructureService.listByEmployee(employeeId));
    }

    @Operation(summary = "创建薪资结构")
    @PostMapping("/salary/structure")
    @RequiresPermission("hr:salary:update")
    public Result<Long> createSalaryStructure(@RequestBody HrSalaryStructure structure) {
        structure.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(salaryStructureService.createStructure(structure));
    }

    @Operation(summary = "修改薪资结构")
    @PutMapping("/salary/structure/{id}")
    @RequiresPermission("hr:salary:update")
    public Result<Void> updateSalaryStructure(@PathVariable Long id, @RequestBody HrSalaryStructure structure) {
        structure.setId(id);
        salaryStructureService.updateStructure(structure);
        return Result.success();
    }

    @Operation(summary = "删除薪资结构")
    @DeleteMapping("/salary/structure/{id}")
    @RequiresPermission("hr:salary:delete")
    public Result<Void> deleteSalaryStructure(@PathVariable Long id) {
        salaryStructureService.deleteStructure(id);
        return Result.success();
    }

    @Operation(summary = "分页查询薪资发放")
    @GetMapping("/salary/payment/page")
    @RequiresPermission("hr:salary:list")
    public Result<Page<HrSalaryPayment>> pageSalaryPayments(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String paymentMonth,
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) String keyword) {
        Page<HrSalaryPayment> page = new Page<>(pageNum, pageSize);
        return Result.success(salaryPaymentService.pagePayments(page, SecurityUtils.getCurrentTenantId(),
                employeeId, paymentMonth, status, deptId, keyword));
    }

    @Operation(summary = "获取薪资发放详情（工资条）")
    @GetMapping("/salary/payment/{id}")
    @RequiresPermission("hr:salary:list")
    public Result<Map<String, Object>> getSalaryPayment(@PathVariable Long id) {
        return Result.success(salaryPaymentService.payslip(id));
    }

    @Operation(summary = "确认发放薪资")
    @PutMapping("/salary/payment/{id}/confirm")
    @RequiresPermission("hr:salary:confirm")
    public Result<Void> confirmSalaryPayment(@PathVariable Long id) {
        salaryPaymentService.confirmPayment(id);
        return Result.success();
    }

    @Operation(summary = "批量确认发放薪资")
    @PutMapping("/salary/payment/batch-confirm")
    @RequiresPermission("hr:salary:confirm")
    public Result<Void> batchConfirmSalaryPayment(@RequestBody List<Long> ids) {
        salaryPaymentService.batchConfirm(ids);
        return Result.success();
    }

    @Operation(summary = "撤销薪资发放")
    @PutMapping("/salary/payment/{id}/revoke")
    @RequiresPermission("hr:salary:confirm")
    public Result<Void> revokeSalaryPayment(@PathVariable Long id) {
        salaryPaymentService.revokePayment(id);
        return Result.success();
    }

    @Operation(summary = "删除薪资记录")
    @DeleteMapping("/salary/payment/{id}")
    @RequiresPermission("hr:salary:delete")
    public Result<Void> deleteSalaryPayment(@PathVariable Long id) {
        salaryPaymentService.deletePayment(id);
        return Result.success();
    }

    @Operation(summary = "生成月度薪资")
    @PostMapping("/salary/payment/generate")
    @RequiresPermission("hr:salary:generate")
    public Result<Map<String, Object>> generateMonthlyPayment(@RequestParam String paymentMonth) {
        return Result.success(salaryPaymentService.generateMonthlyPayment(paymentMonth));
    }

    @Operation(summary = "薪资统计")
    @GetMapping("/salary/payment/stat")
    @RequiresPermission("hr:salary:list")
    public Result<Map<String, Object>> salaryStat(@RequestParam(required = false) String paymentMonth,
                                                  @RequestParam(required = false) Long deptId) {
        return Result.success(salaryPaymentService.statistics(paymentMonth, deptId));
    }

    // ══════════════════ 绩效管理 ══════════════════

    @Operation(summary = "分页查询绩效考核")
    @GetMapping("/performance/page")
    @RequiresPermission("hr:performance:list")
    public Result<Page<HrPerformance>> pagePerformance(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String reviewPeriod,
            @RequestParam(required = false) String reviewType,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer status) {
        Page<HrPerformance> page = new Page<>(pageNum, pageSize);
        return Result.success(performanceService.pageReviews(page, SecurityUtils.getCurrentTenantId(),
                employeeId, reviewPeriod, reviewType, level, status, deptId));
    }

    @Operation(summary = "获取绩效考核详情")
    @GetMapping("/performance/{id}")
    @RequiresPermission("hr:performance:list")
    public Result<HrPerformance> getPerformance(@PathVariable Long id) {
        return Result.success(performanceService.getById(id));
    }

    @Operation(summary = "提交绩效考核")
    @PostMapping("/performance")
    @RequiresPermission("hr:performance:update")
    public Result<Long> submitPerformance(@RequestBody HrPerformance performance) {
        performance.setTenantId(SecurityUtils.getCurrentTenantId());
        return Result.success(performanceService.submitReview(performance));
    }

    @Operation(summary = "修改绩效考核")
    @PutMapping("/performance/{id}")
    @RequiresPermission("hr:performance:update")
    public Result<Void> updatePerformance(@PathVariable Long id, @RequestBody HrPerformance performance) {
        performance.setId(id);
        performanceService.updateReview(performance);
        return Result.success();
    }

    @Operation(summary = "确认绩效考核")
    @PutMapping("/performance/{id}/confirm")
    @RequiresPermission("hr:performance:confirm")
    public Result<Void> confirmPerformance(@PathVariable Long id) {
        performanceService.confirmReview(id);
        return Result.success();
    }

    @Operation(summary = "删除绩效考核")
    @DeleteMapping("/performance/{id}")
    @RequiresPermission("hr:performance:delete")
    public Result<Void> deletePerformance(@PathVariable Long id) {
        performanceService.deleteReview(id);
        return Result.success();
    }

    @Operation(summary = "绩效统计")
    @GetMapping("/performance/stat")
    @RequiresPermission("hr:performance:list")
    public Result<Map<String, Object>> performanceStat(@RequestParam(required = false) String reviewPeriod,
                                                       @RequestParam(required = false) Long deptId) {
        return Result.success(performanceService.statistics(reviewPeriod, deptId));
    }
}
