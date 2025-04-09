package com.healthmanagement.model.course;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Table(name = "coach")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Coach {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, length = 255)
    private String expertise;
}
