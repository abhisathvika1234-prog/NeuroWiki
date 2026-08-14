package com.neurowiki.repository;

import com.neurowiki.entity.KnowledgeDocument;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, Long> {
    List<KnowledgeDocument> findByUserOrderByAddedAtDesc(User user);
    Optional<KnowledgeDocument> findByIdAndUser(Long id, User user);

    @Query("SELECT d FROM KnowledgeDocument d WHERE d.user = :user AND " +
           "(LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgeDocument> searchByUserAndQuery(@Param("user") User user, @Param("query") String query);

    long countByUser(User user);
    List<KnowledgeDocument> findTop5ByUserOrderByAddedAtDesc(User user);
}
