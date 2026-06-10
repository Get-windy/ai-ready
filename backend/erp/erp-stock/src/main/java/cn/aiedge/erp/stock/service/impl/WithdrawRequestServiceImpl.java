package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.WithdrawRequest;
import cn.aiedge.erp.stock.mapper.WithdrawRequestMapper;
import cn.aiedge.erp.stock.service.WithdrawRequestService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class WithdrawRequestServiceImpl extends ServiceImpl<WithdrawRequestMapper, WithdrawRequest> implements WithdrawRequestService {
}
