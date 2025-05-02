package com.healthmanagement.controller.fitness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthmanagement.dto.fitness.ChatRequestDTO;
import com.healthmanagement.dto.fitness.RecommendationDTO;
import com.healthmanagement.service.fitness.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/{userId}/recommendations")
@RequiredArgsConstructor
@Tag(name = "Fitness Tracking", description = "健身追蹤管理 API - 個性化建議")
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Value("${gemini.api.key}")
    private String geminiApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    @Operation(summary = "獲取用戶的個性化健身建議")
    public ResponseEntity<List<RecommendationDTO>> getRecommendations(
            @Parameter(description = "用戶 ID") @PathVariable Integer userId) {
        List<RecommendationDTO> recommendations = recommendationService.getRecommendationsForUser(userId);
        return ResponseEntity.ok(recommendations);
    }

    @PostMapping("/chat")
    @Operation(summary = "與 Gemini 對話，提供健身建議")
    public ResponseEntity<String> chatWithGemini(
            @PathVariable Integer userId,
            @RequestBody @Valid ChatRequestDTO requestBody
    ) {

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=" + geminiApiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // 檢查 userInput 是否為 null，如果是則使用空字符串
        String userInput = requestBody.getPrompt();
        if (userInput == null) {
            userInput = ""; // 使用空字符串替代 null
        }

        Map<String, Object> body = Map.of(
                "contents", List.of(
                        Map.of(
                                "role", "user",
                                "parts", List.of(
                                        Map.of("text", userInput)
                                )
                        )
                )
        );

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, requestEntity, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            // 解析 Gemini 回應的 text
            String content = root.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText();
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Gemini 回應失敗：" + e.getMessage());
        }
    }
}