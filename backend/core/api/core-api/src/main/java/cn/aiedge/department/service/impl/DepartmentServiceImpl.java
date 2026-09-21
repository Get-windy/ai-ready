package cn.aiedge.department.service.impl;

import cn.aiedge.base.entity.Department;
import cn.aiedge.base.entity.SysUser;
import cn.aiedge.base.mapper.DepartmentMapper;
import cn.aiedge.base.mapper.SysUserMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.department.dto.DepartmentQueryRequest;
import cn.aiedge.department.dto.DepartmentVO;
import cn.aiedge.department.service.DepartmentService;
import cn.aiedge.hr.employee.HrEmployee;
import cn.aiedge.hr.mapper.HrEmployeeMapper;
import cn.aiedge.position.entity.Position;
import cn.aiedge.position.mapper.PositionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 部门服务实现
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    /**
     * 根级部门的祖级路径（与建表默认值、DEPT_CHILD 数据权限解析口径保持一致）
     */
    private static final String ROOT_ANCESTORS = "0";

    /** 用户表 Mapper：删除部门前校验 sys_user.dept_id 引用（不新增自定义 Mapper 方法） */
    private final SysUserMapper sysUserMapper;

    /** 岗位 Mapper：删除部门前校验 sys_position.dept_id 引用 */
    private final PositionMapper positionMapper;

    /** 员工档案 Mapper：删除部门前校验 hr_employee.dept_id 引用 */
    private final HrEmployeeMapper hrEmployeeMapper;

    @Override
    public PageResult<DepartmentVO> pageList(DepartmentQueryRequest request) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getDepartmentCode())) {
            wrapper.like(Department::getDeptCode, request.getDepartmentCode());
        }
        if (StringUtils.hasText(request.getDepartmentName())) {
            wrapper.like(Department::getDeptName, request.getDepartmentName());
        }
        if (request.getParentId() != null) {
            wrapper.eq(Department::getParentId, request.getParentId());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Department::getStatus, request.getStatus());
        }

        wrapper.orderByAsc(Department::getSort);

        Page<Department> page = new Page<>(request.getPageNum(), request.getPageSize());
        Page<Department> result = this.page(page, wrapper);

        List<DepartmentVO> voList = result.getRecords().stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());

        return PageResult.of(voList, result.getTotal(), request.getPageNum(), request.getPageSize());
    }

    @Override
    public List<DepartmentVO> listAll() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Department::getStatus, 1).orderByAsc(Department::getSort);

        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentVO> listOptions() {
        // 下拉数据源：不做 status 过滤，返回全部未删除部门（含禁用）。
        // 存量种子部门 status 全为 0，若沿用 listAll() 的 status=1 过滤，全站部门下拉必然为空。
        // 本表有 tenant_id 且不在 IGNORE_TENANT_TABLES 内 → 多租户拦截器自动注入 tenant_id；
        // @TableLogic 会自动追加 deleted 条件，无需手写。
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Department::getSort).orderByAsc(Department::getId);

        return this.list(wrapper).stream()
            .map(this::convertToVO)
            .collect(Collectors.toList());
    }

    @Override
    public List<DepartmentVO> getTree() {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Department::getSort);

        List<Department> all = this.list(wrapper);

        Map<Long, List<Department>> groupByParent = all.stream()
            .collect(Collectors.groupingBy(d -> d.getParentId() != null ? d.getParentId() : 0L));

        return buildTree(0L, groupByParent);
    }

    @Override
    public DepartmentVO getDetail(Long id) {
        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }
        return convertToVO(dept);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(DepartmentVO request) {
        // 检查编码唯一性
        if (StringUtils.hasText(request.getDepartmentCode())) {
            LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Department::getDeptCode, request.getDepartmentCode());
            if (this.count(wrapper) > 0) {
                throw BusinessException.badRequest("部门编码已存在");
            }
        }

        Department dept = new Department();
        dept.setDeptCode(request.getDepartmentCode());
        dept.setDeptName(request.getDepartmentName());
        dept.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        dept.setLeaderId(request.getLeaderId());
        dept.setLeaderName(request.getLeaderName());
        dept.setPhone(request.getPhone());
        dept.setEmail(request.getEmail());
        dept.setSort(request.getSort() != null ? request.getSort() : 0);
        dept.setStatus(request.getStatus() != null ? request.getStatus() : 1);
        dept.setRemark(request.getDescription());

        // 设置祖级路径（与 update / move 共用同一套计算，避免三处各写各的导致口径不一致）
        dept.setAncestors(calcAncestors(dept.getParentId()));

        this.save(dept);
        log.info("创建部门成功: id={}, name={}", dept.getId(), dept.getDeptName());

        return dept.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, DepartmentVO request) {
        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        // 检查编码唯一性（排除自身）
        if (StringUtils.hasText(request.getDepartmentCode())) {
            LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(Department::getDeptCode, request.getDepartmentCode())
                   .ne(Department::getId, id);
            if (this.count(wrapper) > 0) {
                throw BusinessException.badRequest("部门编码已存在");
            }
            dept.setDeptCode(request.getDepartmentCode());
        }

        if (StringUtils.hasText(request.getDepartmentName())) {
            dept.setDeptName(request.getDepartmentName());
        }
        // 上级是否变化：变化时必须重算 ancestors（含全部后代），否则层级路径与真实层级
        // 永久失真，而 ancestors 直接决定全站 DEPT_CHILD 数据权限范围。
        boolean parentChanged = false;
        if (request.getParentId() != null) {
            parentChanged = !Objects.equals(normalizeParentId(dept.getParentId()),
                                            normalizeParentId(request.getParentId()));
            if (parentChanged) {
                // 防环校验：新上级不能是自己，也不能是自己的后代（与 move 共用同一套判定）
                checkParentCycle(id, request.getParentId());
                dept.setParentId(normalizeParentId(request.getParentId()));
                // ancestors 为空/NULL 时按 "0" 处理，不会拼出 "null,9001" 这类脏值
                dept.setAncestors(calcAncestors(dept.getParentId()));
            }
        }
        if (request.getLeaderId() != null) {
            dept.setLeaderId(request.getLeaderId());
        }
        if (StringUtils.hasText(request.getLeaderName())) {
            dept.setLeaderName(request.getLeaderName());
        }
        if (StringUtils.hasText(request.getPhone())) {
            dept.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getEmail())) {
            dept.setEmail(request.getEmail());
        }
        if (request.getSort() != null) {
            dept.setSort(request.getSort());
        }
        if (request.getStatus() != null) {
            dept.setStatus(request.getStatus());
        }
        dept.setRemark(request.getDescription());

        this.updateById(dept);

        // 自身层级变了 → 必须递归重算全部后代的 ancestors（只改自己会让子树路径失真）
        if (parentChanged) {
            refreshDescendantAncestors(id, dept.getAncestors(), loadChildrenMap());
        }

        log.info("更新部门成功: id={}, parentChanged={}", dept.getId(), parentChanged);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        // 检查是否有子部门
        LambdaQueryWrapper<Department> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Department::getParentId, id);
        if (this.count(childWrapper) > 0) {
            throw BusinessException.badRequest("存在子部门，无法删除");
        }

        // 引用校验：sys_user.dept_id / sys_position.dept_id / hr_employee.dept_id，
        // 任一大于 0 即拒绝，避免删除后产生孤儿引用。
        // 说明：三张表都含 tenant_id 且均不在 MyBatisPlusConfig.IGNORE_TENANT_TABLES 内
        //      → 多租户拦截器自动注入 tenant_id 条件；且都有 @TableLogic deleted
        //      → 逻辑删除行不会被统计。此处只用 LambdaQueryWrapper，不新增自定义 Mapper 方法。
        long userCount = sysUserMapper.selectCount(
            new LambdaQueryWrapper<SysUser>().eq(SysUser::getDeptId, id));
        long positionCount = positionMapper.selectCount(
            new LambdaQueryWrapper<Position>().eq(Position::getDeptId, id));
        long employeeCount = hrEmployeeMapper.selectCount(
            new LambdaQueryWrapper<HrEmployee>().eq(HrEmployee::getDeptId, id));

        if (userCount > 0 || positionCount > 0 || employeeCount > 0) {
            // 明确写出是哪一类引用、各多少条
            List<String> refs = new ArrayList<>();
            if (userCount > 0) {
                refs.add(userCount + " 个用户");
            }
            if (positionCount > 0) {
                refs.add(positionCount + " 个岗位");
            }
            if (employeeCount > 0) {
                refs.add(employeeCount + " 名员工");
            }
            throw BusinessException.badRequest("该部门下还有 " + String.join("、", refs) + "，无法删除");
        }

        this.removeById(id);
        log.info("删除部门成功: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids) {
        for (Long id : ids) {
            this.delete(id);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long id, Integer status) {
        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        dept.setStatus(status);
        this.updateById(dept);
        log.info("更新部门状态: id={}, status={}", id, status);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void move(Long id, Long targetId, String position) {
        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        Department target = this.getById(targetId);
        if (target == null) {
            throw BusinessException.badRequest("目标部门不存在");
        }

        // 先算出移动后的新上级，三种位置统一收敛
        Long newParentId;
        switch (position) {
            case "inner":
                // 移动到目标部门下作为子部门
                newParentId = targetId;
                break;
            case "before":
            case "after":
                // 同级移动，调整排序（新上级 = 目标的上级）
                newParentId = target.getParentId();
                break;
            default:
                throw BusinessException.badRequest("无效的移动位置: " + position);
        }
        newParentId = normalizeParentId(newParentId);

        // 防环校验：新上级不能是自己，也不能是自己的后代（与 update 共用同一套判定）
        checkParentCycle(id, newParentId);

        dept.setParentId(newParentId);
        // 与 create / update 共用祖先路径计算；目标 ancestors 为 NULL 时按 "0" 处理，
        // 不再拼出 "null,xxx" 脏值
        String newAncestors = calcAncestors(newParentId);
        dept.setAncestors(newAncestors);
        this.updateById(dept);

        // 自身层级变了 → 递归重算全部后代 ancestors，避免 DEPT_CHILD 数据权限范围失真
        refreshDescendantAncestors(id, newAncestors, loadChildrenMap());

        log.info("移动部门成功: id={}, targetId={}, position={}", id, targetId, position);
    }

    /**
     * 检查 targetId 是否是 id 的子部门（防止循环引用）
     */
    private boolean isDescendant(Long id, Long targetId) {
        if (id.equals(targetId)) {
            return true;
        }
        Department target = this.getById(targetId);
        if (target == null || target.getParentId() == null || target.getParentId() == 0) {
            return false;
        }
        return isDescendant(id, target.getParentId());
    }

    @Override
    public void export(DepartmentQueryRequest request, HttpServletResponse response) throws IOException {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(request.getDepartmentCode())) {
            wrapper.like(Department::getDeptCode, request.getDepartmentCode());
        }
        if (StringUtils.hasText(request.getDepartmentName())) {
            wrapper.like(Department::getDeptName, request.getDepartmentName());
        }
        if (request.getStatus() != null) {
            wrapper.eq(Department::getStatus, request.getStatus());
        }

        wrapper.orderByAsc(Department::getSort);
        List<Department> list = this.list(wrapper);

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment;filename=departments.csv");
        response.setCharacterEncoding("UTF-8");

        try (PrintWriter writer = new PrintWriter(new OutputStreamWriter(response.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.println("部门编码,部门名称,负责人,联系电话,邮箱,排序,状态");
            for (Department d : list) {
                writer.printf("%s,%s,%s,%s,%s,%d,%d%n",
                    d.getDeptCode(),
                    d.getDeptName(),
                    d.getLeaderName() != null ? d.getLeaderName() : "",
                    d.getPhone() != null ? d.getPhone() : "",
                    d.getEmail() != null ? d.getEmail() : "",
                    d.getSort() != null ? d.getSort() : 0,
                    d.getStatus() != null ? d.getStatus() : 1);
            }
            writer.flush();
        }
    }

    // ===== 私有辅助方法 =====

    /**
     * 归一化父部门ID：null 一律按 0（根）处理
     */
    private Long normalizeParentId(Long parentId) {
        return parentId == null ? 0L : parentId;
    }

    /**
     * 归一化祖级路径：NULL / 空白一律按根路径 "0" 处理。
     *
     * <p>历史数据里 ancestors 可能为 NULL（存量种子部门即如此），若直接字符串拼接
     * 会产出 "null,9001" 这类脏值，进而污染 DEPT_CHILD 数据权限解析。</p>
     */
    private String normalizeAncestors(String ancestors) {
        return StringUtils.hasText(ancestors) ? ancestors.trim() : ROOT_ANCESTORS;
    }

    /**
     * 计算某部门在指定上级下的祖级路径（ancestors 不包含自身）。
     *
     * <p>create / update / move 三处共用本方法，避免口径各写各的。</p>
     *
     * @param parentId 上级部门ID，null 或 0 表示根部门
     * @return 形如 "0" 或 "0,1,2" 的祖级路径
     */
    private String calcAncestors(Long parentId) {
        Long normalizedParentId = normalizeParentId(parentId);
        if (normalizedParentId <= 0) {
            return ROOT_ANCESTORS;
        }
        Department parent = this.getById(normalizedParentId);
        if (parent == null) {
            throw BusinessException.badRequest("父部门不存在");
        }
        return normalizeAncestors(parent.getAncestors()) + "," + parent.getId();
    }

    /**
     * 防环校验：新上级不能是自己，也不能是自己的后代。
     *
     * <p>update 与 move 共用同一套判定，避免重复实现两遍。</p>
     */
    private void checkParentCycle(Long id, Long newParentId) {
        Long normalizedParentId = normalizeParentId(newParentId);
        if (normalizedParentId <= 0) {
            return;
        }
        if (id.equals(normalizedParentId)) {
            throw BusinessException.badRequest("不能将部门移动到自己下面");
        }
        if (isDescendant(id, normalizedParentId)) {
            throw BusinessException.badRequest("不能将部门移动到自己的子部门下");
        }
    }

    /**
     * 加载本租户全部部门并按 parentId 分组（用于递归传播 ancestors）。
     *
     * <p>本表有 tenant_id 且不在 IGNORE_TENANT_TABLES 内 → 多租户拦截器自动注入 tenant_id；
     * @TableLogic 自动过滤已删除行。刻意不使用 inSql / apply 内联 SQL ——
     * 多租户插件不会改写内联 SQL，那样必须自己拼 tenant 条件（本仓库已知陷阱）。</p>
     */
    private Map<Long, List<Department>> loadChildrenMap() {
        List<Department> all = this.list(new LambdaQueryWrapper<>());
        return all.stream()
            .filter(d -> d.getParentId() != null)
            .collect(Collectors.groupingBy(Department::getParentId));
    }

    /**
     * 递归刷新全部后代的祖级路径。
     *
     * <p>自身 ancestors 变化后，子树每一层的路径都必须同步前缀，否则层级路径永久失真。</p>
     *
     * @param deptId       当前部门ID
     * @param deptAncestors 当前部门已更新后的 ancestors
     * @param childrenMap  刷新前的父子分组（移动/改上级不会改变后代的 parentId，分组依然有效）
     */
    private void refreshDescendantAncestors(Long deptId, String deptAncestors,
                                            Map<Long, List<Department>> childrenMap) {
        List<Department> children = childrenMap.get(deptId);
        if (children == null || children.isEmpty()) {
            return;
        }
        for (Department child : children) {
            String childAncestors = normalizeAncestors(deptAncestors) + "," + deptId;
            child.setAncestors(childAncestors);
            this.updateById(child);
            // 继续向更深层传播
            refreshDescendantAncestors(child.getId(), childAncestors, childrenMap);
        }
    }

    /**
     * 构建树结构
     */
    private List<DepartmentVO> buildTree(Long parentId, Map<Long, List<Department>> groupByParent) {
        List<DepartmentVO> result = new ArrayList<>();

        List<Department> children = groupByParent.get(parentId);
        if (children == null) {
            return result;
        }

        for (Department dept : children) {
            DepartmentVO vo = convertToVO(dept);
            vo.setChildren(buildTree(dept.getId(), groupByParent));
            result.add(vo);
        }

        return result;
    }

    /**
     * 转换为VO
     */
    private DepartmentVO convertToVO(Department dept) {
        DepartmentVO vo = new DepartmentVO();
        BeanUtils.copyProperties(dept, vo);

        // 字段映射
        vo.setDepartmentCode(dept.getDeptCode());
        vo.setDepartmentName(dept.getDeptName());
        vo.setPath(dept.getAncestors());
        vo.setDescription(dept.getRemark());

        // 计算层级
        if (dept.getAncestors() != null) {
            vo.setLevel(dept.getAncestors().split(",").length);
        } else {
            vo.setLevel(0);
        }

        // 查询父部门名称
        if (dept.getParentId() != null && dept.getParentId() > 0) {
            Department parent = this.getById(dept.getParentId());
            if (parent != null) {
                vo.setParentName(parent.getDeptName());
            }
        }

        return vo;
    }
}
