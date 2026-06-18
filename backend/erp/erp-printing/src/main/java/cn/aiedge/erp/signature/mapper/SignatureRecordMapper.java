package cn.aiedge.erp.signature.mapper;

import cn.aiedge.erp.signature.entity.SignatureRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SignatureRecordMapper extends BaseMapper<SignatureRecord> {

    @Select("SELECT * FROM erp_signature_record WHERE order_no = #{orderNo} ORDER BY sign_time DESC LIMIT 1")
    SignatureRecord selectLatestByOrderNo(@Param("orderNo") String orderNo);

    @Select("SELECT COUNT(*) FROM erp_signature_record WHERE delivery_person_id = #{deliveryPersonId} AND sign_time BETWEEN #{startTime} AND #{endTime}")
    int countByDeliveryPersonAndTime(@Param("deliveryPersonId") String deliveryPersonId,
                                      @Param("startTime") String startTime,
                                      @Param("endTime") String endTime);
}