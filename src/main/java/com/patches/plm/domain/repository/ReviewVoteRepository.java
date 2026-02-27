package com.patches.plm.domain.repository;

import com.patches.plm.domain.entity.ReviewVoteEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewVoteRepository extends JpaRepository<ReviewVoteEntity, Long> {
    java.util.Optional<ReviewVoteEntity> findBySessionIdAndVoterId(Long sessionId, Long voterId);

    long countBySessionId(Long sessionId);

    long countBySessionIdAndVote(Long sessionId, String vote);
}
