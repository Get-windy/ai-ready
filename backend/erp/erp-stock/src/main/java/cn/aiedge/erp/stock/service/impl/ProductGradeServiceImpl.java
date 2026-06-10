package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductGrade;
import cn.aiedge.erp.stock.mapper.ProductGradeMapper;
import cn.aiedge.erp.stock.service.ProductGradeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品等级Service实现
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class ProductGradeServiceImpl extends ServiceImpl<ProductGradeMapper, ProductGrade>
        implements ProductGradeService {

    @Override
    public List<ProductGrade> getActiveGrades() {
        return list(new LambdaQueryWrapper<ProductGrade>()
                .eq(ProductGrade::getStatus, 1)
                .eq(ProductGrade::getDeleted, 0)
                .orderByDesc(ProductGrade::getGradeLevel));
    }
}
