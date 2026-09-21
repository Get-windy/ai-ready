package cn.aiedge.department.service;

import cn.aiedge.base.entity.Department;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.department.dto.DepartmentQueryRequest;
import cn.aiedge.department.dto.DepartmentVO;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 部门服务接口
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface DepartmentService extends IService<Department> {

    /**
     * 分页查询部门
     */
    PageResult<DepartmentVO> pageList(DepartmentQueryRequest request);

    /**
     * 获取所有部门列表（仅启用部门）
     */
    List<DepartmentVO> listAll();

    /**
     * 获取部门下拉选项
     *
     * <p>与 {@link #listAll()} 的区别：<b>不过滤 status</b>，返回全部未删除部门（含禁用）。
     * 存量种子部门 status 均为 0（禁用），若按 status=1 过滤会导致全站部门下拉为空、
     * 用户无法选择上级部门。下拉类场景请使用本方法。</p>
     */
    List<DepartmentVO> listOptions();

    /**
     * 获取部门树
     */
    List<DepartmentVO> getTree();

    /**
     * 获取部门详情
     */
    DepartmentVO getDetail(Long id);

    /**
     * 创建部门
     */
    Long create(DepartmentVO request);

    /**
     * 更新部门
     */
    void update(Long id, DepartmentVO request);

    /**
     * 删除部门
     */
    void delete(Long id);

    /**
     * 批量删除部门
     */
    void batchDelete(List<Long> ids);

    /**
     * 更新部门状态
     */
    void updateStatus(Long id, Integer status);

    /**
     * 移动部门
     */
    void move(Long id, Long targetId, String position);

    /**
     * 导出部门数据
     */
    void export(DepartmentQueryRequest request, HttpServletResponse response) throws IOException;
}
