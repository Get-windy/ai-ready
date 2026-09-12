package cn.aiedge.erp.stock.controller;

import cn.aiedge.base.utils.SecurityUtils;
import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.stock.dto.ProductImageMatchResultVO;
import cn.aiedge.erp.stock.dto.ProductImageRowVO;
import cn.aiedge.erp.stock.entity.ProductImage;
import cn.aiedge.erp.stock.service.ProductImageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * 图片管理Controller（资料 → 商品管理 → 图片管理）
 *
 * <p>对标 ql361 多 Tab 组合页：商品图片列表（商品维度 9 列）+ 图片空间（素材库）。</p>
 */
@Slf4j
@Tag(name = "图片管理")
@RestController
@RequestMapping("/api/erp/md/image")
@RequiredArgsConstructor
public class ProductImageController {

    private final ProductImageService productImageService;

    @Operation(summary = "商品图片列表分页（商品维度）")
    @GetMapping("/page")
    public Result<IPage<ProductImageRowVO>> page(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String imageFilter,
            @RequestParam(required = false) String spec,
            @RequestParam(required = false) String model,
            @RequestParam(required = false) String origin,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        return Result.ok(productImageService.getProductImagePage(currentTenantId(), categoryId, keyword,
                imageFilter, spec, model, origin, brand, status, sortField, sortOrder, pageNum, pageSize));
    }

    @Operation(summary = "图片空间分页（素材库）")
    @GetMapping("/space-page")
    public Result<IPage<ProductImage>> spacePage(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer onlyImage,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "60") Integer pageSize) {
        return Result.ok(productImageService.getSpacePage(currentTenantId(), keyword, onlyImage, pageNum, pageSize));
    }

    @Operation(summary = "上传图片")
    @PostMapping("/upload")
    public Result<ProductImage> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Integer isMain) {
        try {
            return Result.ok(productImageService.upload(file, productId, isMain));
        } catch (IOException e) {
            log.error("图片上传失败", e);
            return Result.fail("图片上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "自动匹配（按名称/按商品货号）")
    @PostMapping("/auto-match")
    public Result<ProductImageMatchResultVO> autoMatch(
            @RequestParam(required = false, defaultValue = "NAME") String matchType) {
        return Result.ok(productImageService.autoMatch(currentTenantId(), matchType));
    }

    @Operation(summary = "选择图片：绑定素材到商品")
    @PostMapping("/bind")
    public Result<Boolean> bind(@RequestBody Map<String, Object> body) {
        Long imageId = toLong(body.get("imageId"));
        Long productId = toLong(body.get("productId"));
        Integer isMain = body.get("isMain") == null ? null : Integer.valueOf(String.valueOf(body.get("isMain")));
        return Result.ok(productImageService.bind(imageId, productId, isMain));
    }

    @Operation(summary = "搬移素材到商品")
    @PostMapping("/move")
    public Result<Integer> move(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("imageIds");
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream().map(this::toLong).toList();
        Long productId = toLong(body.get("productId"));
        return Result.ok(productImageService.move(ids, productId));
    }

    @Operation(summary = "批量删除图片")
    @PostMapping("/batch-delete")
    public Result<Integer> batchDelete(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Object> rawIds = (List<Object>) body.get("imageIds");
        List<Long> ids = rawIds == null ? List.of() : rawIds.stream().map(this::toLong).toList();
        return Result.ok(productImageService.deleteImages(ids));
    }

    @Operation(summary = "设置主图")
    @PostMapping("/{id}/main")
    public Result<Boolean> setMain(@PathVariable Long id) {
        return Result.ok(productImageService.setMain(id));
    }

    @Operation(summary = "商品图片列表（按商品ID）")
    @GetMapping("/product/{productId}")
    public Result<List<ProductImage>> listByProduct(@PathVariable Long productId) {
        return Result.ok(productImageService.listByProductId(productId));
    }

    /**
     * 图片内容访问（供 img 标签直接引用；SaTokenConfig 已放行本路径）
     */
    @Operation(summary = "图片内容访问")
    @GetMapping("/view/{id}")
    public ResponseEntity<byte[]> view(@PathVariable Long id) {
        try {
            ProductImage image = productImageService.getImage(id);
            byte[] content = productImageService.readContent(id);
            if (image == null || content == null || content.length == 0) {
                return ResponseEntity.notFound().build();
            }
            MediaType mediaType = resolveMediaType(image);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, mediaType.toString())
                    // 上传内容不可信：禁止浏览器 MIME 嗅探；SVG 可内嵌脚本，
                    // 直接打开该地址时用 CSP + sandbox 阻断同源脚本执行
                    .header("X-Content-Type-Options", "nosniff")
                    .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; sandbox")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=86400")
                    .body(content);
        } catch (IOException e) {
            log.error("图片读取失败: id={}", id, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /** 按存储扩展名判定图片 Content-Type（回退上传时的 MIME） */
    private MediaType resolveMediaType(ProductImage image) {
        String path = image.getFilePath() == null ? "" : image.getFilePath().toLowerCase();
        if (path.endsWith(".png")) return MediaType.IMAGE_PNG;
        if (path.endsWith(".gif")) return MediaType.IMAGE_GIF;
        if (path.endsWith(".webp")) return MediaType.parseMediaType("image/webp");
        if (path.endsWith(".bmp")) return MediaType.parseMediaType("image/bmp");
        if (path.endsWith(".svg")) return MediaType.parseMediaType("image/svg+xml");
        if (image.getFileType() != null && image.getFileType().startsWith("image/")) {
            return MediaType.parseMediaType(image.getFileType());
        }
        return MediaType.IMAGE_JPEG;
    }

    private Long currentTenantId() {
        Long tenantId = SecurityUtils.getCurrentTenantId();
        return tenantId == null ? 1L : tenantId;
    }

    private Long toLong(Object v) {
        if (v == null || String.valueOf(v).isBlank() || "null".equals(String.valueOf(v))) {
            return null;
        }
        return Long.valueOf(String.valueOf(v));
    }
}
