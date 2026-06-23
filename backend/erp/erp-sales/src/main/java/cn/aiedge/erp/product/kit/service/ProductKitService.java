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

    void calculateKitCost(Long kitId);

    List<ProductKitItem> getKitItems(Long kitId);

    ProductKitItem addKitItem(Long kitId, ProductKitItem item);

    ProductKitItem updateKitItem(Long itemId, ProductKitItem item);

    void removeKitItem(Long itemId);

    BigDecimal calculateKitPrice(Long kitId);

    BigDecimal calculateKitProfitRate(Long kitId);

    Boolean checkKitAvailability(Long kitId, Long warehouseId, BigDecimal quantity);
}