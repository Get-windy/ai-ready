package cn.aiedge.erp.party.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.party.entity.Contact;
import cn.aiedge.erp.party.entity.Party;
import cn.aiedge.erp.party.entity.PartyContact;
import cn.aiedge.erp.party.service.IContactService;
import cn.aiedge.erp.party.service.IPartyContactService;
import cn.aiedge.erp.party.service.PartyService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import cn.dev33.satoken.annotation.SaCheckPermission;

/**
 * 联系人（独立主数据）
 * <p>
 * 联系人与「往来单位」是平行且相互关联的两个实体：
 * 一个联系人可服务多个往来单位（{@link #listParties} / {@link #bindParty}），
 * 一个往来单位可有多个联系人（见 {@code /erp/partner/contacts/by-party/{partyId}}）。
 * </p>
 */
@Tag(name = "联系人（独立主数据）")
@RestController
@RequestMapping("/api/erp/contact")
@RequiredArgsConstructor
public class ContactController {

    private final IContactService contactService;
    private final IPartyContactService partyContactService;
    private final PartyService partyService;

    @Operation(summary = "分页查询联系人")
    @SaCheckPermission("party:contact:list")
    @GetMapping("/page")
    public Result<IPage<Contact>> page(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String mobile,
            @RequestParam(required = false) String gender,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {
        LambdaQueryWrapper<Contact> w = new LambdaQueryWrapper<>();
        w.eq(Contact::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Contact::getContactName, keyword)
                    .or().like(Contact::getMobile, keyword)
                    .or().like(Contact::getPhone, keyword));
        }
        if (StringUtils.hasText(mobile)) w.like(Contact::getMobile, mobile);
        if (StringUtils.hasText(gender)) w.eq(Contact::getGender, gender);
        w.orderByDesc(Contact::getId);
        return Result.ok(contactService.page(new Page<>(pageNum, pageSize), w));
    }

    @Operation(summary = "联系人下拉/放大镜选择（轻量）")
    @SaCheckPermission("party:contact:list")
    @GetMapping("/options")
    public Result<List<Contact>> options(@RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "50") Integer limit) {
        LambdaQueryWrapper<Contact> w = new LambdaQueryWrapper<>();
        w.eq(Contact::getDeleted, 0).eq(Contact::getStatus, 1);
        if (StringUtils.hasText(keyword)) {
            w.and(q -> q.like(Contact::getContactName, keyword).or().like(Contact::getMobile, keyword));
        }
        w.orderByDesc(Contact::getId).last("LIMIT " + Math.max(1, Math.min(limit, 200)));
        return Result.ok(contactService.list(w));
    }

    @Operation(summary = "联系人详情")
    @SaCheckPermission("party:contact:detail")
    @GetMapping("/{id}")
    public Result<Contact> detail(@PathVariable Long id) {
        Contact c = contactService.getById(id);
        return c == null ? Result.fail("联系人不存在") : Result.ok(c);
    }

    @Operation(summary = "新增联系人")
    @SaCheckPermission("party:contact:create")
    @PostMapping
    public Result<Contact> create(@RequestBody Contact body) {
        body.setId(null);
        if (!StringUtils.hasText(body.getContactName())) return Result.fail("请输入联系人姓名");
        if (body.getStatus() == null) body.setStatus(1);
        contactService.save(body);
        return Result.ok(body);
    }

    @Operation(summary = "更新联系人")
    @SaCheckPermission("party:contact:update")
    @PutMapping("/{id}")
    public Result<Boolean> update(@PathVariable Long id, @RequestBody Contact body) {
        Contact exist = contactService.getById(id);
        if (exist == null) return Result.fail("联系人不存在");
        body.setId(id);
        return Result.ok(contactService.updateById(body));
    }

    @Operation(summary = "删除联系人（存在关联往来单位时拒绝）")
    @SaCheckPermission("party:contact:delete")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        Long rel = partyContactService.count(new QueryWrapper<PartyContact>()
                .eq("contact_id", id).eq("deleted", 0));
        if (rel != null && rel > 0) {
            return Result.ok("该联系人仍关联 " + rel + " 个往来单位，请先解除关联");
        }
        return Result.ok(contactService.removeById(id) ? null : "删除失败");
    }

    // ─────────── 关联往来单位（多对多） ───────────

    @Operation(summary = "查询该联系人服务的往来单位")
    @SaCheckPermission("party:contact:view")
    @GetMapping("/{id}/parties")
    public Result<List<Map<String, Object>>> listParties(@PathVariable Long id) {
        List<PartyContact> rels = partyContactService.list(new QueryWrapper<PartyContact>()
                .eq("contact_id", id).eq("deleted", 0).orderByDesc("is_primary").orderByAsc("id"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (PartyContact r : rels) {
            Party p = partyService.getById(r.getPartyId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("relId", r.getId());
            m.put("partyId", r.getPartyId());
            m.put("partyName", p != null ? p.getPartyName() : null);
            m.put("partyCode", p != null ? p.getPartyCode() : null);
            m.put("partyType", p != null ? p.getPartyType() : null);
            m.put("isPrimary", r.getIsPrimary());
            m.put("position", r.getPosition());
            m.put("department", r.getDepartment());
            m.put("region", r.getRegion());
            m.put("detailAddress", r.getDetailAddress());
            m.put("deliveryMethod", r.getDeliveryMethod());
            m.put("deliveryRoute", r.getDeliveryRoute());
            m.put("logisticsCompany", r.getLogisticsCompany());
            out.add(m);
        }
        return Result.ok(out);
    }

    @Operation(summary = "按往来单位维护联系人（人 upsert + 关联 upsert，供往来单位表单联系人分区调用）")
    @SaCheckPermission("party:contact:create")
    @PostMapping("/link")
    public Result<Map<String, Object>> link(@RequestBody Map<String, Object> body) {
        Long partyId = toLong(body.get("partyId"));
        if (partyId == null) return Result.fail("缺少往来单位");
        if (partyService.getById(partyId) == null) return Result.fail("往来单位不存在");

        Long contactId = toLong(body.get("contactId"));
        String name = str(body.get("contactName"));
        String mobile = str(body.get("mobile"));
        if (!StringUtils.hasText(mobile)) mobile = str(body.get("phone"));

        Contact person = null;
        if (contactId != null) {
            person = contactService.getById(contactId);
        } else {
            // 同一个人可服务多个单位：优先按手机号归并，其次按姓名
            if (StringUtils.hasText(mobile)) {
                person = contactService.getOne(new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getMobile, mobile).eq(Contact::getDeleted, 0).last("LIMIT 1"), false);
            }
            if (person == null && StringUtils.hasText(name)) {
                person = contactService.getOne(new LambdaQueryWrapper<Contact>()
                        .eq(Contact::getContactName, name).eq(Contact::getDeleted, 0).last("LIMIT 1"), false);
            }
        }
        if (person == null) {
            person = new Contact();
            person.setStatus(1);
        }
        if (StringUtils.hasText(name)) person.setContactName(name);
        if (StringUtils.hasText(mobile)) person.setMobile(mobile);
        applyStr(body, "phone", person::setPhone);
        applyStr(body, "gender", person::setGender);
        applyStr(body, "email", person::setEmail);
        applyStr(body, "wechat", person::setWechat);
        applyStr(body, "qq", person::setQq);
        applyStr(body, "remark", person::setRemark);
        if (body.get("birthday") != null && StringUtils.hasText(String.valueOf(body.get("birthday")))) {
            try { person.setBirthday(java.time.LocalDate.parse(String.valueOf(body.get("birthday")).substring(0, 10))); }
            catch (Exception ignored) { }
        }
        if (person.getId() == null) contactService.save(person); else contactService.updateById(person);

        // 关联（人 ↔ 单位）+ 关系上下文
        PartyContact rel = partyContactService.getOne(new QueryWrapper<PartyContact>()
                .eq("party_id", partyId).eq("contact_id", person.getId()).eq("deleted", 0).last("LIMIT 1"), false);
        boolean isNew = rel == null;
        if (isNew) {
            rel = new PartyContact();
            rel.setPartyId(partyId);
            rel.setContactId(person.getId());
            rel.setStatus(1);
        }
        if (body.get("isPrimary") != null) rel.setIsPrimary(toInt(body.get("isPrimary"), 0));
        applyStr(body, "position", rel::setPosition);
        applyStr(body, "department", rel::setDepartment);
        applyStr(body, "region", rel::setRegion);
        applyStr(body, "detailAddress", rel::setDetailAddress);
        applyStr(body, "deliveryMethod", rel::setDeliveryMethod);
        applyStr(body, "deliveryRoute", rel::setDeliveryRoute);
        applyStr(body, "logisticsCompany", rel::setLogisticsCompany);

        if (Integer.valueOf(1).equals(rel.getIsPrimary())) {
            PartyContact clear = new PartyContact();
            clear.setIsPrimary(0);
            partyContactService.update(clear, new QueryWrapper<PartyContact>().eq("party_id", partyId).eq("deleted", 0));
        }
        if (isNew) partyContactService.save(rel); else partyContactService.updateById(rel);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("contactId", person.getId());
        out.put("relId", rel.getId());
        return Result.ok(out);
    }

    @Operation(summary = "绑定往来单位（已绑定则更新关系上下文）")
    @SaCheckPermission("party:contact:create")
    @PostMapping("/{id}/parties")
    public Result<Long> bindParty(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long partyId = toLong(body.get("partyId"));
        if (partyId == null) return Result.fail("请选择往来单位");
        if (contactService.getById(id) == null) return Result.fail("联系人不存在");
        if (partyService.getById(partyId) == null) return Result.fail("往来单位不存在");

        PartyContact rel = partyContactService.getOne(new QueryWrapper<PartyContact>()
                .eq("contact_id", id).eq("party_id", partyId).eq("deleted", 0).last("LIMIT 1"), false);
        boolean isNew = rel == null;
        if (isNew) {
            rel = new PartyContact();
            rel.setContactId(id);
            rel.setPartyId(partyId);
            rel.setStatus(1);
        }
        if (body.get("isPrimary") != null) rel.setIsPrimary(toInt(body.get("isPrimary"), 0));
        if (body.containsKey("position")) rel.setPosition((String) body.get("position"));
        if (body.containsKey("department")) rel.setDepartment((String) body.get("department"));
        if (body.containsKey("region")) rel.setRegion((String) body.get("region"));
        if (body.containsKey("detailAddress")) rel.setDetailAddress((String) body.get("detailAddress"));
        if (body.containsKey("deliveryMethod")) rel.setDeliveryMethod((String) body.get("deliveryMethod"));
        if (body.containsKey("deliveryRoute")) rel.setDeliveryRoute((String) body.get("deliveryRoute"));
        if (body.containsKey("logisticsCompany")) rel.setLogisticsCompany((String) body.get("logisticsCompany"));

        // 主联系人唯一性：同单位内互斥
        if (Integer.valueOf(1).equals(rel.getIsPrimary())) {
            PartyContact clear = new PartyContact();
            clear.setIsPrimary(0);
            partyContactService.update(clear, new QueryWrapper<PartyContact>()
                    .eq("party_id", partyId).eq("deleted", 0));
        }
        if (isNew) partyContactService.save(rel); else partyContactService.updateById(rel);
        return Result.ok(rel.getId());
    }

    @Operation(summary = "解除与往来单位的关联")
    @SaCheckPermission("party:contact:delete")
    @DeleteMapping("/{id}/parties/{relId}")
    public Result<Boolean> unbindParty(@PathVariable Long id, @PathVariable Long relId) {
        PartyContact rel = partyContactService.getById(relId);
        if (rel == null || !Objects.equals(rel.getContactId(), id)) return Result.fail("关联不存在");
        // 注意：deleted 是 @TableLogic 字段，用 updateById 不会落库，必须走 removeById
        return Result.ok(partyContactService.removeById(relId));
    }

    private String str(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    /** 请求体带该键且非空才写入（空串视为不修改，避免误清空） */
    private void applyStr(Map<String, Object> body, String key, java.util.function.Consumer<String> setter) {
        if (!body.containsKey(key)) return;
        Object v = body.get(key);
        if (v == null) { setter.accept(null); return; }
        String s = String.valueOf(v);
        setter.accept(s.isEmpty() ? null : s);
    }

    private Long toLong(Object v) {
        if (v instanceof Number n) return n.longValue();
        if (v instanceof String s && StringUtils.hasText(s)) {
            try { return Long.valueOf(s); } catch (NumberFormatException ignored) { }
        }
        return null;
    }

    private Integer toInt(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && StringUtils.hasText(s)) {
            try { return Integer.parseInt(s); } catch (NumberFormatException ignored) { }
        }
        return fallback;
    }
}
