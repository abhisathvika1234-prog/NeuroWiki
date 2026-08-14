package com.neurowiki.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "graph_edges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GraphEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_node_id", nullable = false)
    private GraphNode sourceNode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_node_id", nullable = false)
    private GraphNode targetNode;

    @Column(nullable = false, length = 50)
    private String relationship; // USES, CONTAINS, RELATES_TO, EXTENDS

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
