package com.neurowiki.service;

import com.neurowiki.dto.GraphResponse;
import com.neurowiki.entity.GraphEdge;
import com.neurowiki.entity.GraphNode;
import com.neurowiki.entity.User;
import com.neurowiki.repository.GraphEdgeRepository;
import com.neurowiki.repository.GraphNodeRepository;
import com.neurowiki.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@Transactional
public class GraphService {

    private final GraphNodeRepository graphNodeRepository;
    private final GraphEdgeRepository graphEdgeRepository;
    private final SecurityUtils securityUtils;

    public GraphService(
            GraphNodeRepository graphNodeRepository,
            GraphEdgeRepository graphEdgeRepository,
            SecurityUtils securityUtils
    ) {
        this.graphNodeRepository = graphNodeRepository;
        this.graphEdgeRepository = graphEdgeRepository;
        this.securityUtils = securityUtils;
    }

    // =========================================================
    // GET COMPLETE GRAPH
    // =========================================================

    @Transactional(readOnly = true)
    public GraphResponse getGraph() {

        User user = securityUtils.getCurrentAuthenticatedUser();

        List<GraphNode> nodes =
                graphNodeRepository.findByUser(user);

        if (nodes == null || nodes.isEmpty()) {
            return emptyGraph();
        }

        List<GraphEdge> edges =
                graphEdgeRepository.findByUser(user);

        return buildGraphResponse(nodes, edges);
    }

    // =========================================================
    // ALIAS USED BY CONTROLLER
    // =========================================================

    @Transactional(readOnly = true)
    public GraphResponse getGraphForCurrentUser() {
        return getGraph();
    }

    // =========================================================
    // GET GRAPH BY SOURCE
    // =========================================================

    @Transactional(readOnly = true)
    public GraphResponse getGraphBySource(
            String sourceType,
            Long sourceId
    ) {

        User user =
                securityUtils.getCurrentAuthenticatedUser();

        validateSource(sourceType, sourceId);

        String normalizedSourceType =
                sourceType.trim().toUpperCase();

        List<GraphNode> nodes =
                graphNodeRepository
                        .findByUserAndSourceTypeAndSourceId(
                                user,
                                normalizedSourceType,
                                sourceId
                        );

        if (nodes == null || nodes.isEmpty()) {
            return emptyGraph();
        }

        Set<Long> nodeIds = new HashSet<>();

        for (GraphNode node : nodes) {

            if (node != null &&
                    node.getId() != null) {

                nodeIds.add(node.getId());
            }
        }

        List<GraphEdge> allEdges =
                graphEdgeRepository.findByUser(user);

        List<GraphEdge> sourceEdges =
                allEdges.stream()
                        .filter(Objects::nonNull)
                        .filter(edge -> {

                            if (edge.getSourceNode() == null ||
                                    edge.getTargetNode() == null) {
                                return false;
                            }

                            Long sourceNodeId =
                                    edge.getSourceNode().getId();

                            Long targetNodeId =
                                    edge.getTargetNode().getId();

                            if (sourceNodeId == null ||
                                    targetNodeId == null) {
                                return false;
                            }

                            return nodeIds.contains(sourceNodeId)
                                    && nodeIds.contains(targetNodeId);
                        })
                        .toList();

        return buildGraphResponse(
                nodes,
                sourceEdges
        );
    }

    // =========================================================
    // METHOD USED BY CONTROLLER
    // =========================================================

    @Transactional(readOnly = true)
    public GraphResponse getGraphForSource(
            User user,
            Long sourceId
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "user is required"
            );
        }

        if (sourceId == null) {
            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        /*
         * KnowledgeDocument types are normally:
         *
         * PDF
         * URL
         * TEXT
         *
         * The graph itself stores sourceType.
         *
         * Because this method only receives sourceId,
         * check all supported source types.
         */

        List<GraphNode> nodes =
                graphNodeRepository
                        .findByUserAndSourceTypeAndSourceId(
                                user,
                                "PDF",
                                sourceId
                        );

        if (nodes == null || nodes.isEmpty()) {

            nodes =
                    graphNodeRepository
                            .findByUserAndSourceTypeAndSourceId(
                                    user,
                                    "URL",
                                    sourceId
                            );
        }

        if (nodes == null || nodes.isEmpty()) {

            nodes =
                    graphNodeRepository
                            .findByUserAndSourceTypeAndSourceId(
                                    user,
                                    "TEXT",
                                    sourceId
                            );
        }

        if (nodes == null || nodes.isEmpty()) {

            nodes =
                    graphNodeRepository
                            .findByUserAndSourceTypeAndSourceId(
                                    user,
                                    "KNOWLEDGE",
                                    sourceId
                            );
        }

        if (nodes == null || nodes.isEmpty()) {
            return emptyGraph();
        }

        Set<Long> nodeIds = new HashSet<>();

        for (GraphNode node : nodes) {

            if (node != null &&
                    node.getId() != null) {

                nodeIds.add(node.getId());
            }
        }

        List<GraphEdge> allEdges =
                graphEdgeRepository.findByUser(user);

        List<GraphEdge> sourceEdges =
                allEdges.stream()
                        .filter(Objects::nonNull)
                        .filter(edge -> {

                            if (edge.getSourceNode() == null ||
                                    edge.getTargetNode() == null) {
                                return false;
                            }

                            Long sourceNodeId =
                                    edge.getSourceNode().getId();

                            Long targetNodeId =
                                    edge.getTargetNode().getId();

                            if (sourceNodeId == null ||
                                    targetNodeId == null) {
                                return false;
                            }

                            return nodeIds.contains(sourceNodeId)
                                    && nodeIds.contains(targetNodeId);
                        })
                        .toList();

        return buildGraphResponse(
                nodes,
                sourceEdges
        );
    }

    // =========================================================
    // FIND OR CREATE NODE
    // =========================================================

    public GraphNode findOrCreateNode(
            User user,
            String label,
            String type,
            String sourceType,
            Long sourceId
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "user is required"
            );
        }

        if (label == null || label.isBlank()) {
            throw new IllegalArgumentException(
                    "label is required"
            );
        }

        if (sourceType == null ||
                sourceType.isBlank()) {

            throw new IllegalArgumentException(
                    "sourceType is required"
            );
        }

        if (sourceId == null) {
            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        String cleanLabel =
                label.trim();

        String cleanSourceType =
                sourceType.trim().toUpperCase();

        String cleanType =
                type == null || type.isBlank()
                        ? "CONCEPT"
                        : type.trim().toUpperCase();

        var existing =
                graphNodeRepository
                        .findByUserAndSourceTypeAndSourceIdAndLabelIgnoreCase(
                                user,
                                cleanSourceType,
                                sourceId,
                                cleanLabel
                        );

        if (existing.isPresent()) {

            GraphNode node =
                    existing.get();

            if (!Objects.equals(
                    node.getType(),
                    cleanType
            )) {

                node.setType(cleanType);
            }

            return graphNodeRepository.save(node);
        }

        GraphNode node =
                GraphNode.builder()
                        .user(user)
                        .label(cleanLabel)
                        .type(cleanType)
                        .sourceType(cleanSourceType)
                        .sourceId(sourceId)
                        .build();

        return graphNodeRepository.save(node);
    }

    // =========================================================
    // FIND OR CREATE EDGE
    // =========================================================

    public GraphEdge findOrCreateEdge(
            User user,
            GraphNode sourceNode,
            GraphNode targetNode,
            String relationship
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "user is required"
            );
        }

        if (sourceNode == null) {
            throw new IllegalArgumentException(
                    "sourceNode is required"
            );
        }

        if (targetNode == null) {
            throw new IllegalArgumentException(
                    "targetNode is required"
            );
        }

        if (relationship == null ||
                relationship.isBlank()) {

            throw new IllegalArgumentException(
                    "relationship is required"
            );
        }

        if (!Objects.equals(
                sourceNode.getUser().getId(),
                user.getId()
        )) {

            throw new IllegalArgumentException(
                    "Source node does not belong to current user"
            );
        }

        if (!Objects.equals(
                targetNode.getUser().getId(),
                user.getId()
        )) {

            throw new IllegalArgumentException(
                    "Target node does not belong to current user"
            );
        }

        if (!Objects.equals(
                sourceNode.getSourceType(),
                targetNode.getSourceType()
        )) {

            throw new IllegalArgumentException(
                    "Source nodes must belong to the same source type"
            );
        }

        if (!Objects.equals(
                sourceNode.getSourceId(),
                targetNode.getSourceId()
        )) {

            throw new IllegalArgumentException(
                    "Source nodes must belong to the same source"
            );
        }

        String cleanRelationship =
                relationship.trim().toUpperCase();

        var existing =
                graphEdgeRepository
                        .findByUserAndSourceNodeAndTargetNodeAndRelationship(
                                user,
                                sourceNode,
                                targetNode,
                                cleanRelationship
                        );

        if (existing.isPresent()) {
            return existing.get();
        }

        GraphEdge edge =
                GraphEdge.builder()
                        .user(user)
                        .sourceNode(sourceNode)
                        .targetNode(targetNode)
                        .relationship(cleanRelationship)
                        .build();

        return graphEdgeRepository.save(edge);
    }

    // =========================================================
    // PROCESS AND EXTRACT GRAPH
    // =========================================================

    public void processAndExtractGraph(
            User user,
            String sourceType,
            Long sourceId,
            String title,
            String content
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "user is required"
            );
        }

        if (sourceType == null ||
                sourceType.isBlank()) {

            throw new IllegalArgumentException(
                    "sourceType is required"
            );
        }

        if (sourceId == null) {
            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }

        if (content == null ||
                content.isBlank()) {

            return;
        }

        /*
         * Remove old graph first.
         *
         * This is important when a knowledge page
         * or document is updated.
         */
        deleteGraphBySource(
                user,
                sourceType,
                sourceId
        );

        /*
         * Create the main source node.
         */
        GraphNode sourceNode =
                findOrCreateNode(
                        user,
                        title == null || title.isBlank()
                                ? "Untitled Source"
                                : title,
                        "KNOWLEDGE",
                        sourceType,
                        sourceId
                );

        /*
         * Simple concept extraction.
         *
         * This creates a useful baseline graph
         * without requiring another AI call.
         */
        String cleaned =
                content
                        .replaceAll(
                                "[^a-zA-Z0-9\\s]",
                                " "
                        )
                        .toLowerCase();

        String[] words =
                cleaned.split("\\s+");

        Set<String> concepts =
                new HashSet<>();

        for (String word : words) {

            if (word.length() >= 5 &&
                    word.length() <= 40) {

                concepts.add(word);
            }
        }

        /*
         * Limit the graph size.
         *
         * Without a limit a large PDF could
         * create thousands of nodes.
         */
        int maximumConcepts = 30;
        int count = 0;

        for (String concept : concepts) {

            if (count >= maximumConcepts) {
                break;
            }

            GraphNode conceptNode =
                    findOrCreateNode(
                            user,
                            concept,
                            "CONCEPT",
                            sourceType,
                            sourceId
                    );

            findOrCreateEdge(
                    user,
                    sourceNode,
                    conceptNode,
                    "CONTAINS"
            );

            count++;
        }
    }

    // =========================================================
    // DELETE GRAPH BY SOURCE
    // =========================================================

    public void deleteGraphBySource(
            String sourceType,
            Long sourceId
    ) {

        User user =
                securityUtils.getCurrentAuthenticatedUser();

        deleteGraphBySource(
                user,
                sourceType,
                sourceId
        );
    }

    // =========================================================
    // DELETE GRAPH BY SOURCE + USER
    // =========================================================

    private void deleteGraphBySource(
            User user,
            String sourceType,
            Long sourceId
    ) {

        if (user == null) {
            throw new IllegalArgumentException(
                    "user is required"
            );
        }

        validateSource(
                sourceType,
                sourceId
        );

        String cleanSourceType =
                sourceType.trim().toUpperCase();

        List<GraphNode> nodes =
                graphNodeRepository
                        .findByUserAndSourceTypeAndSourceId(
                                user,
                                cleanSourceType,
                                sourceId
                        );

        if (nodes == null || nodes.isEmpty()) {
            return;
        }

        Set<Long> nodeIds =
                new HashSet<>();

        for (GraphNode node : nodes) {

            if (node != null &&
                    node.getId() != null) {

                nodeIds.add(node.getId());
            }
        }

        List<GraphEdge> edges =
                graphEdgeRepository.findByUser(user);

        for (GraphEdge edge : edges) {

            if (edge == null ||
                    edge.getSourceNode() == null ||
                    edge.getTargetNode() == null) {

                continue;
            }

            Long sourceNodeId =
                    edge.getSourceNode().getId();

            Long targetNodeId =
                    edge.getTargetNode().getId();

            if (nodeIds.contains(sourceNodeId) ||
                    nodeIds.contains(targetNodeId)) {

                graphEdgeRepository.delete(edge);
            }
        }

        graphNodeRepository
                .deleteByUserAndSourceTypeAndSourceId(
                        user,
                        cleanSourceType,
                        sourceId
                );
    }

    // =========================================================
    // BUILD RESPONSE
    // =========================================================

    private GraphResponse buildGraphResponse(
            List<GraphNode> nodes,
            List<GraphEdge> edges
    ) {

        if (nodes == null) {
            nodes = Collections.emptyList();
        }

        if (edges == null) {
            edges = Collections.emptyList();
        }

        List<GraphResponse.NodeDto> nodeDtos =
                nodes.stream()
                        .filter(Objects::nonNull)
                        .map(this::convertNode)
                        .toList();

        List<GraphResponse.EdgeDto> edgeDtos =
                edges.stream()
                        .filter(Objects::nonNull)
                        .map(this::convertEdge)
                        .toList();

        return new GraphResponse(
                nodeDtos,
                edgeDtos
        );
    }

    // =========================================================
    // NODE CONVERSION
    // =========================================================

    private GraphResponse.NodeDto convertNode(
            GraphNode node
    ) {

        return GraphResponse.NodeDto.builder()
                .id(
                        node.getId() != null
                                ? String.valueOf(node.getId())
                                : ""
                )
                .label(node.getLabel())
                .type(node.getType())
                .sourceId(node.getSourceId())
                .sourceType(node.getSourceType())
                .build();
    }

    // =========================================================
    // EDGE CONVERSION
    // =========================================================

    private GraphResponse.EdgeDto convertEdge(
            GraphEdge edge
    ) {

        String sourceId =
                edge.getSourceNode() != null &&
                        edge.getSourceNode().getId() != null
                        ? String.valueOf(
                                edge.getSourceNode().getId()
                        )
                        : "";

        String targetId =
                edge.getTargetNode() != null &&
                        edge.getTargetNode().getId() != null
                        ? String.valueOf(
                                edge.getTargetNode().getId()
                        )
                        : "";

        return GraphResponse.EdgeDto.builder()
                .id(
                        edge.getId() != null
                                ? String.valueOf(edge.getId())
                                : ""
                )
                .source(sourceId)
                .target(targetId)
                .relationship(edge.getRelationship())
                .build();
    }

    // =========================================================
    // EMPTY GRAPH
    // =========================================================

    private GraphResponse emptyGraph() {

        return new GraphResponse(
                Collections.emptyList(),
                Collections.emptyList()
        );
    }

    // =========================================================
    // VALIDATION
    // =========================================================

    private void validateSource(
            String sourceType,
            Long sourceId
    ) {

        if (sourceType == null ||
                sourceType.isBlank()) {

            throw new IllegalArgumentException(
                    "sourceType is required"
            );
        }

        if (sourceId == null) {

            throw new IllegalArgumentException(
                    "sourceId is required"
            );
        }
    }
}