package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductGrade;
import cn.aiedge.erp.stock.mapper.ProductGradeMapper;
import cn.aiedge.erp.stock.service.ProductGradeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 产品等级Service实现
 *
 * <p>价格等级是全局标准槽位（GRADE_1..GRADE_8，种子 tenant_id=0），
 * 多租户插件会过滤掉这些行，故统一走 Mapper 中忽略租户的方法。</p>
 */
@Transactional(rollbackFor = Exception.class)
@Service
public class ProductGradeServiceImpl extends ServiceImpl<ProductGradeMapper, ProductGrade>
        implements ProductGradeService {

    @Override
    public List<ProductGrade> getActiveGrades() {
        // 按 sortOrder/gradeLevel 升序返回，保证「商品单位」明细表第 N 个等级价列
        // 与第 N 个标准等级（grade_price_N）严格对应
        return baseMapper.selectActiveGradesIgnoreTenant();
    }

    @Override
    public List<ProductGrade> getAllGrades() {
        return baseMapper.selectAllIgnoreTenant();
    }

    @Override
    public boolean updateGrade(ProductGrade grade) {
        return baseMapper.updateIgnoreTenant(grade) > 0;
    }

    @Override
    public boolean deleteGrade(Long id) {
        return baseMapper.deleteIgnoreTenant(id) > 0;
    }
}
