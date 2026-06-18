package cn.aiedge.erp.batchsn.mapper;

import cn.aiedge.erp.batchsn.entity.BatchRule;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 批次规则配置数据访问接口
 * 
 * @author devops-engineer
 * @date 2026-05-05
 */
@Mapper
public interface BatchRuleMapper extends BaseMapper<BatchRule> {
    
    /**
     * 根据规则编码查询批次规则
     */
    BatchRule selectByRuleCode(@Param("ruleCode") String ruleCode);
    
    /**
     * 查询所有启用的批次规则
     */
    List<BatchRule> selectEnabledRules();
    
    /**
     * 查询适用于指定产品分类的批次规则
     */
    List<BatchRule> selectRulesForProductCategory(@Param("categoryId") Long categoryId);
    
    /**
     * 更新批次规则状态
     */
    int updateRuleStatus(@Param("ruleId") Long ruleId, @Param("enable") Boolean enable, @Param("updatedBy") String updatedBy);
    
    /**
     * 根据产品ID查找适用的批次规则
     */
    BatchRule findApplicableRuleForProduct(@Param("productId") Long productId, @Param("categoryId") Long categoryId);
    
    /**
     * 查询需要质检的批次规则
     */
    List<BatchRule> selectRulesRequiringQualityCheck();
}