package com.healthmanagement.model.social;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Table(name = "media_file")
public class MediaFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String url;

    private String type; // avatar, post

    @Column(name = "ref_id")
    private Integer refId;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
}
