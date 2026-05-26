package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.Department;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 部门Mapper
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {

    /**
     * 根据部门编码查询部门
     */
    Department selectByDeptCode(@Param("deptCode") String deptCode);

    /**
     * 查询子部门列表
     */
    List<Department> selectByParentId(@Param("parentId") Long parentId);

    /**
     * 查询所有启用的部门
     */
    List<Department> selectAllEnabled();

    /**
     * 根据祖级路径查询部门列表
     */
    List<Department> selectByAncestors(@Param("ancestors") String ancestors);
}
