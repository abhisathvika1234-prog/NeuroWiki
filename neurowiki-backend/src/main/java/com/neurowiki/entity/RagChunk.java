package com.neurowiki.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "rag_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RagChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "source_type", nullable = false, length = 30)
    private String sourceType; // KNOWLEDGE, PDF, URL, TEXT

    @Column(name = "source_id", nullable = false)
    private Long sourceId;

    @Column(name = "source_title", nullable = false, length = 255)
    private String sourceTitle;

    @Column(name = "chunk_index", nullable = false)
    private int chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "embedding_data", columnDefinition = "TEXT")
    private String embeddingData;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
