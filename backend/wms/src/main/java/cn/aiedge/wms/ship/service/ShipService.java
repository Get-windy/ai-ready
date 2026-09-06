package cn.aiedge.wms.ship.service;

import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.entity.WmsShipDetail;
import cn.aiedge.wms.ship.dto.ShipQuery;
import cn.aiedge.wms.ship.dto.WmsShipDetailPageVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.math.BigDecimal;
import java.util.List;

public interface ShipService {
    boolean saveTask(WmsShipTask task);
    boolean updateTask(WmsShipTask task);
    WmsShipTask getTaskById(Long id);
    Page<WmsShipTask> pageTask(Page<WmsShipTask> page, WmsShipTask query);
    boolean removeTask(Long id);
    boolean saveDetail(WmsShipDetail detail);
    boolean updateDetail(WmsShipDetail detail);
    List<WmsShipDetail> listByShipId(Long shipId);
    // 按单据多条件分页（对齐报损单 page）
    Page<WmsShipTask> queryPage(Page<WmsShipTask> page, ShipQuery query);
    // 按明细分页（JOIN 单头，对齐报损单 page-detail）
    IPage<WmsShipDetailPageVO> pageDetail(IPage<WmsShipDetailPageVO> page, ShipQuery query);
    // 明细整体保存（先删后插，仅待处理状态可操作）
    void saveDetails(Long shipId, List<WmsShipDetail> details);
    void startShip(Long taskId, Long userId, String userName);
    void scanItem(Long detailId, BigDecimal scannedQuantity);
    void confirmShip(Long taskId);
}
