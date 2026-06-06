package cn.aiedge.department.service.impl;

import cn.aiedge.base.entity.Department;
import cn.aiedge.base.mapper.DepartmentMapper;
import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.department.dto.DepartmentQueryRequest;
import cn.aiedge.department.dto.DepartmentVO;
import cn.aiedge.department.service.DepartmentService;
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

        // 设置祖级路径
        if (dept.getParentId() > 0) {
            Department parent = this.getById(dept.getParentId());
            if (parent == null) {
                throw BusinessException.badRequest("父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        } else {
            dept.setAncestors("0");
        }

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
        if (request.getParentId() != null) {
            dept.setParentId(request.getParentId());
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
        log.info("更新部门成功: id={}", dept.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        // 检查是否有子部门
        LambdaQueryWrapper<Department> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.eq(Department::getParentId, id);
        if (this.count(childWrapper) > 0) {
            throw BusinessException.badRequest("存在子部门，无法删除");
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
        if (id.equals(targetId) && "inner".equals(position)) {
            throw BusinessException.badRequest("不能将部门移动到自己下面");
        }

        Department dept = this.getById(id);
        if (dept == null) {
            throw BusinessException.notFound("部门不存在");
        }

        Department target = this.getById(targetId);
        if (target == null) {
            throw BusinessException.badRequest("目标部门不存在");
        }

        // 检查是否为目标部门的子部门（防止循环）
        if ("inner".equals(position)) {
            if (isDescendant(id, targetId)) {
                throw BusinessException.badRequest("不能移动到自己的子部门下");
            }
        }

        switch (position) {
            case "inner":
                // 移动到目标部门下作为子部门
                dept.setParentId(targetId);
                dept.setAncestors(target.getAncestors() + "," + target.getId());
                break;
            case "before":
            case "after":
                // 同级移动，调整排序
                dept.setParentId(target.getParentId());
                dept.setAncestors(target.getAncestors());
                break;
            default:
                throw BusinessException.badRequest("无效的移动位置: " + position);
        }

        this.updateById(dept);
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
