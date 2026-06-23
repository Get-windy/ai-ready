package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.Party;
import com.baomidou.mybatisplus.extension.service.IService;

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
}
