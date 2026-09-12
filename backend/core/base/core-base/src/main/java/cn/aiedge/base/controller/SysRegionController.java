package cn.aiedge.base.controller;

import cn.aiedge.base.entity.SysRegion;
import cn.aiedge.base.mapper.SysRegionMapper;
import cn.aiedge.common.result.ApiResponse;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 行政区划（省 / 市 / 区县）
 * <p>
 * 全系统公共能力：往来单位（客户 / 供应商 / 物流公司 / 其他往来单位）表单的
 * 「所在地区」三级联动统一调用本接口，不重复实现。
 * </p>
 */
@Tag(name = "行政区划", description = "省/市/区县三级联动数据")
@RestController
@RequestMapping("/api/sys/region")
@RequiredArgsConstructor
public class SysRegionController {

    private final SysRegionMapper sysRegionMapper;

    @Operation(summary = "按上级代码查询下级行政区划（parentCode 为空返回全部省份）")
    @GetMapping("/children")
    public ResponseEntity<ApiResponse<List<SysRegion>>> children(
            @RequestParam(required = false) String parentCode) {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getStatus, 1);
        if (StringUtils.hasText(parentCode)) {
            wrapper.eq(SysRegion::getParentCode, parentCode);
        } else {
            wrapper.isNull(SysRegion::getParentCode);
        }
        wrapper.orderByAsc(SysRegion::getSortOrder).orderByAsc(SysRegion::getCode);
        return ResponseEntity.ok(ApiResponse.ok(sysRegionMapper.selectList(wrapper)));
    }

    @Operation(summary = "省市区三级树（一次性返回，前端缓存复用）")
    @GetMapping("/tree")
    public ResponseEntity<ApiResponse<List<SysRegion>>> tree() {
        LambdaQueryWrapper<SysRegion> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRegion::getStatus, 1)
                .orderByAsc(SysRegion::getRegionLevel)
                .orderByAsc(SysRegion::getSortOrder)
                .orderByAsc(SysRegion::getCode);
        List<SysRegion> all = sysRegionMapper.selectList(wrapper);

        Map<String, SysRegion> byCode = new LinkedHashMap<>();
        for (SysRegion r : all) {
            r.setChildren(new ArrayList<>());
            byCode.put(r.getCode(), r);
        }
        List<SysRegion> roots = new ArrayList<>();
        for (SysRegion r : all) {
            if (!StringUtils.hasText(r.getParentCode())) {
                roots.add(r);
            } else {
                SysRegion parent = byCode.get(r.getParentCode());
                if (parent != null) {
                    parent.getChildren().add(r);
                }
            }
        }
        return ResponseEntity.ok(ApiResponse.ok(roots));
    }
}
