package com.mg.nmlonline.api.dto;

import java.util.List;

public class ResourceBatchSaleResponseDto {

    private String message;
    private double totalValue;
    private List<ResourceSaleResponseDto> sales;

    public ResourceBatchSaleResponseDto() {
    }

    public ResourceBatchSaleResponseDto(String message, double totalValue, List<ResourceSaleResponseDto> sales) {
        this.message = message;
        this.totalValue = totalValue;
        this.sales = sales;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public double getTotalValue() {
        return totalValue;
    }

    public void setTotalValue(double totalValue) {
        this.totalValue = totalValue;
    }

    public List<ResourceSaleResponseDto> getSales() {
        return sales;
    }

    public void setSales(List<ResourceSaleResponseDto> sales) {
        this.sales = sales;
    }
}
