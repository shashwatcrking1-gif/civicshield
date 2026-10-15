package com.civicshield.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/** Message submitted through the public contact form. */
@Entity
@Table(name = "contact_messages")
@Getter
@Setter
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 2000)
    private String message;

    @Column(nullable = false)
    private LocalDateTime createdDate;

    public ContactMessage() {
    }

    @Builder
    public ContactMessage(Long id, String name, String email, String message, LocalDateTime createdDate) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.message = message;
        this.createdDate = createdDate;
    }

    @PrePersist
    void onCreate() {
        if (createdDate == null) createdDate = LocalDateTime.now();
    }
}
