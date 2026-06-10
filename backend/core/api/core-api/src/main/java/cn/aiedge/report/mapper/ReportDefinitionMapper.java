package cn.aiedge.report.mapper;

import cn.aiedge.report.model.ReportDefinitionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 自定义报表定义Mapper
 * <p>
 * 提供对 report_definition 表的基本CRUD操作，
 * 基于 MyBatis-Plus BaseMapper，无需手写SQL。
 *
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface ReportDefinitionMapper extends BaseMapper<ReportDefinitionEntity> {
}
