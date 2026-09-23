package cn.aiedge.hr.service;

import cn.aiedge.hr.employee.HrEmployee;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 员工档案服务
 */
public interface HrEmployeeService extends IService<HrEmployee> {

    /** 员工台账分页查询（条件全部可选） */
    Page<HrEmployee> pageEmployees(Page<HrEmployee> page, HrEmployeeQuery query);

    /** 生成下一个工号（号段 EMP，租户内唯一） */
    String nextEmployeeNo();

    Long createEmployee(HrEmployee employee);

    void updateEmployee(HrEmployee employee);

    /** 转正：试用(2) → 在职(1)，写转正日期 */
    void regularize(Long id, LocalDate regularDate, String remark);

    /** 离职：在职(1)/试用(2) → 离职(0)，写最后工作日、离职类型与原因 */
    void resign(Long id, LocalDate lastWorkDate, Integer resignType, String resignReason);

    /** 状态流转（兼容旧端点语义）：status=1 转正、status=0 离职；非法迁移抛业务异常 */
    void updateStatus(Long id, Integer status);

    List<HrEmployee> getByDeptId(Long deptId);

    /** 删除（逻辑删） */
    void removeEmployee(Long id);

    /** 批量删除（逻辑删） */
    void batchRemove(List<Long> ids);

    /** 台账统计：在职/试用/离职/本月入职 */
    Map<String, Object> statistics(Long deptId);

    /** 人事异动记录分页（入职/转正/调岗/调薪/离职的历史留痕） */
    Page<cn.aiedge.hr.change.HrEmployeeChange> pageChanges(Page<cn.aiedge.hr.change.HrEmployeeChange> page,
                                                           Long employeeId, String changeType,
                                                           java.time.LocalDate dateFrom,
                                                           java.time.LocalDate dateTo);

    /** 员工台账查询条件（全字段可选） */
    class HrEmployeeQuery {
        private Long tenantId;
        private Long deptId;
        private Long positionId;
        /** 关键字：工号 / 姓名 / 手机号 模糊匹配 */
        private String keyword;
        private String employeeName;
        private String employeeNo;
        private String phone;
        private Integer status;
        private Integer employeeType;
        private Integer gender;
        private Integer education;
        private LocalDate hireDateStart;
        private LocalDate hireDateEnd;
        private LocalDate leaveDateStart;
        private LocalDate leaveDateEnd;
        private String sortField;
        private String sortOrder;

        public Long getTenantId() { return tenantId; }
        public HrEmployeeQuery setTenantId(Long v) { this.tenantId = v; return this; }
        public Long getDeptId() { return deptId; }
        public HrEmployeeQuery setDeptId(Long v) { this.deptId = v; return this; }
        public Long getPositionId() { return positionId; }
        public HrEmployeeQuery setPositionId(Long v) { this.positionId = v; return this; }
        public String getKeyword() { return keyword; }
        public HrEmployeeQuery setKeyword(String v) { this.keyword = v; return this; }
        public String getEmployeeName() { return employeeName; }
        public HrEmployeeQuery setEmployeeName(String v) { this.employeeName = v; return this; }
        public String getEmployeeNo() { return employeeNo; }
        public HrEmployeeQuery setEmployeeNo(String v) { this.employeeNo = v; return this; }
        public String getPhone() { return phone; }
        public HrEmployeeQuery setPhone(String v) { this.phone = v; return this; }
        public Integer getStatus() { return status; }
        public HrEmployeeQuery setStatus(Integer v) { this.status = v; return this; }
        public Integer getEmployeeType() { return employeeType; }
        public HrEmployeeQuery setEmployeeType(Integer v) { this.employeeType = v; return this; }
        public Integer getGender() { return gender; }
        public HrEmployeeQuery setGender(Integer v) { this.gender = v; return this; }
        public Integer getEducation() { return education; }
        public HrEmployeeQuery setEducation(Integer v) { this.education = v; return this; }
        public LocalDate getHireDateStart() { return hireDateStart; }
        public HrEmployeeQuery setHireDateStart(LocalDate v) { this.hireDateStart = v; return this; }
        public LocalDate getHireDateEnd() { return hireDateEnd; }
        public HrEmployeeQuery setHireDateEnd(LocalDate v) { this.hireDateEnd = v; return this; }
        public LocalDate getLeaveDateStart() { return leaveDateStart; }
        public HrEmployeeQuery setLeaveDateStart(LocalDate v) { this.leaveDateStart = v; return this; }
        public LocalDate getLeaveDateEnd() { return leaveDateEnd; }
        public HrEmployeeQuery setLeaveDateEnd(LocalDate v) { this.leaveDateEnd = v; return this; }
        public String getSortField() { return sortField; }
        public HrEmployeeQuery setSortField(String v) { this.sortField = v; return this; }
        public String getSortOrder() { return sortOrder; }
        public HrEmployeeQuery setSortOrder(String v) { this.sortOrder = v; return this; }
    }
}
