package cn.aiedge.hr;

import cn.aiedge.hr.salary.HrSalaryPayment;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface HrSalaryPaymentMapper extends BaseMapper<HrSalaryPayment> {
    List<HrSalaryPayment> selectByEmployeeId(@Param("employeeId") Long employeeId);
}
