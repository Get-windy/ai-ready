package cn.aiedge.erp.delivery.print;

import cn.aiedge.erp.delivery.entity.DeliveryRoute;
import cn.aiedge.erp.delivery.entity.RoutePoint;
import cn.aiedge.erp.delivery.mapper.DeliveryRouteMapper;
import cn.aiedge.erp.delivery.mapper.RoutePointMapper;
import cn.aiedge.erp.printing.spi.PrintDataProvider;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 配送路线（线路列表）打印装配器 —— **手写**，因为它是两段式，通用装配器接不上。
 *
 * <p>{@code erp_delivery_route}（配送路线）与 {@code erp_route_point}（线路点位）**不是父子键**：
 * 点位挂在 {@code erp_route}（线路主档）上，配送路线只是引用了 {@code route_id}。
 * 通用装配器只会拿「单据自己的 id」去比对外键列，这里要先用**路线上的 route_id** 去取点位，
 * 所以手写这一层。</p>
 */
@Component
@RequiredArgsConstructor
public class DeliveryRoutePrintDataProvider implements PrintDataProvider {

    /** 与前端 {@code <PrintDialog page-code="dms-route-list">} 一致；不能含 `/` */
    public static final String PAGE_CODE = "dms-route-list";

    private final DeliveryRouteMapper routeMapper;
    private final RoutePointMapper routePointMapper;
    private final ObjectMapper objectMapper;

    @Override
    public String pageCode() {
        return PAGE_CODE;
    }

    @Override
    public Map<String, Object> load(Long documentId) {
        DeliveryRoute doc = routeMapper.selectById(documentId);
        if (doc == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>(toMap(doc));

        List<Map<String, Object>> rows = new ArrayList<>();
        if (doc.getRouteId() != null) {
            List<RoutePoint> points = routePointMapper.selectList(
                    new LambdaQueryWrapper<RoutePoint>()
                            .eq(RoutePoint::getRouteId, doc.getRouteId())
                            .orderByAsc(RoutePoint::getPointOrder));
            int fallbackLineNo = 1;
            for (RoutePoint point : points) {
                Map<String, Object> row = new LinkedHashMap<>(toMap(point));
                // 行号取 pointOrder（点位顺序），为空按顺序补
                row.put("lineNo", point.getPointOrder() != null ? point.getPointOrder() : fallbackLineNo);
                fallbackLineNo++;
                rows.add(row);
            }
        }
        data.put("items", rows);
        return data;
    }

    @Override
    public String documentNo(Long documentId) {
        DeliveryRoute doc = routeMapper.selectById(documentId);
        return doc != null ? doc.getRouteCode() : null;
    }

    private Map<String, Object> toMap(Object bean) {
        return objectMapper.convertValue(bean, new TypeReference<Map<String, Object>>() {
        });
    }
}
