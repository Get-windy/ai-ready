package cn.aiedge.agreement.mapper;

import cn.aiedge.agreement.entity.AgreementTerm;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 协议条款 Mapper（无 jsonb 列，直接用 BaseMapper 的增删改查即可）。
 *
 * <p>唯一索引 {@code uk_agreement_term_version_code (version_id, term_code) WHERE deleted = 0}
 * 保证一个版本里同一条款类别只有一个选择；服务层写条款时按"整份覆盖"处理
 * （先删该版本的未删除行、再插入本次选择），因此不依赖数据库约束报错来纠错。</p>
 */
@Mapper
public interface AgreementTermMapper extends BaseMapper<AgreementTerm> {
}
