package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.ProductAttachment;
import cn.aiedge.erp.stock.service.ProductAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "产品附件管理")
@RestController
@RequestMapping("/api/erp/product/attachments")
@RequiredArgsConstructor
public class ProductAttachmentController {

    private final ProductAttachmentService productAttachmentService;

    @Operation(summary = "查询产品附件列表")
    @GetMapping("/{productId}")
    public Result<List<ProductAttachment>> getByProduct(@PathVariable Long productId) {
        return Result.ok(productAttachmentService.getByProductId(productId));
    }

    @Operation(summary = "新增附件记录")
    @PostMapping
    public Result<Boolean> create(@RequestBody ProductAttachment attachment) {
        return Result.ok(productAttachmentService.save(attachment));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(productAttachmentService.removeById(id));
    }
}
