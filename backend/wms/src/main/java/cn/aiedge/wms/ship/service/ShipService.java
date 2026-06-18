package cn.aiedge.wms.ship.service;

import cn.aiedge.wms.entity.WmsShipTask;
import cn.aiedge.wms.entity.WmsShipDetail;
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
    void startShip(Long taskId, Long userId, String userName);
    void scanItem(Long detailId, BigDecimal scannedQuantity);
    void confirmShip(Long taskId);
}
