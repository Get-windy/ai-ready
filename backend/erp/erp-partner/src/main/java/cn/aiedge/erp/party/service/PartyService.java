package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.Party;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.time.LocalDate;
import java.util.List;

public interface PartyService extends IService<Party> {

    Party getByPartyCode(String partyCode);

    List<Party> listByPartyType(Integer partyType);

    List<Party> listByCategoryId(Long categoryId);

    boolean checkPartyCodeExists(String partyCode);

    boolean checkPartyCodeExists(String partyCode, Long excludeId);

    boolean updatePartyStatus(Long partyId, Integer status);

    List<Party> listByPartyLevel(String partyLevel);

    List<Party> listByStatus(Integer status);

    Party getPartyDetailById(Long partyId);

    boolean hasTransactions(Long partyId);

    /**
     * 分页查询往来单位
     */
    IPage<Party> getPartyPage(String keyword, Integer partyType, Integer status,
                              Long categoryId, String settleType, String region,
                              String handler, String address,
                              LocalDate createTimeStart, LocalDate createTimeEnd,
                              LocalDate lastTradeStart, LocalDate lastTradeEnd,
                              Integer pageNum, Integer pageSize);

    /**
     * 搜索往来单位（下拉选择用）
     */
    List<Party> search(String keyword, Integer partyType);

    /**
     * 获取往来单位列表（不分页）
     */
    List<Party> getPartyList(Integer partyType, Integer status, Integer pageSize);

    /**
     * 根据前缀获取下一个编号序号
     */
    Integer getNextSeq(String prefix);
}
