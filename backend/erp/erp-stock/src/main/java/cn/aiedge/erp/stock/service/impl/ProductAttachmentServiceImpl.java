package cn.aiedge.erp.stock.service.impl;

import cn.aiedge.erp.stock.entity.ProductAttachment;
import cn.aiedge.erp.stock.mapper.ProductAttachmentMapper;
import cn.aiedge.erp.stock.service.ProductAttachmentService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional(rollbackFor = Exception.class)
@Service
public class ProductAttachmentServiceImpl extends ServiceImpl<ProductAttachmentMapper, ProductAttachment>
        implements ProductAttachmentService {

    @Override
    public List<ProductAttachment> getByProductId(Long productId) {
        return lambdaQuery()
                .eq(ProductAttachment::getProductId, productId)
                .eq(ProductAttachment::getDeleted, 0)
                .orderByAsc(ProductAttachment::getSortOrder)
                .list();
    }
}
