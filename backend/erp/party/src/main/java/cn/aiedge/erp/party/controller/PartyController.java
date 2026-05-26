package cn.aiedge.erp.party.controller;

import cn.aiedge.common.result.ApiResponse;
import cn.aiedge.common.result.PageResult;
import cn.aiedge.erp.party.dto.PartyDTO;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "往来单位管理", description = "往来单位CRUD接口")
@RestController
@RequestMapping("/api/erp/party")
@RequiredArgsConstructor
public class PartyController {

    private final PartyService partyService;

    @Operation(summary = "获取往来单位详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Party>> getById(@PathVariable Long id) {
        Party party = partyService.getById(id);
        return ResponseEntity.ok(ApiResponse.ok(party));
    }

    @Operation(summary = "根据单位编码查询")
    @GetMapping("/by-code/{partyCode}")
    public ResponseEntity<ApiResponse<Party>> getByPartyCode(@PathVariable String partyCode) {
        Party party = partyService.getByPartyCode(partyCode);
        return ResponseEntity.ok(ApiResponse.ok(party));
    }

    @Operation(summary = "根据单位类型查询列表")
    @GetMapping("/by-type/{partyType}")
    public ResponseEntity<ApiResponse<List<Party>>> listByPartyType(@PathVariable Integer partyType) {
        List<Party> parties = partyService.listByPartyType(partyType);
        return ResponseEntity.ok(ApiResponse.ok(parties));
    }

    @Operation(summary = "根据分类ID查询列表")
    @GetMapping("/by-category/{categoryId}")
    public ResponseEntity<ApiResponse<List<Party>>> listByCategoryId(@PathVariable Long categoryId) {
        List<Party> parties = partyService.listByCategoryId(categoryId);
        return ResponseEntity.ok(ApiResponse.ok(parties));
    }

    @Operation(summary = "根据状态查询列表")
    @GetMapping("/by-status/{status}")
    public ResponseEntity<ApiResponse<List<Party>>> listByStatus(@PathVariable Integer status) {
        List<Party> parties = partyService.listByStatus(status);
        return ResponseEntity.ok(ApiResponse.ok(parties));
    }

    @Operation(summary = "分页查询往来单位")
    @GetMapping("/page")
    public ResponseEntity<ApiResponse<PageResult<Party>>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer partyType,
            @RequestParam(required = false) Integer status) {
        Page<Party> page = new Page<>(current, size);
        LambdaQueryWrapper<Party> wrapper = new LambdaQueryWrapper<>();
        if (partyType != null) {
            wrapper.eq(Party::getPartyType, partyType);
        }
        if (status != null) {
            wrapper.eq(Party::getStatus, status);
        }
        wrapper.eq(Party::getDeleted, 0);
        wrapper.orderByDesc(Party::getCreateTime);
        
        Page<Party> result = partyService.page(page, wrapper);
        PageResult<Party> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(result.getCurrent());
        pageResult.setPageSize(result.getSize());
        
        return ResponseEntity.ok(ApiResponse.ok(pageResult));
    }

    @Operation(summary = "创建往来单位")
    @PostMapping
    public ResponseEntity<ApiResponse<Boolean>> create(@RequestBody Party party) {
        boolean success = partyService.save(party);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "更新往来单位")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> update(@PathVariable Long id, @RequestBody Party party) {
        party.setId(id);
        boolean success = partyService.updateById(party);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "更新往来单位状态")
    @PutMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Boolean>> updateStatus(
            @PathVariable Long id,
            @RequestParam Integer status) {
        boolean success = partyService.updatePartyStatus(id, status);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "删除往来单位")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Boolean>> delete(@PathVariable Long id) {
        boolean success = partyService.removeById(id);
        return ResponseEntity.ok(ApiResponse.ok(success));
    }

    @Operation(summary = "检查往来单位是否有交易记录")
    @GetMapping("/{id}/has-transactions")
    public ResponseEntity<ApiResponse<Boolean>> hasTransactions(@PathVariable Long id) {
        boolean hasTransactions = partyService.hasTransactions(id);
        return ResponseEntity.ok(ApiResponse.ok(hasTransactions));
    }
}
