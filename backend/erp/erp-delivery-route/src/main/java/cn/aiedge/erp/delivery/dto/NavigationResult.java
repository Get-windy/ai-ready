package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class NavigationResult {

    private String navigationUrl;

    private Double distance;

    private Integer duration;

    private String polyline;

    private List<NavigationStep> steps;
}