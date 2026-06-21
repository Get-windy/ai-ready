package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.PartnerAttachment;
import cn.aiedge.erp.stock.service.PartnerAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@Tag(name = "往来单位附件管理")
@RestController
@RequestMapping("/api/erp/partner/attachments")
@RequiredArgsConstructor
public class PartnerAttachmentController {

    private final PartnerAttachmentService partnerAttachmentService;

    @Operation(summary = "查询附件列表")
    @GetMapping("/{partnerId}")
    public Result<List<PartnerAttachment>> getByPartner(@PathVariable Long partnerId) {
        return Result.ok(partnerAttachmentService.getByPartnerId(partnerId));
    }

    @Operation(summary = "新增附件")
    @PostMapping
    public Result<Boolean> create(@RequestBody PartnerAttachment attachment) {
        return Result.ok(partnerAttachmentService.save(attachment));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return Result.ok(partnerAttachmentService.removeById(id));
    }
}
