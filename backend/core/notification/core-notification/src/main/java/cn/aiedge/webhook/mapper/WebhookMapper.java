package cn.aiedge.webhook.mapper;

import cn.aiedge.webhook.entity.Webhook;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface WebhookMapper extends BaseMapper<Webhook> {

    @Select("SELECT * FROM sys_webhook WHERE model_name = #{modelName} AND trigger_event = #{triggerEvent} AND active = true AND deleted = 0")
    List<Webhook> selectByModelAndEvent(@Param("modelName") String modelName, @Param("triggerEvent") String triggerEvent);

    @Select("SELECT * FROM sys_webhook WHERE model_name = #{modelName} AND active = true AND deleted = 0")
    List<Webhook> selectByModel(@Param("modelName") String modelName);

    @Select("SELECT * FROM sys_webhook WHERE active = true AND deleted = 0")
    List<Webhook> selectAllActive();
}