package cn.aiedge.finance.service;

import cn.aiedge.finance.entity.VoucherEntry;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface VoucherEntryService extends IService<VoucherEntry> {
    
    List<VoucherEntry> listByVoucherId(Long voucherId);
    
    boolean saveEntries(Long voucherId, List<VoucherEntry> entries);
    
    boolean deleteByVoucherId(Long voucherId);
    
    boolean updateEntry(VoucherEntry entry);
}