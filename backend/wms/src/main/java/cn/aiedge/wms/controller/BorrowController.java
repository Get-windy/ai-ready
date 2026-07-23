package cn.aiedge.wms.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.wms.borrow.dto.BorrowReturnRequest;
import cn.aiedge.wms.borrow.dto.WmsBorrowOrderVO;
import cn.aiedge.wms.borrow.service.BorrowService;
import cn.aiedge.wms.entity.WmsBorrowOrder;
import cn.aiedge.wms.entity.WmsBorrowReturn;
import cn.aiedge.wms.entity.WmsBorrowReturnItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Validated
@Tag(name = "借进借出管理")
@RestController
@RequestMapping("/api/wms/borrow")
@RequiredArgsConstructor
public class BorrowController {

    private final BorrowService borrowService;

    @Operation(summary = "分页查询借进借出单")
    @GetMapping("/page")
    public Result<Page<WmsBorrowOrder>> page(@Valid Page<WmsBorrowOrder> page, WmsBorrowOrder query) {
        return Result.ok(borrowService.pageOrder(page, query));
    }

    @Operation(summary = "查询借进借出单（含明细）")
    @GetMapping("/{id}")
    public Result<WmsBorrowOrderVO> getById(@PathVariable @NotNull(message = "单据ID不能为空") Long id) {
        WmsBorrowOrderVO vo = borrowService.getOrderDetail(id);
        if (vo == null) {
            return Result.fail("借进借出单不存在");
        }
        return Result.ok(vo);
    }

    @Operation(summary = "新建借进借出单（含明细）")
    @PostMapping("/create")
    public Result<WmsBorrowOrder> create(@Valid @RequestBody WmsBorrowOrderVO order) {
        WmsBorrowOrder created = borrowService.createOrder(order);
        log.info("新建借进借出单: id={}, orderNo={}", created.getId(), created.getOrderNo());
        return Result.ok(created);
    }

    @Operation(summary = "更新借进借出单（仅草稿，明细整体替换）")
    @PostMapping("/update")
    public Result<Boolean> update(@Valid @RequestBody WmsBorrowOrderVO order) {
        borrowService.updateOrder(order);
        log.info("更新借进借出单: id={}", order.getId());
        return Result.ok(Boolean.TRUE);
    }

    @Operation(summary = "删除借进借出单（仅草稿/已取消）")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable @NotNull(message = "单据ID不能为空") Long id) {
        borrowService.removeOrder(id);
        log.info("删除借进借出单: id={}", id);
        return Result.ok("删除成功");
    }

    @Operation(summary = "提交审批")
    @PostMapping("/submit")
    public Result<String> submit(@RequestParam @NotNull Long id) {
        borrowService.submit(id);
        log.info("提交审批: id={}", id);
        return Result.ok("提交成功");
    }

    @Operation(summary = "审批通过（借进库存增加/借出库存扣减）")
    @PostMapping("/approve")
    public Result<String> approve(@RequestParam @NotNull Long id,
                                  @RequestParam(required = false) Long operatorId,
                                  @RequestParam(required = false) String operatorName) {
        borrowService.approve(id, operatorId, operatorName);
        log.info("审批通过: id={}, operatorId={}", id, operatorId);
        return Result.ok("审批通过");
    }

    @Operation(summary = "取消单据")
    @PostMapping("/cancel")
    public Result<String> cancel(@RequestParam @NotNull Long id) {
        borrowService.cancel(id);
        log.info("取消单据: id={}", id);
        return Result.ok("取消成功");
    }

    @Operation(summary = "归还登记（支持部分归还，库存反向回冲）")
    @PostMapping("/return")
    public Result<WmsBorrowReturn> returnOrder(@Valid @RequestBody BorrowReturnRequest request) {
        WmsBorrowReturn ret = borrowService.returnOrder(request);
        log.info("归还登记: orderId={}, returnId={}", request.getOrderId(), ret.getId());
        return Result.ok(ret);
    }

    @Operation(summary = "归还记录分页")
    @GetMapping("/return-page")
    public Result<Page<WmsBorrowReturn>> returnPage(@Valid Page<WmsBorrowReturn> page,
                                                    @RequestParam(required = false) Long orderId) {
        return Result.ok(borrowService.pageReturn(page, orderId));
    }

    @Operation(summary = "归还记录明细")
    @GetMapping("/return-items/{returnId}")
    public Result<List<WmsBorrowReturnItem>> returnItems(@PathVariable @NotNull Long returnId) {
        return Result.ok(borrowService.listReturnItems(returnId));
    }
}
