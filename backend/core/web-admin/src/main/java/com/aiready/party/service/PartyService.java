package com.aiready.party.service;

import com.aiready.party.entity.Party;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
 * 往来单位服务接口
 */
public interface PartyService extends IService<Party> {

    /**
     * 根据单位编码查询
     */
    Party getByPartyCode(String partyCode);

    /**
     * 根据单位类型查询列表
     */
    List<Party> listByPartyType(Integer partyType);

    /**
     * 根据分类ID查询列表
     */
    List<Party> listByCategoryId(Long categoryId);

    /**
     * 检查单位编码是否存在
     */
    boolean checkPartyCodeExists(String partyCode);

    /**
     * 检查单位编码是否存在（排除指定ID）
     */
    boolean checkPartyCodeExists(String partyCode, Long excludeId);

    /**
     * 更新往来单位状态
     */
    boolean updatePartyStatus(Long partyId, Integer status);

    /**
     * 根据等级查询往来单位列表
     */
    List<Party> listByPartyLevel(String partyLevel);

    /**
     * 根据状态查询往来单位列表
     */
    List<Party> listByStatus(Integer status);

    /**
     * 获取往来单位详情（包含联系人列表）
     */
    Party getPartyDetailById(Long partyId);

    /**
     * 检查往来单位是否有交易记录
     */
    boolean hasTransactions(Long partyId);
}