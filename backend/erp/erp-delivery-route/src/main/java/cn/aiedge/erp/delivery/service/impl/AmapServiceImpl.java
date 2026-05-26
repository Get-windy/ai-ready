package cn.aiedge.erp.delivery.service.impl;

import cn.aiedge.erp.delivery.dto.*;
import cn.aiedge.erp.delivery.service.AmapService;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AmapServiceImpl implements AmapService {

    @Value("${amap.key:}")
    private String amapKey;

    @Value("${amap.base-url:https://restapi.amap.com/v3}")
    private String baseUrl;

    private static final String DRIVING_API = "/direction/driving";
    private static final String GEOCODE_API = "/geocode/geo";
    private static final String REGEOCODE_API = "/geocode/regeo";
    private static final String DISTANCE_API = "/distance";

    @Override
    @Cacheable(value = "amapRoute", key = "#origin + '-' + #destination + '-' + #strategy")
    public AmapRouteResult getDrivingRoute(String origin, String destination, String strategy) {
        String url = baseUrl + DRIVING_API;
        JSONObject params = new JSONObject();
        params.set("key", amapKey);
        params.set("origin", origin);
        params.set("destination", destination);
        params.set("strategy", strategy != null ? strategy : "0");
        params.set("extensions", "all");

        String response = HttpUtil.get(url, params);
        log.debug("高德路线规划响应: {}", response);

        JSONObject json = JSONUtil.parseObj(response);
        if (!"1".equals(json.getStr("status"))) {
            throw new RuntimeException("高德API调用失败: " + json.getStr("info"));
        }

        return parseRouteResult(json);
    }

    @Override
    public AmapRouteResult getMultiPointRoute(String origin, String destination, List<String> waypoints, String strategy) {
        String url = baseUrl + DRIVING_API;
        JSONObject params = new JSONObject();
        params.set("key", amapKey);
        params.set("origin", origin);
        params.set("destination", destination);
        params.set("waypoints", StrUtil.join(";", waypoints));
        params.set("strategy", strategy != null ? strategy : "0");
        params.set("extensions", "all");

        String response = HttpUtil.get(url, params);
        log.debug("高德多点路线规划响应: {}", response);

        JSONObject json = JSONUtil.parseObj(response);
        if (!"1".equals(json.getStr("status"))) {
            throw new RuntimeException("高德API调用失败: " + json.getStr("info"));
        }

        return parseRouteResult(json);
    }

    @Override
    public String getNavigationUrl(String origin, String destination) {
        return String.format("https://uri.amap.com/navigation?from=%s&to=%s&mode=car&policy=1&coordinate=gaode", 
                origin, destination);
    }

    @Override
    @Cacheable(value = "amapGeocode", key = "#address")
    public String geocodeAddress(String address) {
        String url = baseUrl + GEOCODE_API;
        JSONObject params = new JSONObject();
        params.set("key", amapKey);
        params.set("address", address);
        params.set("city", "全国");

        String response = HttpUtil.get(url, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (!"1".equals(json.getStr("status"))) {
            throw new RuntimeException("地址解析失败: " + address);
        }

        JSONArray geocodes = json.getJSONArray("geocodes");
        if (geocodes.isEmpty()) {
            throw new RuntimeException("未找到地址坐标: " + address);
        }

        return geocodes.getJSONObject(0).getStr("location");
    }

    @Override
    @Cacheable(value = "amapRegeocode", key = "#location")
    public String regeocodeLocation(String location) {
        String url = baseUrl + REGEOCODE_API;
        JSONObject params = new JSONObject();
        params.set("key", amapKey);
        params.set("location", location);
        params.set("extensions", "base");

        String response = HttpUtil.get(url, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (!"1".equals(json.getStr("status"))) {
            throw new RuntimeException("逆地址解析失败: " + location);
        }

        JSONObject regeocode = json.getJSONObject("regeocode");
        return regeocode.getStr("formatted_address");
    }

    @Override
    public Double calculateDistance(String origin, String destination) {
        String url = baseUrl + DISTANCE_API;
        JSONObject params = new JSONObject();
        params.set("key", amapKey);
        params.set("origins", origin);
        params.set("destination", destination);
        params.set("type", "1");

        String response = HttpUtil.get(url, params);
        JSONObject json = JSONUtil.parseObj(response);

        if (!"1".equals(json.getStr("status"))) {
            throw new RuntimeException("距离计算失败");
        }

        JSONArray results = json.getJSONArray("results");
        if (!results.isEmpty()) {
            return results.getJSONObject(0).getDouble("distance");
        }
        return 0.0;
    }

    @Override
    public List<String> optimizeRouteOrder(String origin, List<String> waypoints, String destination) {
        List<String> optimizedOrder = new ArrayList<>();
        optimizedOrder.add(origin);

        List<String> remaining = new ArrayList<>(waypoints);
        String current = origin;

        while (!remaining.isEmpty()) {
            String nearest = findNearestPoint(current, remaining);
            optimizedOrder.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        optimizedOrder.add(destination);
        return optimizedOrder;
    }

    private String findNearestPoint(String current, List<String> points) {
        String nearest = null;
        double minDistance = Double.MAX_VALUE;

        for (String point : points) {
            try {
                Double distance = calculateDistance(current, point);
                if (distance < minDistance) {
                    minDistance = distance;
                    nearest = point;
                }
            } catch (Exception e) {
                log.warn("距离计算失败: {} -> {}", current, point);
            }
        }

        return nearest != null ? nearest : points.get(0);
    }

    private AmapRouteResult parseRouteResult(JSONObject json) {
        AmapRouteResult result = new AmapRouteResult();
        result.setStatus(json.getStr("status"));
        result.setInfo(json.getStr("info"));
        result.setInfocode(json.getStr("infocode"));

        JSONObject route = json.getJSONObject("route");
        if (route != null) {
            result.setCount(route.getInt("count", 0));

            JSONArray paths = route.getJSONArray("paths");
            if (paths != null && !paths.isEmpty()) {
                List<AmapRoutePath> pathList = new ArrayList<>();
                for (int i = 0; i < paths.size(); i++) {
                    JSONObject pathJson = paths.getJSONObject(i);
                    AmapRoutePath path = new AmapRoutePath();
                    path.setDistance(pathJson.getDouble("distance"));
                    path.setDuration(pathJson.getInt("duration"));

                    JSONArray steps = pathJson.getJSONArray("steps");
                    if (steps != null) {
                        List<AmapRouteStep> stepList = new ArrayList<>();
                        for (int j = 0; j < steps.size(); j++) {
                            JSONObject stepJson = steps.getJSONObject(j);
                            AmapRouteStep step = new AmapRouteStep();
                            step.setDistance(stepJson.getDouble("distance"));
                            step.setDuration(stepJson.getInt("duration"));
                            step.setPolyline(stepJson.getStr("polyline"));
                            step.setRoad(stepJson.getStr("road"));
                            step.setInstruction(stepJson.getStr("instruction"));
                            stepList.add(step);
                        }
                        path.setSteps(stepList);
                    }
                    pathList.add(path);
                }
                result.setRoute(pathList);
            }
        }

        return result;
    }
}