package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.BatchModifyPriceDTO;
import cn.aiedge.erp.stock.dto.GradePriceVO;
import cn.aiedge.erp.stock.dto.ProductPriceVO;
import cn.aiedge.erp.stock.entity.CustomerGradeDiscount;
import cn.aiedge.erp.stock.entity.CustomerGradePrice;
import cn.aiedge.erp.stock.entity.CustomerProductPrice;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.Map;

/**
 * 商品价格管理服务（资料 → 商品管理 → 商品价格管理，4 子标签）
 */
public interface ProductPriceService {

    // ── 子标签 1：商品价格批量修改 ──
    IPage<ProductPriceVO> pagePrices(Long categoryId, String keyword, String brand, String unitType,
                                     Long productId, Integer shelfStatus,
                                     String purchaseDateOp, String purchaseDate,
                                     String stockQtyOp, String stockQty, Boolean showHierarchy,
                                     int pageNum, int pageSize);

    List<ProductPriceVO> listPrices(Long categoryId, String keyword, String brand, String unitType,
                                    Long productId, Integer shelfStatus,
                                    String purchaseDateOp, String purchaseDate,
                                    String stockQtyOp, String stockQty, Boolean showHierarchy);

    /** 批量修改选中商品单位行的价格，返回实际更新行数 */
    int batchModifyPrices(BatchModifyPriceDTO dto);

    /** 品牌下拉 */
    List<String> listBrands();

    /** 客户级别下拉（客户档案级别 + 客户级别主数据） */
    List<String> listCustomerGrades();

    /** 价格等级名称列表（按 sort_order，用于列标题/导出表头动态渲染） */
    List<String> listGradeNames();

    /**
     * 统一取价：按「客户指定价 → 级别指定价 → 客户级别折扣 → 商品单位价」优先级解析最终售价。
     * 供销售/采购/零售等取价链路复用，保证与商品价格管理页同口径。
     */
    Map<String, Object> resolvePrice(Long customerId, Long productId, Long unitId);

    // ── 子标签 2：客户级别折扣设置 ──
    IPage<CustomerGradeDiscount> pageGradeDiscounts(String keyword, int pageNum, int pageSize);

    List<CustomerGradeDiscount> listGradeDiscounts(String keyword);

    /** 保存（id 为空新增，否则修改），级别名称在租户内唯一 */
    void saveGradeDiscount(CustomerGradeDiscount entity);

    void deleteGradeDiscount(Long id);

    // ── 子标签 3：级别指定价设置 ──
    IPage<GradePriceVO> pageLevelPrices(String gradeName, String keyword, String brand, int pageNum, int pageSize);

    List<GradePriceVO> listLevelPrices(String gradeName, String keyword, String brand);

    void saveLevelPrice(CustomerGradePrice entity);

    void deleteLevelPrices(List<Long> ids);

    // ── 子标签 4：客户指定价设置 ──
    IPage<GradePriceVO> pageCustomerPrices(Long customerId, Long productId, String keyword, String brand, int pageNum, int pageSize);

    List<GradePriceVO> listCustomerPrices(Long customerId, Long productId, String keyword, String brand);

    void saveCustomerPrice(CustomerProductPrice entity);

    void deleteCustomerPrices(List<Long> ids);

    // ── 导入 ──
    /** 导入级别指定价 / 客户指定价，返回 {success, failed, messages} */
    Map<String, Object> importRules(String type, List<Map<String, Object>> rows);
}
