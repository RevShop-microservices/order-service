package com.example.order_service.service;

import com.example.order_service.CustomExceptions.*;
import com.example.order_service.dto.*;
//import com.Revature.Ecommerce.Platform.dto.ProductRequestDTO;
import com.example.order_service.models.Products;
import com.example.order_service.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductService.class);

    @Autowired
    private ProductRepository repository;

    @Autowired
    private MongoOperations mongoTemplate;

    @Autowired
    private EmbeddingService embeddingService;

    public ProductResponseDTO mapToDTO(Products product) {
        return ProductResponseDTO.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .stock(product.getStock())
                .inStock(product.getStock() != null && product.getStock() > 0)
                .city(product.getCity())
                .images(product.getImages())
                .tags(product.getTags())
                .attributes(product.getAttributes())
                .build();
    }

    public Products mapToEntity(ProductRequestDTO dto, Long sellerId) {
        return Products.builder()
                .name(dto.getName())
                .description(dto.getDescription())
                .category(dto.getCategory())
                .brand(dto.getBrand())
                .price(dto.getPrice())
                .stock(dto.getStock())
                .city(dto.getCity())
                .images(dto.getImages())
                .tags(dto.getTags())
                .attributes(dto.getAttributes())
                .sellerId(sellerId)
                .build();
    }

    public Products createProduct(Products product) {
        log.info("Creating product: {}", product.getName());
        String text = product.getName() + " " + product.getDescription();
        product.setEmbedding(embeddingService.getEmbedding(text));
        return repository.save(product);
    }

    public Products getProductById(String id) {
        log.info("Fetching product with ID: {}", id);
        return repository.findById(id).orElseThrow(() -> {
                    log.error("Product not found with ID: {}", id);
                    return new ProductNotFoundException("Product not found");
        });
    }

    public Products updateProduct(String id, ProductRequestDTO dto, Long sellerId) {
        log.info("Updating product ID: {} by seller: {}", id, sellerId);
        Products product=getProductById(id);
        if(!product.getSellerId().equals(sellerId)){
            log.warn("Unauthorized update attempt by seller: {}", sellerId);
            throw new UnauthorizedException("You cannot update this product");
        }
        if (dto.getName() != null) {
            product.setName(dto.getName());
        }

        if (dto.getDescription() != null) {
            product.setDescription(dto.getDescription());
        }

        if (dto.getPrice() != null) {
            product.setPrice(dto.getPrice());
        }

        if (dto.getCategory() != null) {
            product.setCategory(dto.getCategory());
        }

        if (dto.getBrand() != null) {
            product.setBrand(dto.getBrand());
        }

        if (dto.getStock() != null) {
            product.setStock(dto.getStock());
        }

        if (dto.getAttributes() != null) {
            product.setAttributes(dto.getAttributes());
        }

        if (dto.getImages() != null) {
            product.setImages(dto.getImages());
        }

        if (dto.getTags() != null) {
            product.setTags(dto.getTags());
        }

        if (dto.getCity() != null) {
            product.setCity(dto.getCity());
        }

        String text = product.getName() + " " + product.getDescription();
        product.setEmbedding(embeddingService.getEmbedding(text));
        log.info("Product updated successfully: {}", id);
        return repository.save(product);
    }

    public void deleteProduct(String id, Long sellerId){
        log.info("Deleting product ID: {} by seller: {}", id, sellerId);
        Products product=getProductById(id);
        if(!product.getSellerId().equals(sellerId)){
            log.warn("Unauthorized delete attempt by seller: {}", sellerId);
            throw new UnauthorizedException("You cannot delete this product");
        }
        repository.deleteById(id);
        log.info("Product deleted successfully: {}", id);
    }
    //multiple search conditions
    public ProductSearchResponseDTO searchProducts(
            String keyword,
            String category,
            String brand,
            Double minPrice,
            Double maxPrice,
            String tag,
            int page,
            int size,
            String sortBy,
            String sortDir) {

        log.info("Searching products with filters");

        Sort sort = Sort.by(
                "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC,
                sortBy != null ? sortBy : "price"
        );

        Pageable pageable = PageRequest.of(page, size, sort);

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new InvalidFilterException("Invalid price range");
        }

        Query query = new Query();
        List<Criteria> criteriaList = new ArrayList<>();

        if (keyword != null && !keyword.isEmpty()) {
            criteriaList.add(new Criteria().orOperator(
                    Criteria.where("name").regex("^" + keyword, "i"),
                    Criteria.where("description").regex(keyword, "i"),
                    Criteria.where("brand").regex(keyword, "i")
            ));
        }

        if (category != null && !category.isEmpty()) {
            criteriaList.add(Criteria.where("category").is(category));
        }

        if (brand != null && !brand.isEmpty()) {
            criteriaList.add(Criteria.where("brand").is(brand));
        }

        if (minPrice != null && maxPrice != null) {
            criteriaList.add(Criteria.where("price").gte(minPrice).lte(maxPrice));
        }

        if (tag != null && !tag.isEmpty()) {
            criteriaList.add(Criteria.where("tags").in(tag));
        }

        if (!criteriaList.isEmpty()) {
            query.addCriteria(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
        }

        long total = mongoTemplate.count(query, Products.class);

        query.with(pageable);

        List<Products> products = mongoTemplate.find(query, Products.class);

        List<ProductResponseDTO> productDTOs = products.stream()
                .map(this::mapToDTO)
                .toList();

        return new ProductSearchResponseDTO(
                productDTOs,
                page,
                size,
                total
        );
    }
}