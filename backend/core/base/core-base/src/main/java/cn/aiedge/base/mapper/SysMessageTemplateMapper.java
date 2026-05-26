package cn.aiedge.base.mapper;

import cn.aiedge.base.entity.SysMessageTemplate;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 消息模板Mapper
 * 
 * @author AI-Ready Team
 * @since 1.0.0
 */
@Mapper
public interface SysMessageTemplateMapper extends BaseMapper<SysMessageTemplate> {

    /**
     * 根据模板编码查询
     */
    SysMessageTemplate selectByCode(@Param("templateCode") String templateCode, 
                                     @Param("tenantId") Long tenantId);
}
