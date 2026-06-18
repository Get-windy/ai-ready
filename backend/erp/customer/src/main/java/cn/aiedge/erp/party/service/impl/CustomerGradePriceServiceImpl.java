package cn.aiedge.erp.party.service.impl;

import cn.aiedge.erp.party.entity.CustomerGrade;
import cn.aiedge.erp.party.entity.CustomerGradePrice;
import cn.aiedge.erp.party.mapper.CustomerGradePriceMapper;
import cn.aiedge.erp.party.service.CustomerGradePriceService;
import cn.aiedge.erp.party.service.CustomerGradeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomerGradePriceServiceImpl extends ServiceImpl<CustomerGradePriceMapper, CustomerGradePrice> implements CustomerGradePriceService {

    @Autowired
    private CustomerGradeService customerGradeService;

    @Override
    public CustomerGradePrice getByGradeAndProduct(Long gradeId, Long productId) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByDesc(CustomerGradePrice::getPriority)
                .last("LIMIT 1")
        );
    }

    @Override
    public CustomerGradePrice getByGradeAndSku(Long gradeId, Long skuId) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getSkuId, skuId)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByDesc(CustomerGradePrice::getPriority)
                .last("LIMIT 1")
        );
    }

    @Override
    public List<CustomerGradePrice> listByProduct(Long productId) {
        return this.list(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByAsc(CustomerGradePrice::getGradeId)
        );
    }

    @Override
    public List<CustomerGradePrice> listByGrade(Long gradeId) {
        return this.list(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByAsc(CustomerGradePrice::getProductId)
        );
    }

    @Override
    public BigDecimal calculateGradePrice(Long gradeId, Long productId, BigDecimal standardPrice) {
        CustomerGradePrice gradePrice = getByGradeAndProduct(gradeId, productId);
        if (gradePrice == null) {
            return standardPrice;
        }

        if (gradePrice.getPriceType() == 1) {
            return gradePrice.getGradePrice();
        } else if (gradePrice.getPriceType() == 2) {
            BigDecimal discountRate = gradePrice.getDiscountRate();
            if (discountRate == null) {
                discountRate = customerGradeService.getDiscountRate(gradeId);
            }
            return standardPrice.multiply(discountRate).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        }

        return standardPrice;
    }

    @Override
    @Transactional
    public boolean batchSetGradePrice(Long gradeId, List<Long> productIds, BigDecimal price) {
        for (Long productId : productIds) {
            CustomerGradePrice gradePrice = getByGradeAndProduct(gradeId, productId);
            if (gradePrice == null) {
                gradePrice = new CustomerGradePrice();
                gradePrice.setGradeId(gradeId);
                gradePrice.setProductId(productId);
                gradePrice.setPriceType(1);
                gradePrice.setGradePrice(price);
                gradePrice.setStatus(1);
                gradePrice.setDeleted(0);
                this.save(gradePrice);
            } else {
                gradePrice.setGradePrice(price);
                this.updateById(gradePrice);
            }
        }
        return true;
    }

    @Override
    @Transactional
    public boolean batchSetDiscountRate(Long gradeId, List<Long> productIds, BigDecimal discountRate) {
        for (Long productId : productIds) {
            CustomerGradePrice gradePrice = getByGradeAndProduct(gradeId, productId);
            if (gradePrice == null) {
                gradePrice = new CustomerGradePrice();
                gradePrice.setGradeId(gradeId);
                gradePrice.setProductId(productId);
                gradePrice.setPriceType(2);
                gradePrice.setDiscountRate(discountRate);
                gradePrice.setStatus(1);
                gradePrice.setDeleted(0);
                this.save(gradePrice);
            } else {
                gradePrice.setPriceType(2);
                gradePrice.setDiscountRate(discountRate);
                this.updateById(gradePrice);
            }
        }
        return true;
    }

    @Override
    @Transactional
    public boolean copyGradePrice(Long sourceGradeId, Long targetGradeId) {
        List<CustomerGradePrice> sourcePrices = listByGrade(sourceGradeId);
        List<CustomerGradePrice> targetPrices = new ArrayList<>();

        for (CustomerGradePrice sourcePrice : sourcePrices) {
            CustomerGradePrice targetPrice = new CustomerGradePrice();
            targetPrice.setGradeId(targetGradeId);
            targetPrice.setProductId(sourcePrice.getProductId());
            targetPrice.setSkuId(sourcePrice.getSkuId());
            targetPrice.setPriceType(sourcePrice.getPriceType());
            targetPrice.setGradePrice(sourcePrice.getGradePrice());
            targetPrice.setDiscountRate(sourcePrice.getDiscountRate());
            targetPrice.setStatus(1);
            targetPrice.setDeleted(0);
            targetPrices.add(targetPrice);
        }

        return this.saveBatch(targetPrices);
    }

    @Override
    @Transactional
    public boolean syncGradePrice(Long gradeId) {
        List<CustomerGradePrice> prices = listByGrade(gradeId);
        for (CustomerGradePrice price : prices) {
            if (price.getPriceType() == 2 && price.getDiscountRate() != null) {
            }
        }
        return true;
    }

    @Override
    public boolean existsPriceSetting(Long gradeId, Long productId) {
        Long count = this.count(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getDeleted, 0)
        );
        return count != null && count > 0;
    }

    @Override
    public CustomerGradePrice getDefaultPrice(Long productId) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getIsDefault, true)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .last("LIMIT 1")
        );
    }

    @Override
    @Transactional
    public boolean setDefaultPrice(Long id) {
        CustomerGradePrice price = this.getById(id);
        if (price == null) {
            return false;
        }

        this.update(
            new LambdaUpdateWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getProductId, price.getProductId())
                .eq(CustomerGradePrice::getIsDefault, true)
                .set(CustomerGradePrice::getIsDefault, false)
        );

        price.setIsDefault(true);
        return this.updateById(price);
    }

    @Override
    public List<CustomerGradePrice> listActivePrices(Long gradeId) {
        return this.list(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByAsc(CustomerGradePrice::getProductId)
        );
    }

    @Override
    public CustomerGradePrice getPriceByPriority(Long gradeId, Long productId) {
        return this.getOne(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByDesc(CustomerGradePrice::getPriority)
                .last("LIMIT 1")
        );
    }

    @Override
    public boolean validatePriceRange(Long gradeId, Long productId, BigDecimal price) {
        CustomerGrade grade = customerGradeService.getById(gradeId);
        if (grade == null) {
            return false;
        }

        BigDecimal minPrice = grade.getMinPrice();
        BigDecimal maxPrice = grade.getMaxPrice();

        if (minPrice != null && price.compareTo(minPrice) < 0) {
            return false;
        }

        if (maxPrice != null && price.compareTo(maxPrice) > 0) {
            return false;
        }

        return true;
    }

    @Override
    public boolean checkPriceExists(Long gradeId, Long productId) {
        Long count = this.count(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getDeleted, 0)
        );
        return count != null && count > 0;
    }

    @Override
    public boolean checkPriceExists(Long gradeId, Long productId, Long excludeId) {
        Long count = this.count(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getGradeId, gradeId)
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getDeleted, 0)
                .ne(CustomerGradePrice::getId, excludeId)
        );
        return count != null && count > 0;
    }

    @Override
    @Transactional
    public boolean updatePriceStatus(Long id, Integer status) {
        CustomerGradePrice price = this.getById(id);
        if (price == null) {
            return false;
        }
        price.setStatus(status);
        return this.updateById(price);
    }

    @Override
    public List<CustomerGradePrice> getActivePrices(Long productId) {
        return this.list(
            new LambdaQueryWrapper<CustomerGradePrice>()
                .eq(CustomerGradePrice::getProductId, productId)
                .eq(CustomerGradePrice::getStatus, 1)
                .eq(CustomerGradePrice::getDeleted, 0)
                .orderByAsc(CustomerGradePrice::getGradeId)
        );
    }
}
