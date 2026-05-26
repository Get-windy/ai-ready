package com.aiready.system.service;

import com.aiready.system.entity.Position;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 岗位服务接口
 */
public interface PositionService extends IService<Position> {

    /**
     * 根据岗位编码查询
     */
    Position getByPositionCode(String positionCode);

    /**
     * 根据部门ID查询岗位列表
     */
    List<Position> listByDeptId(Long deptId);

    /**
     * 根据岗位类型查询
     */
    List<Position> listByType(Integer positionType);

    /**
     * 获取所有启用的岗位列表
     */
    List<Position> listActivePositions();

    /**
     * 检查岗位编码是否已存在
     */
    boolean checkPositionCodeExists(String positionCode);

    /**
     * 检查岗位编码是否已存在（排除指定ID）
     */
    boolean checkPositionCodeExists(String positionCode, Long excludeId);

    /**
     * 检查岗位下是否有员工
     */
    boolean hasEmployees(Long positionId);

    /**
     * 启用岗位
     */
    boolean enablePosition(Long id);

    /**
     * 停用岗位
     */
    boolean disablePosition(Long id);

    /**
     * 更新在职人数
     */
    boolean updateCurrentCount(Long positionId, Integer delta);

    /**
     * 根据ID查询（包含部门名称）
     */
    Position getPositionById(Long id);

    /**
     * 获取部门下的岗位树
     */
    List<Position> getPositionsByDeptTree(Long deptId);

    /**
     * 批量更新状态
     */
    boolean batchUpdateStatus(List<Long> ids, Integer status);
}
