package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.erp.party.entity.PartyAttachment;
import cn.aiedge.erp.party.service.PartyAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 往来单位附件管理（基础资料表单「上传附件」分区 + 列表「附件」列）
 */
@Tag(name = "往来单位附件", description = "供应商/客户/物流公司等基础资料附件")
@RestController
@RequestMapping("/api/erp/partner/attachments")
@RequiredArgsConstructor
public class PartyAttachmentController {

    private final PartyAttachmentService partyAttachmentService;

    @Operation(summary = "查询某往来单位的附件列表")
    @GetMapping("/by-partner/{partyId}")
    public ResponseEntity<ApiResponse<List<PartyAttachment>>> listByPartner(@PathVariable Long partyId) {
        return ResponseEntity.ok(ApiResponse.ok(partyAttachmentService.listByPartnerId(partyId)));
    }

    @Operation(summary = "新增附件记录")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(@RequestBody PartyAttachment attachment) {
        return ResponseEntity.ok(ApiResponse.ok(partyAttachmentService.save(attachment)));
    }

    @Operation(summary = "删除附件")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(partyAttachmentService.removeById(id)));
    }
}
