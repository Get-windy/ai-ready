package cn.aiedge.erp.stock.service;

import cn.aiedge.erp.stock.entity.ProductAttachment;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface ProductAttachmentService extends IService<ProductAttachment> {
    List<ProductAttachment> getByProductId(Long productId);
}
