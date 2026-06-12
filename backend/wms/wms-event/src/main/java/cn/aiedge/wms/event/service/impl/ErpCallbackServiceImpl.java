package cn.aiedge.wms.event.service.impl;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.event.service.ErpCallbackService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErpCallbackServiceImpl implements ErpCallbackService {

    @Override
    public Result<String> handleReceiptComplete(JsonNode payload) {
        log.info("ERP 收到入库完成回调: {}", payload);
        return Result.ok("入库完成通知已处理");
    }

    @Override
    public Result<String> handleShipComplete(JsonNode payload) {
        log.info("ERP 收到出库完成回调: {}", payload);
        return Result.ok("出库完成通知已处理");
    }

    @Override
    public Result<String> handleInventoryChange(JsonNode payload) {
        log.info("ERP 收到库存变动回调: {}", payload);
        return Result.ok("库存变动通知已处理");
    }

    @Override
    public Result<String> handleCheckDiff(JsonNode payload) {
        log.info("ERP 收到盘点差异回调: {}", payload);
        return Result.ok("盘点差异通知已处理");
    }
}
