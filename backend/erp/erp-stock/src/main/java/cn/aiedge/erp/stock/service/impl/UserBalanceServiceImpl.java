package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.UserBalance;
import cn.aiedge.erp.stock.mapper.UserBalanceMapper;
import cn.aiedge.erp.stock.service.UserBalanceService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class UserBalanceServiceImpl extends ServiceImpl<UserBalanceMapper, UserBalance> implements UserBalanceService {
}
