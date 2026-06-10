package cn.aiedge.erp.payment.service;

import cn.aiedge.erp.payment.entity.Offset;
import cn.aiedge.erp.payment.entity.OffsetItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface OffsetService extends IService<Offset> {

    Offset getByOffsetNo(String offsetNo);

    Page<Offset> pageList(String keyword, String partyType, Long partyId, String status, int pageNum, int pageSize);

    Offset createOffset(Offset offset, List<OffsetItem> items);

    Offset completeOffset(Long id);

    Offset cancelOffset(Long id, String reason);

    List<OffsetItem> getItems(Long offsetId);

    String generateOffsetNo();
}
