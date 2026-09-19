package cn.aiedge.erp.finance.initial.mapper;

import cn.aiedge.erp.finance.initial.entity.InitialFinanceSubject;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 财务期初（按科目）Mapper。
 *
 * <p>本页的查询/写库全部走 MyBatis-Plus 的 Wrapper（含租户、逻辑删除均由插件注入），
 * **没有手写 SQL** —— 因此不存在「手写 SQL 漏租户条件」的风险。</p>
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface InitialFinanceSubjectMapper extends BaseMapper<InitialFinanceSubject> {
}
