package cn.aiedge.erp.party.controller;

import cn.aiedge.base.vo.Result;
import cn.aiedge.erp.party.dto.PartyContactDTO;
import cn.aiedge.erp.party.entity.Contact;
import cn.aiedge.erp.party.entity.PartyContact;
import cn.aiedge.erp.party.service.IContactService;
import cn.aiedge.erp.party.service.IPartyContactService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import cn.dev33.satoken.annotation.SaCheckPermission;

@Slf4j
@Tag(name = "往来单位联系人管理")
@RestController
@RequestMapping("/api/erp/partner/contacts")
@RequiredArgsConstructor
public class PartyContactController {

    private final IPartyContactService partyContactService;
    private final IContactService contactService;

    @Operation(summary = "分页查询联系人")
    @SaCheckPermission("party:contacts:list")
    @GetMapping("/page")
    public Result<IPage<PartyContactDTO>> pageContacts(
            @RequestParam(required = false) Long partyId,
            @RequestParam(required = false) String contactName,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize) {

        Page<PartyContact> page = new Page<>(pageNum, pageSize);
        QueryWrapper<PartyContact> wrapper = new QueryWrapper<>();
        wrapper.eq(partyId != null, "party_id", partyId)
               .like(contactName != null && !contactName.isEmpty(), "contact_name", contactName)
               .eq("deleted", 0)
               .orderByDesc("create_time");

        IPage<PartyContact> result = partyContactService.page(page, wrapper);

        IPage<PartyContactDTO> dtoPage = new Page<>();
        dtoPage.setRecords(result.getRecords().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList()));
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());

        return Result.ok(dtoPage);
    }

    @Operation(summary = "根据往来单位ID查询联系人列表")
    @SaCheckPermission("party:contacts:detail")
    @GetMapping("/by-party/{partyId}")
    public Result<List<PartyContactDTO>> getContactsByParty(@PathVariable Long partyId) {
        List<PartyContact> contacts = partyContactService.list(
                new QueryWrapper<PartyContact>()
                        .eq("party_id", partyId)
                        .eq("deleted", 0)
                        .orderByAsc("is_primary").orderByDesc("create_time"));
        return Result.ok(convertWithPerson(contacts));
    }

    @Operation(summary = "获取联系人详情")
    @SaCheckPermission("party:contacts:detail")
    @GetMapping("/{id}")
    public Result<PartyContactDTO> getContact(@PathVariable Long id) {
        PartyContact contact = partyContactService.getById(id);
        if (contact == null || contact.getDeleted() != 0) {
            return Result.fail("联系人不存在");
        }
        return Result.ok(convertToDTO(contact));
    }

    @Operation(summary = "新增联系人")
    @SaCheckPermission("party:contacts:create")
    @PostMapping
    public Result<Boolean> addContact(@RequestBody PartyContactDTO dto) {
        PartyContact contact = new PartyContact();
        BeanUtils.copyProperties(dto, contact);
        boolean result = partyContactService.save(contact);
        return Result.ok(result);
    }

    @Operation(summary = "更新联系人")
    @SaCheckPermission("party:contacts:update")
    @PutMapping("/{id}")
    public Result<Boolean> updateContact(@PathVariable Long id, @RequestBody PartyContactDTO dto) {
        PartyContact contact = partyContactService.getById(id);
        if (contact == null || contact.getDeleted() != 0) {
            return Result.fail("联系人不存在");
        }
        BeanUtils.copyProperties(dto, contact);
        contact.setId(id);
        boolean result = partyContactService.updateById(contact);
        return Result.ok(result);
    }

    @Operation(summary = "删除联系人")
    @SaCheckPermission("party:contacts:delete")
    @DeleteMapping("/{id}")
    public Result<Boolean> deleteContact(@PathVariable Long id) {
        PartyContact contact = partyContactService.getById(id);
        if (contact == null || contact.getDeleted() != 0) {
            return Result.fail("联系人不存在");
        }
        // 逻辑删除（deleted 为 @TableLogic 字段，须走 removeById 才会落库）
        boolean result = partyContactService.removeById(id);
        return Result.ok(result);
    }

    @Operation(summary = "设置主要联系人")
    @SaCheckPermission("party:contacts:update")
    @PutMapping("/{id}/primary")
    public Result<Boolean> setPrimaryContact(@PathVariable Long id, @RequestParam Long partyId) {
        // 先将该往来单位的所有联系人设为非主要
        PartyContact updateWrapper = new PartyContact();
        updateWrapper.setIsPrimary(0);
        partyContactService.update(updateWrapper,
                new QueryWrapper<PartyContact>().eq("party_id", partyId));

        // 将指定联系人设为主要联系人
        PartyContact contact = partyContactService.getById(id);
        if (contact == null || contact.getDeleted() != 0) {
            return Result.fail("联系人不存在");
        }
        contact.setIsPrimary(1);
        boolean result = partyContactService.updateById(contact);
        return Result.ok(result);
    }

    /**
     * 关联记录 + 独立联系人（biz_contact）合并为 DTO：
     * 「人」的属性优先取 biz_contact，回退关联表上的历史冗余列（兼容迁移前数据）。
     */
    private List<PartyContactDTO> convertWithPerson(List<PartyContact> rels) {
        Set<Long> ids = rels.stream().map(PartyContact::getContactId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Contact> personMap = ids.isEmpty() ? Collections.emptyMap()
                : contactService.listByIds(ids).stream()
                    .collect(Collectors.toMap(Contact::getId, c -> c, (a, b) -> a));
        return rels.stream().map(r -> {
            PartyContactDTO dto = convertToDTO(r);
            Contact c = r.getContactId() == null ? null : personMap.get(r.getContactId());
            if (c != null) {
                dto.setContactId(c.getId());
                dto.setContactName(c.getContactName());
                dto.setGender(c.getGender());
                dto.setMobile(c.getMobile());
                dto.setPhone(c.getPhone());
                dto.setEmail(c.getEmail());
                dto.setWechat(c.getWechat());
                dto.setQq(c.getQq());
                dto.setBirthday(c.getBirthday());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private PartyContactDTO convertToDTO(PartyContact contact) {
        PartyContactDTO dto = new PartyContactDTO();
        BeanUtils.copyProperties(contact, dto);
        return dto;
    }
}
