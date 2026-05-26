package com.aiready.system.service;

import com.aiready.system.entity.Department;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 部门服务接口
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 根据部门编码查询
     */
    Department getByDeptCode(String deptCode);

    /**
     * 根据父部门ID查询子部门列表
     */
    List<Department> listByParentId(Long parentId);

    /**
     * 获取部门树形结构
     */
    List<Department> getDeptTree();

    /**
     * 获取部门树形结构（排除指定部门及其子部门）
     */
    List<Department> getDeptTreeExclude(Long excludeDeptId);

    /**
     * 获取所有子部门ID（包含自身）
     */
    List<Long> getChildDeptIds(Long deptId);

    /**
     * 检查部门编码是否已存在
     */
    boolean checkDeptCodeExists(String deptCode);

    /**
     * 检查部门编码是否已存在（排除指定ID）
     */
    boolean checkDeptCodeExists(String deptCode, Long excludeId);

    /**
     * 检查部门是否有子部门
     */
    boolean hasChildren(Long deptId);

    /**
     * 检查部门下是否有用户
     */
    boolean hasUsers(Long deptId);

    /**
     * 启用部门
     */
    boolean enableDept(Long id);

    /**
     * 停用部门
     */
    boolean disableDept(Long id);

    /**
     * 根据ID查询（包含父部门名称）
     */
    Department getDeptById(Long id);

    /**
     * 获取上级部门列表
     */
    List<Department> getParentDepts(Long deptId);

    /**
     * 移动部门
     */
    boolean moveDept(Long deptId, Long newParentId);

    /**
     * 获取部门层级
     */
    Integer getDeptLevel(Long deptId);
}
