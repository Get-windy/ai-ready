package cn.aiedge.erp.delivery.dto;

import lombok.Data;

@Data
public class NavigationStep {

    private String instruction;

    private Double distance;

    private Integer duration;

    private String roadName;

    private String action;
}