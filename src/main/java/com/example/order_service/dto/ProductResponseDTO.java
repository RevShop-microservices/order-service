package com.example.order_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProductResponseDTO {

    private String id;
    private String name;
    private String description;
    private String category;
    private String brand;
    private Double price;

    private boolean inStock;
    private Integer stock;

    private String city;
    private List<String> images;
    private List<String> tags;

    private Map<String, Object> attributes;
}