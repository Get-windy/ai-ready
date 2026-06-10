package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.entity.WriteOff;
import cn.aiedge.erp.payment.service.WriteOffService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/erp/write-off")
@RequiredArgsConstructor
@Tag(name = "核销记录管理", description = "收/付款单与应收/应付单的核销记录")
public class WriteOffController {

    private final WriteOffService writeOffService;

    @GetMapping("/page")
    @Operation(summary = "分页查询核销记录")
    public Page<WriteOff> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String writeOffType,
            @RequestParam(required = false) Long receiptId,
            @RequestParam(required = false) Long paymentId,
            @RequestParam(required = false) Long receivableId,
            @RequestParam(required = false) Long payableId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        return writeOffService.pageList(keyword, writeOffType, receiptId, paymentId,
                receivableId, payableId, startDate, endDate, pageNum, pageSize);
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取核销记录详情")
    public WriteOff getById(@PathVariable Long id) {
        WriteOff writeOff = writeOffService.getById(id);
        if (writeOff == null) {
            throw BusinessException.notFound("核销记录不存在");
        }
        return writeOff;
    }

    @GetMapping("/receipt/{receiptId}")
    @Operation(summary = "根据收款单查询核销记录")
    public List<WriteOff> getByReceiptId(@PathVariable Long receiptId) {
        return writeOffService.getByReceiptId(receiptId);
    }

    @GetMapping("/payment/{paymentId}")
    @Operation(summary = "根据付款单查询核销记录")
    public List<WriteOff> getByPaymentId(@PathVariable Long paymentId) {
        return writeOffService.getByPaymentId(paymentId);
    }
}
