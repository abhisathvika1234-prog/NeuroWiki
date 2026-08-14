package com.neurowiki.repository;

import com.neurowiki.entity.KnowledgePage;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface KnowledgePageRepository extends JpaRepository<KnowledgePage, Long> {
    List<KnowledgePage> findByUserOrderByUpdatedAtDesc(User user);
    Optional<KnowledgePage> findByIdAndUser(Long id, User user);
    List<KnowledgePage> findByUserAndFavoriteTrueOrderByUpdatedAtDesc(User user);
    List<KnowledgePage> findByUserAndCategoryOrderByUpdatedAtDesc(User user, String category);

    @Query("SELECT k FROM KnowledgePage k WHERE k.user = :user AND " +
           "(LOWER(k.title) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(k.content) LIKE LOWER(CONCAT('%', :query, '%')) OR LOWER(k.tags) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<KnowledgePage> searchByUserAndQuery(@Param("user") User user, @Param("query") String query);

    long countByUser(User user);
    long countByUserAndFavoriteTrue(User user);
    List<KnowledgePage> findTop5ByUserOrderByUpdatedAtDesc(User user);
}
