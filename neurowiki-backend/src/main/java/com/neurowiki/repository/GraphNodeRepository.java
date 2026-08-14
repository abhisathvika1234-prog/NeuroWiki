package com.neurowiki.repository;

import com.neurowiki.entity.GraphNode;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphNodeRepository extends JpaRepository<GraphNode, Long> {

    /**
     * Get all graph nodes belonging to a user.
     */
    List<GraphNode> findByUser(User user);

    /**
     * Find a node only inside the current source.
     *
     * This prevents concepts from different files/URLs
     * from being mixed together.
     */
    Optional<GraphNode> findByUserAndSourceTypeAndSourceIdAndLabelIgnoreCase(
            User user,
            String sourceType,
            Long sourceId,
            String label
    );

    /**
     * Delete all graph nodes belonging to one source.
     */
    void deleteByUserAndSourceTypeAndSourceId(
            User user,
            String sourceType,
            Long sourceId
    );

    /**
     * Get all nodes belonging to one specific source.
     */
    List<GraphNode> findByUserAndSourceTypeAndSourceId(
            User user,
            String sourceType,
            Long sourceId
    );
}