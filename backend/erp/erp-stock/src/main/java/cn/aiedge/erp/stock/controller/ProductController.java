package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.entity.Product;
import cn.aiedge.erp.stock.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 产品Controller
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Tag(name = "产品管理", description = "产品查询接口")
@RestController
@RequestMapping("/api/product")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(summary = "查询产品列表")
    @GetMapping("/list")
    public Result<List<Product>> list() {
        List<Product> list = productService.getProductList();
        return Result.ok(list);
    }
}
