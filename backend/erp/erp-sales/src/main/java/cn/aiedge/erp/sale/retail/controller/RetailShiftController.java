package cn.aiedge.erp.sale.retail.controller;

import cn.aiedge.erp.sale.retail.entity.RetailShift;
import cn.aiedge.erp.sale.retail.service.IRetailShiftService;
import cn.aiedge.erp.sale.retail.service.IRetailShiftService.ShiftDetailVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@Slf4j
@Tag(name = "零售交班管理", description = "POS收银班次开班、交班、班次汇总查询")
@RestController
@RequestMapping("/api/sales/retail/shift")
@RequiredArgsConstructor
public class RetailShiftController {

    private final IRetailShiftService retailShiftService;

    // ═══ 1. 开班 ═══
    @PostMapping("/open")
    @Operation(summary = "开班（校验无未交班班次）")
    public RetailShift open(@RequestBody OpenShiftRequest request) {
        return retailShiftService.openShift(request.getCashierId(), request.getCashierName(),
                request.getWarehouseId(), request.getOpeningCash(), request.getRemark());
    }

    // ═══ 2. 交班 ═══
    @PostMapping("/close")
    @Operation(summary = "交班（录入实点现金，自动汇总班次销售并计算长短款）")
    public RetailShift close(@RequestBody CloseShiftRequest request) {
        return retailShiftService.closeShift(request.getShiftId(), request.getClosingCash(), request.getRemark());
    }

    // ═══ 3. 当前班次 ═══
    @GetMapping("/current")
    @Operation(summary = "查询收银员当前营业中班次")
    public RetailShift current(
            @Parameter(description = "收银员ID") @RequestParam Long cashierId) {
        return retailShiftService.getCurrent(cashierId);
    }

    // ═══ 4. 历史班次分页 ═══
    @GetMapping("/page")
    @Operation(summary = "历史班次分页查询")
    public IPage<RetailShift> page(
            @Parameter(description = "收银员ID") @RequestParam(required = false) Long cashierId,
            @Parameter(description = "状态：1=营业中 2=已交班") @RequestParam(required = false) Integer status,
            @Parameter(description = "开始日期") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期") @RequestParam(required = false) String dateEnd,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return retailShiftService.pageShifts(new Page<>(pageNum, pageSize), cashierId, status, dateStart, dateEnd);
    }

    // ═══ 5. 班次详情（含汇总明细） ═══
    @GetMapping("/{id}")
    @Operation(summary = "查询班次详情（含该班次已结算零售单明细）")
    public ShiftDetailVO getDetail(@PathVariable Long id) {
        return retailShiftService.getDetail(id);
    }

    // ── 请求体DTO ──

    public static class OpenShiftRequest {
        private Long cashierId;
        private String cashierName;
        private Long warehouseId;
        private BigDecimal openingCash;
        private String remark;

        public Long getCashierId() { return cashierId; }
        public void setCashierId(Long cashierId) { this.cashierId = cashierId; }
        public String getCashierName() { return cashierName; }
        public void setCashierName(String cashierName) { this.cashierName = cashierName; }
        public Long getWarehouseId() { return warehouseId; }
        public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
        public BigDecimal getOpeningCash() { return openingCash; }
        public void setOpeningCash(BigDecimal openingCash) { this.openingCash = openingCash; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }

    public static class CloseShiftRequest {
        private Long shiftId;
        private BigDecimal closingCash;
        private String remark;

        public Long getShiftId() { return shiftId; }
        public void setShiftId(Long shiftId) { this.shiftId = shiftId; }
        public BigDecimal getClosingCash() { return closingCash; }
        public void setClosingCash(BigDecimal closingCash) { this.closingCash = closingCash; }
        public String getRemark() { return remark; }
        public void setRemark(String remark) { this.remark = remark; }
    }
}
