package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.CustomerGradePrice;
import com.baomidou.mybatisplus.extension.service.IService;

import java.math.BigDecimal;
import java.util.List;

public interface CustomerGradePriceService extends IService<CustomerGradePrice> {

    CustomerGradePrice getByGradeAndProduct(Long gradeId, Long productId);

    CustomerGradePrice getByGradeAndSku(Long gradeId, Long skuId);

    List<CustomerGradePrice> listByProduct(Long productId);

    List<CustomerGradePrice> listByGrade(Long gradeId);

    BigDecimal calculateGradePrice(Long gradeId, Long productId, BigDecimal standardPrice);

    boolean batchSetGradePrice(Long gradeId, List<Long> productIds, BigDecimal price);

    boolean batchSetDiscountRate(Long gradeId, List<Long> productIds, BigDecimal discountRate);

    boolean copyGradePrice(Long sourceGradeId, Long targetGradeId);

    boolean syncGradePrice(Long gradeId);

    boolean existsPriceSetting(Long gradeId, Long productId);

    CustomerGradePrice getDefaultPrice(Long productId);

    boolean setDefaultPrice(Long id);

    List<CustomerGradePrice> listActivePrices(Long gradeId);

    CustomerGradePrice getPriceByPriority(Long gradeId, Long productId);

    boolean validatePriceRange(Long gradeId, Long productId, BigDecimal price);

    boolean checkPriceExists(Long gradeId, Long productId);

    boolean checkPriceExists(Long gradeId, Long productId, Long excludeId);

    boolean updatePriceStatus(Long id, Integer status);

    List<CustomerGradePrice> getActivePrices(Long productId);
}
