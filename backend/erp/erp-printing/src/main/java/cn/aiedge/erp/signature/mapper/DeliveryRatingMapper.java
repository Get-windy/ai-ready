package cn.aiedge.erp.signature.mapper;

import cn.aiedge.erp.signature.entity.DeliveryRating;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface DeliveryRatingMapper extends BaseMapper<DeliveryRating> {

    @Select("SELECT AVG(rating_score) as avg_score, COUNT(*) as total_count FROM erp_delivery_rating WHERE delivery_person_id = #{deliveryPersonId}")
    Map<String, Object> selectRatingStatsByDeliveryPerson(@Param("deliveryPersonId") Long deliveryPersonId);

    @Select("SELECT rating_score, COUNT(*) as count FROM erp_delivery_rating WHERE delivery_person_id = #{deliveryPersonId} GROUP BY rating_score ORDER BY rating_score")
    List<Map<String, Object>> selectRatingDistribution(@Param("deliveryPersonId") Long deliveryPersonId);
}