package cn.aiedge.platform.mapper;

import cn.aiedge.platform.model.SmsConfig;
import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SmsConfigMapper extends BaseMapper<SmsConfig> {

    /**
     * 取当前生效的短信配置（平台级全局优先）。口径与
     * {@link MailConfigMapper#selectEffective()} 完全一致，详见那里的注释。
     * 本表**没有** `deleted` 列（实测 9 列）。
     */
    @InterceptorIgnore(tenantLine = "true")
    @Select("SELECT * FROM sys_sms_config ORDER BY (tenant_id = 0) DESC, id ASC LIMIT 1")
    SmsConfig selectEffective();
}
