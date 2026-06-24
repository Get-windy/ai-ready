package cn.aiedge.trade.mapper;

import cn.aiedge.trade.entity.ExternalOrderRaw;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExternalOrderRawMapper extends BaseMapper<ExternalOrderRaw> {

    @Select("SELECT * FROM external_order_raw WHERE process_status = #{status} ORDER BY receive_time ASC LIMIT #{limit}")
    List<ExternalOrderRaw> selectPending(int status, int limit);

    @Select("SELECT COUNT(*) FROM external_order_raw WHERE process_status = 0 AND channel_code = #{channelCode}")
    int countPending(String channelCode);

    @Select("SELECT COUNT(*) > 0 FROM external_order_raw WHERE channel_code = #{channelCode} AND external_order_id = #{externalOrderId}")
    boolean existsByExternalId(String channelCode, String externalOrderId);
}