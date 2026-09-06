package cn.aiedge.wms.listener;

import cn.aiedge.erp.stock.event.StocktakeMoveEvent;
import cn.aiedge.wms.entity.WmsMoveDetail;
import cn.aiedge.wms.entity.WmsMoveTask;
import cn.aiedge.wms.move.service.MoveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 监听盘点「货位转移差异」事件，生成移库单（sourceType=盘点差异，sourceNo=盘点单号）.
 * <p>同一 Spring 容器内进程内解耦：erp-stock 发布 {@link StocktakeMoveEvent}，wms 依赖 erp-stock 可监听。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StocktakeMoveListener {

    private final MoveService moveService;

    @EventListener
    @Transactional(rollbackFor = Exception.class)
    public void onStocktakeMove(StocktakeMoveEvent event) {
        if (event.getItems() == null || event.getItems().isEmpty()) return;
        List<WmsMoveDetail> details = event.getItems().stream().map(t -> {
            WmsMoveDetail d = new WmsMoveDetail();
            d.setProductId(t.getProductId());
            d.setProductCode(t.getProductCode());
            d.setProductName(t.getProductName());
            d.setProductSpec(t.getProductSpec());
            d.setProductUnit(t.getProductUnit());
            d.setQuantity(t.getQuantity());
            d.setFromLocationCode(t.getFromLocationCode());
            d.setToLocationId(t.getToLocationId());
            d.setToLocationCode(t.getToLocationCode());
            d.setBatchNo(t.getBatchNo());
            d.setSerialNo(t.getSerialNo());
            d.setStatus(0);
            return d;
        }).collect(Collectors.toList());
        WmsMoveTask task = moveService.createMoveFromStocktake(
                event.getSourceNo(), event.getWarehouseId(), event.getWarehouseName(), details);
        log.info("盘点货位转移已生成移库单: sourceNo={}, taskNo={}, items={}",
                event.getSourceNo(), task.getTaskNo(), details.size());
    }
}
