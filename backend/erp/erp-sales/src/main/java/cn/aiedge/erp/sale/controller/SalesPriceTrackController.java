package cn.aiedge.erp.sale.controller;

import cn.aiedge.erp.sale.dto.SalesDetailQueryDTO;
import cn.aiedge.erp.sale.service.SalesDetailQueryService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 销售价格跟踪控制器
 * 提供销售价格跟踪的综合查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sales/price-track")
@RequiredArgsConstructor
@Tag(name = "销售价格跟踪", description = "销售价格跟踪查询接口")
public class SalesPriceTrackController {

    private final SalesDetailQueryService salesDetailQueryService;

    @GetMapping("/page")
    @Operation(summary = "分页查询销售价格跟踪")
    public Page<Map<String, Object>> page(SalesDetailQueryDTO queryDTO) {
        if (queryDTO.getCurrent() == null || queryDTO.getCurrent() <= 0) {
            queryDTO.setCurrent(1L);
        }
        if (queryDTO.getSize() == null || queryDTO.getSize() <= 0) {
            queryDTO.setSize(20L);
        }
        return salesDetailQueryService.pageDetail(queryDTO);
    }
}