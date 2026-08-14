package com.neurowiki.repository;

import com.neurowiki.entity.GraphEdge;
import com.neurowiki.entity.GraphNode;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GraphEdgeRepository extends JpaRepository<GraphEdge, Long> {

    List<GraphEdge> findByUser(User user);

    Optional<GraphEdge> findByUserAndSourceNodeAndTargetNodeAndRelationship(
            User user, GraphNode sourceNode, GraphNode targetNode, String relationship);

    void deleteByUserAndSourceNodeOrUserAndTargetNode(User user1, GraphNode sourceNode, User user2, GraphNode targetNode);
}
