package com.healthmanagement.controller.shop;

import com.healthmanagement.util.ApiResponse;
import com.healthmanagement.dto.shop.ProductDTO;
import com.healthmanagement.dto.shop.ProductRequest;
import com.healthmanagement.service.shop.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/products")
@Tag(name = "商品管理", description = "商品管理相關API")
public class ProductController {

    @Autowired
    private ProductService productService;

    @GetMapping
    @Operation(
        summary = "獲取所有商品",
        description = "獲取系統中所有商品的列表"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取商品列表")
    })
    public ResponseEntity<?> getAllProducts(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        List<ProductDTO> products = productService.getAllProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/{id}")
    @Operation(
        summary = "獲取商品詳情",
        description = "根據商品ID獲取商品的詳細信息"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取商品信息"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    })
    public ResponseEntity<?> getProductById(@PathVariable Integer id) {
        ProductDTO productDTO = productService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.success(productDTO));
    }

    @PostMapping
    @Operation(
        summary = "創建新商品",
        description = "創建一個新的商品，需提供商品的詳細信息"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品創建成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "服務器內部錯誤")
    })
    public ResponseEntity<?> createProduct(@RequestBody ProductRequest productRequest) {
        ProductDTO createdProduct = productService.createProduct(productRequest);
        return new ResponseEntity<>(ApiResponse.success(createdProduct), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    @Operation(
        summary = "更新商品信息",
        description = "根據商品ID更新現有商品的信息"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品更新成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "請求參數無效")
    })
    public ResponseEntity<?> updateProduct(@PathVariable Integer id, @RequestBody ProductRequest productRequest) {
        ProductDTO updatedProduct = productService.updateProduct(id, productRequest);
        return ResponseEntity.ok(ApiResponse.success(updatedProduct));
    }

    @DeleteMapping("/{id}")
    @Operation(
        summary = "刪除商品",
        description = "根據商品ID刪除指定商品"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "商品刪除成功"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "商品不存在")
    })
    public ResponseEntity<?> deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success("Product deleted successfully"));
    }

    @GetMapping("/search")
    @Operation(
        summary = "搜索商品",
        description = "根據關鍵字搜索商品（搜索範圍包括商品名稱和描述）"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取搜索結果")
    })
    public ResponseEntity<?> searchProducts(@RequestParam String keyword) {
        List<ProductDTO> products = productService.searchProducts(keyword);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/price-range")
    @Operation(
        summary = "按價格範圍查詢商品",
        description = "獲取指定價格範圍內的所有商品"
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "成功獲取價格範圍內的商品列表"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "價格範圍參數無效")
    })
    public ResponseEntity<?> getProductsByPriceRange(
            @RequestParam BigDecimal minPrice,
            @RequestParam BigDecimal maxPrice) {
        List<ProductDTO> products = productService.getProductsByPriceRange(minPrice, maxPrice);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable String category) {
        List<ProductDTO> products = productService.getProductsByCategory(category);
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/in-stock")
    public ResponseEntity<?> getInStockProducts() {
        List<ProductDTO> products = productService.getInStockProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestProducts() {
        List<ProductDTO> products = productService.getLatestProducts();
        return ResponseEntity.ok(ApiResponse.success(products));
    }
}