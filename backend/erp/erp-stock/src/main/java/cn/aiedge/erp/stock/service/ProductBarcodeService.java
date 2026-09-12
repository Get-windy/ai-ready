package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.dto.ProductBarcodeVO;
import cn.aiedge.erp.stock.entity.ProductBarcode;
import cn.aiedge.erp.stock.entity.ProductUnit;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductBarcodeService extends IService<ProductBarcode> {

    List<ProductBarcode> getByProductId(Long productId);

    /**
     * 商品条码分页查询（一行 = 商品 × 单位）
     *
     * @param barcodeFilter 条码过滤：ALL/空=全部，HAS=有条码，NONE=无条码，其它=按条码类型过滤
     * @param createTimeOp  新增时间比较符（&lt; = &gt; ≠ ≤ ≥，空值按 ≥）
     * @param purchaseDateOp 采购日期比较符（同上）
     */
    IPage<ProductBarcodeVO> pageBarcodes(Long categoryId, String keyword, String barcodeFilter,
                                         Integer shelfStatus, String status,
                                         String createTimeOp, String createTimeStart,
                                         String purchaseDateOp, String purchaseDateStart,
                                         String sortField, String sortOrder,
                                         int pageNum, int pageSize);

    /**
     * 导出用：按同样条件查询全部条码行
     */
    List<ProductBarcodeVO> listBarcodes(Long categoryId, String keyword, String barcodeFilter,
                                        Integer shelfStatus, String status,
                                        String createTimeOp, String createTimeStart,
                                        String purchaseDateOp, String purchaseDateStart,
                                        String sortField, String sortOrder);

    /**
     * 行级修改：维护单位行条码（含全局唯一性校验），同步既有条码表默认记录
     */
    void updateUnitBarcode(Long unitId, String barcode, String barcodeType);

    /**
     * 商品档案保存单位「落库前」校验：条码归一化 + 同一请求内查重 + 跨商品唯一性（单位行/条码表双口径）。
     * <p>
     * 必须在删除旧单位之前调用 —— 校验失败不产生任何写入，避免先删后插中途失败导致单位数据丢失。
     *
     * @param productId 商品 ID（用于排除本商品自身；新增时可为 null）
     * @param units     待保存单位（方法内会就地 trim 条码）
     */
    void validateUnitBarcodes(Long productId, List<ProductUnit> units);

    /**
     * 商品档案保存单位后统一收口：重建条码表默认记录（双口径同步）。
     * <p>
     * 条码页行级「修改」跳转商品档案（对标 ql361），因此唯一性与双写必须挂在商品保存路径，
     * 避免绕过条码页直接改档案时条码重复或条码表陈旧。
     *
     * @param productId 商品 ID
     * @param units     已落库的单位行（需带 id 与 barcode）
     */
    void syncUnitBarcodes(Long productId, List<ProductUnit> units);
}
