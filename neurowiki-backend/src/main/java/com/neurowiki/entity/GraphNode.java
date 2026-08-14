package com.neurowiki.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "graph_nodes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphNode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(nullable = false, length = 30)
    private String type; // CONCEPT, KNOWLEDGE, PDF

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "source_type", length = 30)
    private String sourceType; // KNOWLEDGE, PDF

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
