package cn.aiedge.position.service;

import cn.aiedge.position.dto.*;
import cn.aiedge.position.entity.Position;
import cn.aiedge.common.result.PageResult;
import com.baomidou.mybatisplus.extension.service.IService;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

/**
 * 岗位服务接口
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
public interface PositionService extends IService<Position> {

    /**
     * 分页查询岗位
     */
    PageResult<PositionVO> pageList(PositionQueryRequest request);

    /**
     * 获取岗位列表（下拉选择用）
     */
    List<PositionVO> listAll();

    /**
     * 根据ID获取岗位详情
     */
    PositionVO getDetail(Long id);

    /**
     * 创建岗位
     */
    Long create(PositionCreateRequest request);

    /**
     * 更新岗位
     */
    void update(PositionUpdateRequest request);

    /**
     * 删除岗位
     */
    void delete(Long id);

    /**
     * 批量删除岗位
     */
    void batchDelete(List<Long> ids);

    /**
     * 启用/禁用岗位
     */
    void updateStatus(Long id, Integer status);

    /**
     * 导出岗位数据
     */
    void export(PositionQueryRequest request, HttpServletResponse response) throws IOException;

    /**
     * 根据部门ID获取岗位列表
     */
    List<PositionVO> getByDeptId(Long deptId);

    /**
     * 根据用户ID获取岗位列表
     */
    List<PositionVO> getByUserId(Long userId);

    /**
     * 分配岗位给用户
     */
    void assignToUser(Long userId, List<Long> positionIds, Long primaryPositionId);

    /**
     * 移除用户的岗位
     */
    void removeFromUser(Long userId, List<Long> positionIds);

    /**
     * 获取岗位下的用户ID列表
     */
    List<Long> getUserIds(Long positionId);
}
