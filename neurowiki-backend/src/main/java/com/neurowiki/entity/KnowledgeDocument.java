package com.neurowiki.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "knowledge_documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, length = 20)
    private String type; // PDF, URL, TEXT

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 500)
    private String sourceUrl;

    @Column(nullable = false, length = 20)
    private String status; // PROCESSED, PENDING, FAILED

    @Column(length = 50)
    private String fileSize;

    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Column(name = "concepts_extracted_count", nullable = false)
    private int conceptsExtractedCount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        this.addedAt = LocalDateTime.now();
    }
}
