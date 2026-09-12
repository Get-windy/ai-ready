package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.dto.PartyContactRow;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.mapper.PartyQueryParam;
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
     * 分页查询往来单位（支持多重身份合并）。
     * <p>
     * showAsCustomer=true 时，除 party_type=partyType 的记录外，额外纳入
     * roles 中包含该类型身份的往来单位（对标「显示客户中的供应商」/「显示供应商中的客户」）。
     * </p>
     */
    IPage<Party> getPartyPageByRole(String keyword, Integer partyType, Integer status,
                                    Long categoryId, String settleType,
                                    LocalDate createTimeStart, LocalDate createTimeEnd,
                                    Boolean showAsCustomer, Integer pageNum, Integer pageSize);

    /**
     * 客户列表分页（资料 → 往来单位 → 客户 → 全部客户子标签，18 项查询条件全量生效）
     */
    IPage<Party> getCustomerPage(PartyQueryParam query, Integer pageNum, Integer pageSize);

    /**
     * 会员管理分页（客户 → 会员管理子标签）
     */
    IPage<Party> getMemberPage(PartyQueryParam query, Integer pageNum, Integer pageSize);

    /**
     * 全部联系人分页（客户 → 全部联系人子标签，联系人 × 归属客户联表）
     */
    IPage<PartyContactRow> getContactPage(PartyQueryParam query, Integer pageNum, Integer pageSize);

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
