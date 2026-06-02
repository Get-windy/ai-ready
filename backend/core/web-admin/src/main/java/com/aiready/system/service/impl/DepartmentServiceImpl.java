package com.aiready.system.service.impl;

import com.aiready.system.entity.Department;
import com.aiready.system.mapper.DepartmentMapper;
import com.aiready.system.service.DepartmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门服务实现类
 */
@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, Department> implements DepartmentService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public Department getByDeptCode(String deptCode) {
        return this.getOne(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptCode, deptCode)
                .eq(Department::getDeleted, 0)
        );
    }

    @Override
    public List<Department> listByParentId(Long parentId) {
        return this.list(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, parentId)
                .eq(Department::getStatus, 1)
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSortOrder)
        );
    }

    @Override
    public List<Department> getDeptTree() {
        List<Department> allDepts = this.list(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getStatus, 1)
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSortOrder)
        );
        return buildTree(allDepts, 0L);
    }

    @Override
    public List<Department> getDeptTreeExclude(Long excludeDeptId) {
        List<Department> allDepts = this.list(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getStatus, 1)
                .eq(Department::getDeleted, 0)
                .orderByAsc(Department::getSortOrder)
        );
        // 排除指定部门及其子部门
        List<Long> excludeIds = getChildDeptIds(excludeDeptId);
        allDepts = allDepts.stream()
            .filter(dept -> !excludeIds.contains(dept.getId()))
            .collect(Collectors.toList());
        return buildTree(allDepts, 0L);
    }

    @Override
    public List<Long> getChildDeptIds(Long deptId) {
        List<Long> result = new ArrayList<>();
        result.add(deptId);
        
        List<Department> children = listByParentId(deptId);
        for (Department child : children) {
            result.addAll(getChildDeptIds(child.getId()));
        }
        
        return result;
    }

    @Override
    public boolean checkDeptCodeExists(String deptCode) {
        return this.count(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptCode, deptCode)
                .eq(Department::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean checkDeptCodeExists(String deptCode, Long excludeId) {
        return this.count(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getDeptCode, deptCode)
                .ne(Department::getId, excludeId)
                .eq(Department::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean hasChildren(Long deptId) {
        return this.count(
            new LambdaQueryWrapper<Department>()
                .eq(Department::getParentId, deptId)
                .eq(Department::getDeleted, 0)
        ) > 0;
    }

    @Override
    public boolean hasUsers(Long deptId) {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM sys_user WHERE dept_id = ? AND deleted = 0",
            Integer.class, deptId);
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public boolean enableDept(Long id) {
        Department dept = new Department();
        dept.setId(id);
        dept.setStatus(1);
        return this.updateById(dept);
    }

    @Override
    @Transactional
    public boolean disableDept(Long id) {
        Department dept = new Department();
        dept.setId(id);
        dept.setStatus(0);
        return this.updateById(dept);
    }

    @Override
    public Department getDeptById(Long id) {
        Department dept = this.getById(id);
        if (dept != null && dept.getParentId() != null) {
            Department parent = this.getById(dept.getParentId());
            if (parent != null) {
                dept.setParentName(parent.getDeptName());
            }
        }
        return dept;
    }

    @Override
    public List<Department> getParentDepts(Long deptId) {
        List<Department> result = new ArrayList<>();
        Department current = this.getById(deptId);
        
        while (current != null && current.getParentId() != null && current.getParentId() != 0) {
            current = this.getById(current.getParentId());
            if (current != null) {
                result.add(0, current);
            }
        }
        
        return result;
    }

    @Override
    @Transactional
    public boolean moveDept(Long deptId, Long newParentId) {
        Department dept = this.getById(deptId);
        if (dept == null) {
            return false;
        }

        // 更新父部门ID
        dept.setParentId(newParentId);
        
        // 更新层级路径
        if (newParentId == 0) {
            dept.setAncestors("0,");
        } else {
            Department parent = this.getById(newParentId);
            if (parent != null) {
                dept.setAncestors(parent.getAncestors() + newParentId + ",");
            }
        }
        
        return this.updateById(dept);
    }

    @Override
    public Integer getDeptLevel(Long deptId) {
        Department dept = this.getById(deptId);
        if (dept == null || !StringUtils.hasText(dept.getAncestors())) {
            return 0;
        }
        // 计算层级（根据逗号数量）
        String ancestors = dept.getAncestors();
        int count = 0;
        for (char c : ancestors.toCharArray()) {
            if (c == ',') {
                count++;
            }
        }
        return count - 1;
    }

    /**
     * 构建部门树
     */
    private List<Department> buildTree(List<Department> depts, Long parentId) {
        List<Department> result = new ArrayList<>();
        for (Department dept : depts) {
            if (dept.getParentId().equals(parentId)) {
                result.add(dept);
                // 递归构建子树
                List<Department> children = buildTree(depts, dept.getId());
                if (!CollectionUtils.isEmpty(children)) {
                    // 这里可以通过其他方式存储子部门，如使用VO对象
                }
            }
        }
        return result;
    }
}
