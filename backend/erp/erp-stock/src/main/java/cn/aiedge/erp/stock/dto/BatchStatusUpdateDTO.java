package cn.aiedge.erp.stock.dto;

import lombok.Data;

import java.util.List;

/**
 * 批量更新商品状态 DTO
 */
@Data
public class BatchStatusUpdateDTO {

    /**
     * 待更新的商品ID列表
     */
    private List<Long> ids;

    /**
     * 目标状态: ENABLED / DISABLED
     */
    private String status;
}
