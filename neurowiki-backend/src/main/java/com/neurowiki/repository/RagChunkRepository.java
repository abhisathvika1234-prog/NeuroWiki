package com.neurowiki.repository;

import com.neurowiki.entity.RagChunk;
import com.neurowiki.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RagChunkRepository extends JpaRepository<RagChunk, Long> {

    List<RagChunk> findByUser(User user);

    List<RagChunk> findByUserAndSourceTypeAndSourceId(User user, String sourceType, Long sourceId);

    void deleteByUserAndSourceTypeAndSourceId(User user, String sourceType, Long sourceId);
}
