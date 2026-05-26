package com.example.order_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ProductSearchResponseDTO {
    private List<ProductResponseDTO> products;
    private int page;
    private int size;
    private long totalElements;
}
