package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.GroupBuyActivity;
import cn.aiedge.erp.stock.mapper.GroupBuyActivityMapper;
import cn.aiedge.erp.stock.service.GroupBuyActivityService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(rollbackFor = Exception.class)
public class GroupBuyActivityServiceImpl extends ServiceImpl<GroupBuyActivityMapper, GroupBuyActivity> implements GroupBuyActivityService {
}
