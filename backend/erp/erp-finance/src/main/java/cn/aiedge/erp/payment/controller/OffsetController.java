package cn.aiedge.erp.payment.controller;

import cn.aiedge.common.exception.BusinessException;
import cn.aiedge.erp.payment.dto.OffsetCreateDTO;
import cn.aiedge.erp.payment.dto.OffsetDTO;
import cn.aiedge.erp.payment.dto.OffsetItemDTO;
import cn.aiedge.erp.payment.entity.Offset;
import cn.aiedge.erp.payment.entity.OffsetItem;
import cn.aiedge.erp.payment.service.OffsetService;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/erp/offset")
@RequiredArgsConstructor
@Tag(name = "往来对冲", description = "同一单位的应收应付对冲管理")
public class OffsetController {

    private final OffsetService offsetService;

    @GetMapping("/page")
    @Operation(summary = "分页查询对冲单")
    public Page<OffsetDTO> page(
            @Parameter(description = "关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "对方类型") @RequestParam(required = false) String partyType,
            @Parameter(description = "对方ID") @RequestParam(required = false) Long partyId,
            @Parameter(description = "状态") @RequestParam(required = false) String status,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") int pageNum,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") int pageSize) {
        Page<Offset> page = offsetService.pageList(keyword, partyType, partyId, status, pageNum, pageSize);
        Page<OffsetDTO> voPage = new Page<>(pageNum, pageSize, page.getTotal());
        voPage.setRecords(page.getRecords().stream().map(this::convertToDTO).collect(Collectors.toList()));
        return voPage;
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取对冲单详情")
    public OffsetDTO getById(@PathVariable Long id) {
        Offset offset = offsetService.getById(id);
        if (offset == null) {
            throw BusinessException.notFound("对冲单不存在");
        }
        OffsetDTO dto = convertToDTO(offset);
        dto.setItems(offsetService.getItems(id).stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return dto;
    }

    @PostMapping
    @Operation(summary = "创建对冲单")
    public OffsetDTO create(@Valid @RequestBody OffsetCreateDTO dto) {
        Offset offset = new Offset();
        BeanUtils.copyProperties(dto, offset);
        offset.setTenantId(1L);
        offset.setCreateBy(StpUtil.getLoginIdAsLong());

        List<OffsetItem> items = dto.getItems().stream().map(itemDTO -> {
            OffsetItem item = new OffsetItem();
            BeanUtils.copyProperties(itemDTO, item);
            return item;
        }).collect(Collectors.toList());

        Offset created = offsetService.createOffset(offset, items);
        OffsetDTO result = convertToDTO(created);
        result.setItems(offsetService.getItems(created.getId()).stream().map(this::convertItemToDTO).collect(Collectors.toList()));
        return result;
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "完成对冲")
    public OffsetDTO complete(@PathVariable Long id) {
        Offset offset = offsetService.completeOffset(id);
        return convertToDTO(offset);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "取消对冲")
    public OffsetDTO cancel(@PathVariable Long id, @RequestParam String reason) {
        Offset offset = offsetService.cancelOffset(id, reason);
        return convertToDTO(offset);
    }

    private OffsetDTO convertToDTO(Offset offset) {
        OffsetDTO dto = new OffsetDTO();
        BeanUtils.copyProperties(offset, dto);
        return dto;
    }

    private OffsetItemDTO convertItemToDTO(OffsetItem item) {
        OffsetItemDTO dto = new OffsetItemDTO();
        BeanUtils.copyProperties(item, dto);
        return dto;
    }
}
