package com.neurowiki.repository;

import com.neurowiki.entity.AiHistory;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiHistoryRepository extends JpaRepository<AiHistory, Long> {
    List<AiHistory> findByUserOrderByTimestampDesc(User user);
    long countByUser(User user);
}
