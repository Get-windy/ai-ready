package cn.aiedge.erp.printing.mapper;

import cn.aiedge.erp.printing.entity.SetPrintConfig;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 打印设置 Mapper（表 {@code set_print_config}）
 *
 * <p>只用 MyBatis-Plus 通用方法：该表**有** {@code tenant_id}，多租户条件由全局插件自动注入
 * （见 {@code MyBatisPlusConfig}，本表**未**加入 IGNORE_TENANT_TABLES），故这里不手写 SQL、
 * 也不需要自己拼租户条件。</p>
 */
@Mapper
public interface SetPrintConfigMapper extends BaseMapper<SetPrintConfig> {
}
