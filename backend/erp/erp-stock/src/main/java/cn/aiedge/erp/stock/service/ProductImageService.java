package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.ProductImageMatchResultVO;
import cn.aiedge.erp.stock.dto.ProductImageRowVO;
import cn.aiedge.erp.stock.entity.ProductImage;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/**
 * 图片管理服务（商品图片列表 + 图片空间）
 */
public interface ProductImageService {

    /**
     * 商品图片列表分页（商品维度）
     *
     * @param imageFilter ALL 全部 / HAS 已上传图片 / NONE 未上传图片
     * @param sortField   PRODUCT_CODE 按货号 / PRODUCT_NAME 按名称
     * @param sortOrder   ASC 顺序 / DESC 倒序
     */
    IPage<ProductImageRowVO> getProductImagePage(Long tenantId, Long categoryId, String keyword,
                                                 String imageFilter, String spec, String model,
                                                 String origin, String brand, String status,
                                                 String sortField, String sortOrder,
                                                 Integer pageNum, Integer pageSize);

    /**
     * 图片空间分页（素材库）
     *
     * @param onlyImage 1=只显示图片类型素材
     */
    IPage<ProductImage> getSpacePage(Long tenantId, String keyword, Integer onlyImage,
                                     Integer pageNum, Integer pageSize);

    /**
     * 上传图片（productId 为空即图片空间素材）
     */
    ProductImage upload(MultipartFile file, Long productId, Integer isMain) throws IOException;

    /**
     * 自动匹配：按图片名称（按名称）/ 商品货号（按货号）把素材关联到商品
     *
     * @param matchType NAME / CODE
     */
    ProductImageMatchResultVO autoMatch(Long tenantId, String matchType);

    /**
     * 把素材图片绑定到商品（商品图片列表行内「选择图片」）
     */
    boolean bind(Long imageId, Long productId, Integer isMain);

    /**
     * 批量搬移素材到指定商品（图片空间「搬移」）
     */
    int move(List<Long> imageIds, Long productId);

    /**
     * 批量删除图片（记录 + 物理文件）
     */
    int deleteImages(List<Long> ids);

    /**
     * 设置主图（同时回写商品主图 image_url）
     */
    boolean setMain(Long imageId);

    /**
     * 读取图片二进制内容（供 img 直接访问）
     */
    byte[] readContent(Long imageId) throws IOException;

    /**
     * 查询图片元数据（访问时判定 Content-Type）
     */
    ProductImage getImage(Long imageId);

    /**
     * 查询商品图片（供其它模块引用图片素材）
     */
    List<ProductImage> listByProductId(Long productId);
}
