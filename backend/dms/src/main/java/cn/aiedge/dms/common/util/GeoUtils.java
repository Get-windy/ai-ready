package cn.aiedge.dms.common.util;

import java.util.ArrayList;
import java.util.List;

/**
 * 地理计算工具（纯静态、无外部依赖）
 *
 * 覆盖配送场景的四类基础能力：
 * 1. 球面距离（Haversine，米）—— 替代各处的「经纬度等权欧氏距离」直角近似；
 * 2. 电子围栏判定 —— 圆形（距离阈值）与多边形（射线法）；
 * 3. 坐标体系转换 —— WGS-84（GPS 原始）/ GCJ-02（高德、腾讯）/ BD-09（百度）；
 * 4. 多边形文本解析（"lng,lat;lng,lat;…"，与地图厂商 polyline 同构）。
 *
 * 坐标体系约定：本系统对外统一 GCJ-02，入库前转换（见《路线规划开发文档》§3.5）。
 *
 * @author AI-Ready Team
 */
public final class GeoUtils {

    private GeoUtils() {
    }

    /** 地球平均半径（米） */
    private static final double EARTH_RADIUS_METERS = 6371000d;

    /** GCJ-02 椭球参数（克拉索夫斯基） */
    private static final double GCJ_A = 6378245.0d;
    private static final double GCJ_EE = 0.00669342162296594323d;

    /** 坐标体系标识 */
    public static final String COORD_WGS84 = "WGS84";
    public static final String COORD_GCJ02 = "GCJ02";
    public static final String COORD_BD09 = "BD09";

    // ==================== 距离 ====================

    /**
     * Haversine 球面距离（米）
     */
    public static double distanceMeters(double lat1, double lng1, double lat2, double lng2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_METERS * c;
    }

    // ==================== 围栏判定 ====================

    /**
     * 圆形围栏判定：点是否落在「中心点 + 半径」的圆内
     */
    public static boolean inCircle(double lat, double lng, double centerLat, double centerLng, double radiusMeters) {
        return distanceMeters(lat, lng, centerLat, centerLng) <= radiusMeters;
    }

    /**
     * 多边形围栏判定（射线法，坐标按 lng=x / lat=y）
     *
     * @param polygon 顶点列表，每个元素为 [lng, lat]；要求首尾不重复、至少 3 个顶点
     */
    public static boolean inPolygon(double lat, double lng, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 3) {
            return false;
        }
        boolean inside = false;
        int size = polygon.size();
        for (int i = 0, j = size - 1; i < size; j = i++) {
            double yi = polygon.get(i)[1];
            double yj = polygon.get(j)[1];
            double xi = polygon.get(i)[0];
            double xj = polygon.get(j)[0];
            boolean intersect = ((yi > lat) != (yj > lat))
                    && (lng < (xj - xi) * (lat - yi) / (yj - yi) + xi);
            if (intersect) {
                inside = !inside;
            }
        }
        return inside;
    }

    /**
     * 点到多边形边界的最短距离（米）—— 用于「围栏外多远」提示
     */
    public static double distanceToPolygonMeters(double lat, double lng, List<double[]> polygon) {
        if (polygon == null || polygon.size() < 2) {
            return 0d;
        }
        double min = Double.MAX_VALUE;
        int size = polygon.size();
        for (int i = 0; i < size; i++) {
            double[] a = polygon.get(i);
            double[] b = polygon.get((i + 1) % size);
            min = Math.min(min, distanceToSegmentMeters(lat, lng, a[1], a[0], b[1], b[0]));
        }
        return min == Double.MAX_VALUE ? 0d : min;
    }

    /** 点到线段（经纬度）的近似最短距离（米）：小范围内按等距圆柱投影折算 */
    private static double distanceToSegmentMeters(double lat, double lng,
                                                  double lat1, double lng1,
                                                  double lat2, double lng2) {
        double metersPerDegLat = 111320d;
        double metersPerDegLng = 111320d * Math.cos(Math.toRadians(lat));

        double px = (lng - lng1) * metersPerDegLng;
        double py = (lat - lat1) * metersPerDegLat;
        double dx = (lng2 - lng1) * metersPerDegLng;
        double dy = (lat2 - lat1) * metersPerDegLat;
        double lenSq = dx * dx + dy * dy;

        double t = lenSq == 0 ? 0 : Math.max(0, Math.min(1, (px * dx + py * dy) / lenSq));
        double projX = dx * t;
        double projY = dy * t;
        return Math.sqrt(Math.pow(px - projX, 2) + Math.pow(py - projY, 2));
    }

    /**
     * 解析多边形文本 "lng,lat;lng,lat;…"
     */
    public static List<double[]> parsePolygon(String text) {
        List<double[]> points = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return points;
        }
        for (String pair : text.split(";")) {
            if (pair.isBlank()) {
                continue;
            }
            String[] xy = pair.split(",");
            if (xy.length != 2) {
                continue;
            }
            try {
                points.add(new double[]{Double.parseDouble(xy[0].trim()), Double.parseDouble(xy[1].trim())});
            } catch (NumberFormatException ignore) {
                // 单点格式非法不影响其余顶点
            }
        }
        return points;
    }

    /**
     * 序列化多边形为 "lng,lat;lng,lat;…"
     */
    public static String formatPolygon(List<double[]> points) {
        if (points == null || points.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (double[] p : points) {
            if (p == null || p.length < 2) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(';');
            }
            sb.append(p[0]).append(',').append(p[1]);
        }
        return sb.toString();
    }

    // ==================== 坐标转换 ====================

    /**
     * 任意体系 → GCJ-02（系统统一口径）
     *
     * @param from WGS84 / GCJ02 / BD09（大小写不敏感，缺省按 GCJ02）
     */
    public static double[] toGcj02(double lat, double lng, String from) {
        String system = from == null ? COORD_GCJ02 : from.trim().toUpperCase();
        return switch (system) {
            case COORD_WGS84 -> wgs84ToGcj02(lat, lng);
            case COORD_BD09 -> bd09ToGcj02(lat, lng);
            default -> new double[]{lat, lng};
        };
    }

    /**
     * WGS-84 → GCJ-02（火星坐标偏移）
     */
    public static double[] wgs84ToGcj02(double lat, double lng) {
        if (outOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double dLat = transformLat(lng - 105.0, lat - 35.0);
        double dLng = transformLng(lng - 105.0, lat - 35.0);
        double radLat = lat / 180.0 * Math.PI;
        double magic = Math.sin(radLat);
        magic = 1 - GCJ_EE * magic * magic;
        double sqrtMagic = Math.sqrt(magic);
        dLat = (dLat * 180.0) / ((GCJ_A * (1 - GCJ_EE)) / (magic * sqrtMagic) * Math.PI);
        dLng = (dLng * 180.0) / (GCJ_A / sqrtMagic * Math.cos(radLat) * Math.PI);
        return new double[]{lat + dLat, lng + dLng};
    }

    /**
     * GCJ-02 → WGS-84（一次反向迭代，米级精度足够配送场景）
     */
    public static double[] gcj02ToWgs84(double lat, double lng) {
        if (outOfChina(lat, lng)) {
            return new double[]{lat, lng};
        }
        double[] gcj = wgs84ToGcj02(lat, lng);
        return new double[]{lat * 2 - gcj[0], lng * 2 - gcj[1]};
    }

    /**
     * GCJ-02 → BD-09
     */
    public static double[] gcj02ToBd09(double lat, double lng) {
        double x = lng;
        double y = lat;
        double z = Math.sqrt(x * x + y * y) + 0.00002 * Math.sin(y * Math.PI * 3000.0 / 180.0);
        double theta = Math.atan2(y, x) + 0.000003 * Math.cos(x * Math.PI * 3000.0 / 180.0);
        return new double[]{z * Math.sin(theta) + 0.006, z * Math.cos(theta) + 0.0065};
    }

    /**
     * BD-09 → GCJ-02
     */
    public static double[] bd09ToGcj02(double lat, double lng) {
        double x = lng - 0.0065;
        double y = lat - 0.006;
        double z = Math.sqrt(x * x + y * y) - 0.00002 * Math.sin(y * Math.PI * 3000.0 / 180.0);
        double theta = Math.atan2(y, x) - 0.000003 * Math.cos(x * Math.PI * 3000.0 / 180.0);
        return new double[]{z * Math.sin(theta), z * Math.cos(theta)};
    }

    /** 中国境外不做偏移 */
    private static boolean outOfChina(double lat, double lng) {
        return lng < 72.004 || lng > 137.8347 || lat < 0.8293 || lat > 55.8271;
    }

    private static double transformLat(double x, double y) {
        double ret = -100.0 + 2.0 * x + 3.0 * y + 0.2 * y * y + 0.1 * x * y + 0.2 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(y * Math.PI) + 40.0 * Math.sin(y / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (160.0 * Math.sin(y / 12.0 * Math.PI) + 320 * Math.sin(y * Math.PI / 30.0)) * 2.0 / 3.0;
        return ret;
    }

    private static double transformLng(double x, double y) {
        double ret = 300.0 + x + 2.0 * y + 0.1 * x * x + 0.1 * x * y + 0.1 * Math.sqrt(Math.abs(x));
        ret += (20.0 * Math.sin(6.0 * x * Math.PI) + 20.0 * Math.sin(2.0 * x * Math.PI)) * 2.0 / 3.0;
        ret += (20.0 * Math.sin(x * Math.PI) + 40.0 * Math.sin(x / 3.0 * Math.PI)) * 2.0 / 3.0;
        ret += (150.0 * Math.sin(x / 12.0 * Math.PI) + 300.0 * Math.sin(x / 30.0 * Math.PI)) * 2.0 / 3.0;
        return ret;
    }

    /**
     * 坐标是否有效（经纬度范围 + 非空）
     */
    public static boolean validCoord(Double lat, Double lng) {
        return lat != null && lng != null
                && lat >= -90 && lat <= 90
                && lng >= -180 && lng <= 180
                && !(lat == 0d && lng == 0d);
    }
}
