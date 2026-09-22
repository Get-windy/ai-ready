package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.borrow.dto.BorrowOrderItemVO;
import cn.aiedge.wms.borrow.dto.BorrowOrderQuery;
import cn.aiedge.wms.borrow.dto.BorrowReturnRequest;
import cn.aiedge.wms.borrow.dto.ConvertPurchaseRequest;
import cn.aiedge.wms.borrow.dto.WmsBorrowOrderVO;
import cn.aiedge.wms.borrow.service.BorrowService;
import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowReturn;
import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Validated
@Tag(name = "借进借出管理")
@RestController
@RequestMapping("/api/wms/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "分页查询借进借出单")
    @SaCheckPermission("wms:borrow:list")
    @GetMapping("/page")
    public Result<Page<WmsBorrowOrder>> page(@Valid Page<WmsBorrowOrder> page, WmsBorrowOrder query) {
        return Result.ok(borrowService.pageOrder(page, query));
    }

    @Operation(summary = "生成下一借进/借出单号")
    @SaCheckPermission("wms:borrow:list")
    @GetMapping("/next-no")
    public Result<String> nextNo(@RequestParam(required = false) Integer direction,
                                 @RequestParam(required = false) String prefix) {
        if (direction == null) {
            // 前端 codeGenerator 会传 prefix='JJD'/'JCD'，据此推断方向
            direction = (prefix != null && prefix.toUpperCase().contains("CD"))
                    ? BorrowService.DIRECTION_OUT : BorrowService.DIRECTION_IN;
        }
        return Result.ok(borrowService.generateNo(direction));
    }

    @Operation(summary = "多条件分页查询借进借出单(按单据)")
    @SaCheckPermission("wms:borrow:list")
    @GetMapping("/doc-query")
    public Result<Page<WmsBorrowOrder>> docQuery(BorrowOrderQuery query) {
        return Result.ok(borrowService.pageOrderByQuery(query));
    }

    @Operation(summary = "分页查询借进借出明细(按明细)")
    @SaCheckPermission("wms:borrow:view")
    @GetMapping("/page-detail")
    public Result<Page<BorrowOrderItemVO>> pageDetail(BorrowOrderQuery query) {
        return Result.ok(borrowService.pageDetail(query));
    }

    @Operation(summary = "记账(入库/出库)")
    @SaCheckPermission("wms:borrow:post")
    @PostMapping("/post")
    public Result<String> post(@RequestParam @NotNull Long id,
                               @RequestParam(required = false) Long operatorId,
                               @RequestParam(required = false) String operatorName) {
        borrowService.post(id, operatorId, operatorName);
        log.info("记账: id={}, operatorId={}", id, operatorId);
        return Result.ok("记账成功");
    }

    @Operation(summary = "借转采购登记")
    @SaCheckPermission("wms:borrow:convert")
    @PostMapping("/convert-purchase")
    public Result<WmsBorrowOrder> convertPurchase(@Valid @RequestBody ConvertPurchaseRequest request) {
        WmsBorrowOrder updated = borrowService.convertPurchase(request);
        log.info("借转采购: orderId={}, orderNo={}", updated.getId(), updated.getOrderNo());
        return Result.ok(updated);
    }

    @Operation(summary = "借转销售登记（借出方向）")
    @SaCheckPermission("wms:borrow:convert")
    @PostMapping("/convert-sale")
    public Result<WmsBorrowOrder> convertSale(@Valid @RequestBody ConvertPurchaseRequest request) {
        WmsBorrowOrder updated = borrowService.convertSale(request);
        log.info("借转销售: orderId={}, orderNo={}", updated.getId(), updated.getOrderNo());
        return Result.ok(updated);
    }

    @Operation(summary = "借进借出商品台账聚合查询（按 商品×往来单位 分组）")
    @SaCheckPermission("wms:borrow:view")
    @GetMapping("/aggregate")
    public Result<List<java.util.Map<String, Object>>> aggregate(
            @Parameter(description = "方向 1-借进 2-借出") @RequestParam(required = false) Integer direction,
            @Parameter(description = "往来单位") @RequestParam(required = false) String partnerName,
            @Parameter(description = "商品名称") @RequestParam(required = false) String productName,
            @Parameter(description = "开始日期 YYYY-MM-DD") @RequestParam(required = false) String dateStart,
            @Parameter(description = "结束日期 YYYY-MM-DD") @RequestParam(required = false) String dateEnd,
            @Parameter(description = "商品分类ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "经手人") @RequestParam(required = false) String handlerName,
            @Parameter(description = "部门") @RequestParam(required = false) String deptName) {
        return Result.ok(borrowService.aggregateByProduct(direction, partnerName, productName, dateStart, dateEnd, categoryId, handlerName, deptName));
    }

    @Operation(summary = "查询借进借出单（含明细）")
    @SaCheckPermission("wms:borrow:detail")
    @GetMapping("/{id}")
    public Result<WmsBorrowOrderVO> getById(@PathVariable @NotNull(message = "单据ID不能为空") Long id) {
        WmsBorrowOrderVO vo = borrowService.getOrderDetail(id);
        if (vo == null) {
            return Result.fail("借进借出单不存在");
        }
        return Result.ok(vo);
    }

    @Operation(summary = "新建借进借出单（含明细）")
    @SaCheckPermission("wms:borrow:create")
    @PostMapping("/create")
    public Result<WmsBorrowOrder> create(@Valid @RequestBody WmsBorrowOrderVO order) {
        WmsBorrowOrder created = borrowService.createOrder(order);
        log.info("新建借进借出单: id={}, orderNo={}", created.getId(), created.getOrderNo());
        return Result.ok(created);
    }

    @Operation(summary = "更新借进借出单（仅草稿，明细整体替换）")
    @SaCheckPermission("wms:borrow:create")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsBorrowOrderVO order) {
        borrowService.updateOrder(order);
        log.info("更新借进借出单: id={}", order.getId());
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "删除借进借出单（仅草稿/已取消）")
    @SaCheckPermission("wms:borrow:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "单据ID不能为空") Long id) {
        borrowService.removeOrder(id);
        log.info("删除借进借出单: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "提交审批")
    @SaCheckPermission("wms:borrow:submit")
    @PostMapping("/submit")
    public Result<String> submit(@RequestParam @NotNull Long id) {
        borrowService.submit(id);
        log.info("提交审批: id={}", id);
        return Result.ok("提交成功");
    }

    @Operation(summary = "审批通过（借进库存增加/借出库存扣减）")
    @SaCheckPermission("wms:borrow:approve")
    @PostMapping("/approve")
    public Result<String> approve(@RequestParam @NotNull Long id,
                                  @RequestParam(required = false) Long operatorId,
                                  @RequestParam(required = false) String operatorName) {
        borrowService.approve(id, operatorId, operatorName);
        log.info("审批通过: id={}, operatorId={}", id, operatorId);
        return Result.ok("审批通过");
    }

    @Operation(summary = "取消单据")
    @SaCheckPermission("wms:borrow:cancel")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long id) {
        borrowService.cancel(id);
        log.info("取消单据: id={}", id);
        return Result.ok("取消成功");
    }

    @Operation(summary = "归还登记（支持部分归还，库存反向回冲）")
    @SaCheckPermission("wms:borrow:return")
    @PostMapping("/return")
    public Result<WmsBorrowReturn> returnOrder(@Valid @RequestBody BorrowReturnRequest request) {
        WmsBorrowReturn ret = borrowService.returnOrder(request);
        log.info("归还登记: orderId={}, returnId={}", request.getOrderId(), ret.getId());
        return Result.ok(ret);
    }

    @Operation(summary = "归还记录分页")
    @SaCheckPermission("wms:borrow:view")
    @GetMapping("/return-page")
    public Result<Page<WmsBorrowReturn>> returnPage(@Valid Page<WmsBorrowReturn> page,
                                                    @RequestParam(required = false) Long orderId) {
        return Result.ok(borrowService.pageReturn(page, orderId));
    }

    @Operation(summary = "归还记录明细")
    @SaCheckPermission("wms:borrow:detail")
    @GetMapping("/return-items/{returnId}")
    public Result<List<WmsBorrowReturnItem>> returnItems(@PathVariable @NotNull Long returnId) {
        return Result.ok(borrowService.listReturnItems(returnId));
    }
}
