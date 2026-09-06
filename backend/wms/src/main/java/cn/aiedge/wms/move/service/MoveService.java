package cn.aiedge.wms.move.service;

import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.move.dto.MoveTaskQuery;
import cn.aiedge.wms.move.dto.MoveDetailVO;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.util.List;

public interface MoveService {
    boolean saveTask(WmsMoveTask task);
    boolean updateTask(WmsMoveTask task);
    WmsMoveTask getTaskById(Long id);
    Page<WmsMoveTask> pageTask(Page<WmsMoveTask> page, MoveTaskQuery query);
    boolean removeTask(Long id);
    boolean saveDetail(WmsMoveDetail detail);
    boolean updateDetail(WmsMoveDetail detail);
    List<WmsMoveDetail> listByTaskId(Long taskId);
    // 明细整体保存（先删后插，仅待处理状态可操作）
    void saveDetails(Long taskId, List<WmsMoveDetail> details);
    void startMove(Long taskId, Long userId, String userName);
    void executeMove(Long taskId, Long userId, String userName);
    /** 取消移库（状态 0-待移库 / 1-移库中 可取消），记录取消原因 */
    void cancelMove(Long taskId, String reason);
    /** 生成移库单号（MV-…） */
    String nextNo();
    /** 按明细分页（明细行 + 单头字段） */
    Page<MoveDetailVO> pageDetail(Page<MoveDetailVO> page, MoveTaskQuery query);
    /** 从来源（盘点货位转移差异）创建移库单：sourceType=盘点差异，sourceNo=来源单号，状态=草稿 */
    WmsMoveTask createMoveFromStocktake(String sourceNo, Long warehouseId, String warehouseName, List<WmsMoveDetail> details);
}
