package cn.aiedge.erp.supply.dashboard.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Comment;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 仪表板配置实体
 * 管理仪表板布局和组件配置
 * 
 * @author AI-Edge
 * @since 2026-05-04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "supply_dashboard_config", indexes = {
    @Index(name = "idx_dashboard_code", columnList = "dashboard_code", unique = true),
    @Index(name = "idx_org_user", columnList = "org_id, user_id"),
    @Index(name = "idx_role_status", columnList = "role_code, status")
})
@Comment("供应链仪表板配置表")
public class DashboardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    @Comment("主键ID")
    private Long id;

    @Column(name = "dashboard_code", nullable = false, length = 50)
    @Comment("仪表板编码")
    private String dashboardCode;

    @Column(name = "dashboard_name", nullable = false, length = 100)
    @Comment("仪表板名称")
    private String dashboardName;

    @Column(name = "dashboard_description", length = 500)
    @Comment("仪表板描述")
    private String dashboardDescription;

    @Column(name = "dashboard_type", length = 30)
    @Comment("仪表板类型：OVERVIEW-概览，INVENTORY-库存，PURCHASE-采购，LOGISTICS-物流，SUPPLIER-供应商，CUSTOM-自定义")
    private String dashboardType;

    @Column(name = "role_code", length = 50)
    @Comment("角色编码（为空表示公共仪表板）")
    private String roleCode;

    @Column(name = "user_id")
    @Comment("用户ID（为空表示角色通用仪表板）")
    private Long userId;

    @Column(name = "org_id")
    @Comment("组织ID")
    private Long orgId;

    @Column(name = "department_id")
    @Comment("部门ID")
    private Long departmentId;

    @Column(name = "layout_type", length = 30)
    @Comment("布局类型：GRID-网格，FLEX-弹性，CUSTOM-自定义")
    private String layoutType;

    @Column(name = "theme", length = 30)
    @Comment("主题：LIGHT-浅色，DARK-深色，BLUE-蓝色，GREEN-绿色")
    private String theme;

    @Column(name = "refresh_interval")
    @Comment("自动刷新间隔（秒），0表示不自动刷新")
    private Integer refreshInterval;

    @Column(name = "default_time_range", length = 30)
    @Comment("默认时间范围：TODAY-今天，THIS_WEEK-本周，THIS_MONTH-本月，LAST_7_DAYS-最近7天，LAST_30_DAYS-最近30天")
    private String defaultTimeRange;

    @Column(name = "widget_config", columnDefinition = "TEXT")
    @Comment("组件配置（JSON格式）")
    private String widgetConfig;

    @Column(name = "data_filter_config", columnDefinition = "TEXT")
    @Comment("数据筛选配置（JSON格式）")
    private String dataFilterConfig;

    @Column(name = "alert_config", columnDefinition = "TEXT")
    @Comment("告警配置（JSON格式）")
    private String alertConfig;

    @Column(name = "export_config", columnDefinition = "TEXT")
    @Comment("导出配置（JSON格式）")
    private String exportConfig;

    @Column(name = "is_default")
    @Comment("是否为默认仪表板")
    private Boolean defaultDashboard = false;

    @Column(name = "is_public")
    @Comment("是否公开")
    private Boolean publicDashboard = false;

    @Column(name = "is_editable")
    @Comment("是否可编辑")
    private Boolean editable = true;

    @Column(name = "version")
    @Comment("版本号")
    private Integer version = 1;

    @Column(name = "status", length = 20)
    @Comment("状态：ACTIVE-激活，INACTIVE-未激活，ARCHIVED-已归档")
    private String status = "ACTIVE";

    @Column(name = "sort_order")
    @Comment("排序顺序")
    private Integer sortOrder = 0;

    @Column(name = "created_by", length = 50)
    @Comment("创建人")
    private String createdBy;

    @CreationTimestamp
    @Column(name = "created_time")
    @Comment("创建时间")
    private LocalDateTime createdTime;

    @Column(name = "updated_by", length = 50)
    @Comment("更新人")
    private String updatedBy;

    @UpdateTimestamp
    @Column(name = "updated_time")
    @Comment("更新时间")
    private LocalDateTime updatedTime;

    @Column(name = "last_accessed_time")
    @Comment("最后访问时间")
    private LocalDateTime lastAccessedTime;

    @Column(name = "access_count")
    @Comment("访问次数")
    private Long accessCount = 0L;

    @Column(name = "remark", length = 500)
    @Comment("备注")
    private String remark;

    /**
     * 仪表板类型枚举
     */
    public enum DashboardType {
        OVERVIEW("概览仪表板"),
        INVENTORY("库存监控仪表板"),
        PURCHASE("采购监控仪表板"),
        LOGISTICS("物流监控仪表板"),
        SUPPLIER("供应商监控仪表板"),
        FINANCE("财务监控仪表板"),
        CUSTOM("自定义仪表板");

        private final String description;

        DashboardType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 布局类型枚举
     */
    public enum LayoutType {
        GRID("网格布局"),
        FLEX("弹性布局"),
        CUSTOM("自定义布局");

        private final String description;

        LayoutType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 主题枚举
     */
    public enum Theme {
        LIGHT("浅色主题"),
        DARK("深色主题"),
        BLUE("蓝色主题"),
        GREEN("绿色主题"),
        PURPLE("紫色主题");

        private final String description;

        Theme(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 默认时间范围枚举
     */
    public enum DefaultTimeRange {
        TODAY("今天"),
        YESTERDAY("昨天"),
        THIS_WEEK("本周"),
        LAST_WEEK("上周"),
        THIS_MONTH("本月"),
        LAST_MONTH("上月"),
        LAST_7_DAYS("最近7天"),
        LAST_30_DAYS("最近30天"),
        LAST_90_DAYS("最近90天");

        private final String description;

        DefaultTimeRange(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 状态枚举
     */
    public enum Status {
        ACTIVE("激活"),
        INACTIVE("未激活"),
        ARCHIVED("已归档");

        private final String description;

        Status(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 组件配置类
     */
    @Data
    public static class WidgetConfig {
        private List<Widget> widgets;
        private Layout layout;
        private Style style;

        @Data
        public static class Widget {
            private String widgetId;
            private String widgetType; // KPI, CHART, TABLE, MAP, etc.
            private String metricCode;
            private String title;
            private Position position;
            private Size size;
            private Config config;
            private Filter filter;
        }

        @Data
        public static class Layout {
            private String type;
            private Integer columns;
            private Integer rowHeight;
            private Boolean compact;
            private Boolean preventCollision;
            private Boolean isDraggable;
            private Boolean isResizable;
        }

        @Data
        public static class Style {
            private String backgroundColor;
            private String borderColor;
            private String borderRadius;
            private String padding;
            private String margin;
            private String shadow;
        }

        @Data
        public static class Position {
            private Integer x;
            private Integer y;
            private Integer w;
            private Integer h;
        }

        @Data
        public static class Size {
            private Integer width;
            private Integer height;
            private String minWidth;
            private String minHeight;
            private String maxWidth;
            private String maxHeight;
        }

        @Data
        public static class Config {
            private String chartType;
            private String colorScheme;
            private Boolean showLegend;
            private Boolean showTooltip;
            private Boolean showAxis;
            private Boolean showGrid;
            private Boolean animate;
        }

        @Data
        public static class Filter {
            private TimeFilter time;
            private DataFilter data;
            private String customFilter;
        }

        @Data
        public static class TimeFilter {
            private String type;
            private String start;
            private String end;
            private String comparePeriod;
        }

        @Data
        public static class DataFilter {
            private List<String> orgIds;
            private List<String> deptIds;
            private List<String> productCategories;
            private List<String> suppliers;
            private List<String> warehouses;
        }
    }
}