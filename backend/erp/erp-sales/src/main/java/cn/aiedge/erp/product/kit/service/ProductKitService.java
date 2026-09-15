package cn.aiedge.erp.product.kit.service;

import cn.aiedge.erp.product.kit.entity.ProductKit;
import cn.aiedge.erp.product.kit.entity.ProductKitItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface ProductKitService extends IService<ProductKit> {

    ProductKit getByKitCode(String kitCode);

    Page<ProductKit> pageList(String keyword, Integer kitType, Integer status, int pageNum, int pageSize);

    List<ProductKit> listActiveKits();

    List<ProductKit> listByKitType(Integer kitType);

    String generateKitCode();

    ProductKit createKit(ProductKit kit, List<ProductKitItem> items);

    ProductKit updateKit(Long kitId, ProductKit kit, List<ProductKitItem> items);

    ProductKit copyKit(Long kitId);

    void activateKit(Long kitId);

    void deactivateKit(Long kitId);

    /**
     * 批量激活套装
     *
     * @param ids 套装ID列表（空/全不存在返回 0）
     * @return 实际更新条数
     */
    int activateKits(List<Long> ids);

    /**
     * 批量停用套装
     *
     * @param ids 套装ID列表（空/全不存在返回 0）
     * @return 实际更新条数
     */
    int deactivateKits(List<Long> ids);

    void calculateKitCost(Long kitId);

    List<ProductKitItem> getKitItems(Long kitId);

    ProductKitItem addKitItem(Long kitId, ProductKitItem item);

    ProductKitItem updateKitItem(Long itemId, ProductKitItem item);

    void removeKitItem(Long itemId);

    /**
     * 删除套装（级联逻辑删除其组件行）
     *
     * @throws RuntimeException 套装不存在
     */
    void deleteKit(Long kitId);

    /**
     * 批量删除套装（级联逻辑删除组件行）
     *
     * @param ids 套装ID列表（空/全不存在返回 0）
     * @return 实际删除条数
     */
    int deleteKits(List<Long> ids);

    BigDecimal calculateKitPrice(Long kitId);

    BigDecimal calculateKitProfitRate(Long kitId);

    Boolean checkKitAvailability(Long kitId, Long warehouseId, BigDecimal quantity);

    /**
     * 批量获取套餐商品明细摘要（商品列表「套餐」子标签的"商品明细"列）
     *
     * @return kitId → "组件名×数量；..." 的映射
     */
    java.util.Map<Long, String> itemsSummary(List<Long> kitIds);
}