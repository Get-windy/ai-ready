package cn.aiedge.erp.party.service;

import cn.aiedge.erp.party.entity.Party;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 双写的**贸易边方向判定**（纯函数，不连库）。
 *
 * <p>为什么单独钉这个：方向错了就是"把客户记成供应商"，而 ⑲ 裁定「账期不可传递、
 * 商务条件按方向各存一份」—— 方向一错，后续账期/额度全会挂到反的那一边上。</p>
 *
 * <p>⚠️ 特别注意"判不出来"这一档：{@code party_type=3}（第三方服务主体，§3.2c）
 * **既不是客户也不是供应商** ⇒ 必须返回空列表（**不猜**），而不是给个默认方向。</p>
 */
class PartyMirrorWriterTest {

    private static Party party(String roles, Integer partyType) {
        Party p = new Party();
        p.setRoles(roles);
        p.setPartyType(partyType);
        return p;
    }

    @Test
    @DisplayName("roles 含 CUSTOMER ⇒ 只建 SALE 边")
    void customerOnly() {
        assertEquals(List.of(PartyMirrorWriter.DIRECTION_SALE),
                PartyMirrorWriter.directionsOf(party("CUSTOMER", 1)));
    }

    @Test
    @DisplayName("roles 含 SUPPLIER ⇒ 只建 PURCHASE 边")
    void supplierOnly() {
        assertEquals(List.of(PartyMirrorWriter.DIRECTION_PURCHASE),
                PartyMirrorWriter.directionsOf(party("SUPPLIER", 2)));
    }

    @Test
    @DisplayName("⑬ 角色可并存：roles 同时含客户与供应商 ⇒ **两条边**")
    void bothRolesMeansTwoEdges() {
        List<String> d = PartyMirrorWriter.directionsOf(party("CUSTOMER,SUPPLIER", 1));
        assertEquals(2, d.size(), "同一主体既是客户又是供应商时要两条边：" + d);
        assertTrue(d.contains(PartyMirrorWriter.DIRECTION_SALE));
        assertTrue(d.contains(PartyMirrorWriter.DIRECTION_PURCHASE));
    }

    @Test
    @DisplayName("roles 为空但 party_type=2 ⇒ 按历史口径当作供应商")
    void nullRolesFallsBackToPartyType() {
        assertEquals(List.of(PartyMirrorWriter.DIRECTION_PURCHASE),
                PartyMirrorWriter.directionsOf(party(null, 2)));
    }

    @Test
    @DisplayName("⚠️ 第三方服务主体（party_type=3）⇒ **一条边都不建**（方向未定，不猜）")
    void thirdPartyServiceHasNoTradeEdge() {
        assertTrue(PartyMirrorWriter.directionsOf(party(null, 3)).isEmpty(),
                "承运商这类第三方服务主体不是客户也不是供应商 ⇒ 不许编一个默认方向");
        assertTrue(PartyMirrorWriter.directionsOf(party(null, null)).isEmpty());
    }

    @Test
    @DisplayName("null 实体 ⇒ 空列表（不抛错）")
    void nullEntity() {
        assertTrue(PartyMirrorWriter.directionsOf(null).isEmpty());
    }
}
