package cn.aiedge.erp.marketing.service.impl;

import cn.aiedge.erp.marketing.entity.CommissionRecord;
import cn.aiedge.erp.marketing.mapper.CommissionRecordMapper;
import cn.aiedge.erp.marketing.service.CommissionRecordService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommissionRecordServiceImpl extends ServiceImpl<CommissionRecordMapper, CommissionRecord>
        implements CommissionRecordService {
}
