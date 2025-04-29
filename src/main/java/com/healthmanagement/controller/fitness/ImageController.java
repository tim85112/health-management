package com.healthmanagement.controller.fitness;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/static-images") // 修改這裡
public class ImageController {

    private static final String IMAGE_DIR = "src/main/resources/static/images"; // 顯式指定路徑

    @GetMapping("/{imageName:.+}")
    public ResponseEntity<Resource> getImage(@PathVariable String imageName) throws IOException {
        try {
            Path imagePath = Paths.get(IMAGE_DIR, imageName);
            Resource resource = new UrlResource(imagePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                MediaType mediaType;
                if (imageName.toLowerCase().endsWith(".png")) {
                    mediaType = MediaType.IMAGE_PNG;
                } else if (imageName.toLowerCase().endsWith(".jpg") || imageName.toLowerCase().endsWith(".jpeg")) {
                    mediaType = MediaType.IMAGE_JPEG;
                } else {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM; // 預設為二進制流
                }

                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}