package cn.aiedge.finance.service.impl;

import cn.aiedge.finance.entity.VoucherEntry;
import cn.aiedge.finance.mapper.VoucherEntryMapper;
import cn.aiedge.finance.service.VoucherEntryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VoucherEntryServiceImpl extends ServiceImpl<VoucherEntryMapper, VoucherEntry> implements VoucherEntryService {
    
    @Override
    public List<VoucherEntry> listByVoucherId(Long voucherId) {
        return baseMapper.listByVoucherId(voucherId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveEntries(Long voucherId, List<VoucherEntry> entries) {
        int entryNo = 1;
        for (VoucherEntry entry : entries) {
            entry.setVoucherId(voucherId);
            entry.setEntryNo(entryNo++);
            this.save(entry);
        }
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteByVoucherId(Long voucherId) {
        LambdaQueryWrapper<VoucherEntry> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(VoucherEntry::getVoucherId, voucherId);
        return this.remove(wrapper);
    }
    
    @Override
    public boolean updateEntry(VoucherEntry entry) {
        return this.updateById(entry);
    }
}