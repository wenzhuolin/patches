package com.patches.plm.domain.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_vote")
public class ReviewVoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @Column(name = "vote", nullable = false, length = 16)
    private String vote;

    @Column(name = "comment", columnDefinition = "text")
    private String comment;

    @Column(name = "voted_at", nullable = false)
    private OffsetDateTime votedAt = OffsetDateTime.now();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getVoterId() {
        return voterId;
    }

    public void setVoterId(Long voterId) {
        this.voterId = voterId;
    }

    public String getVote() {
        return vote;
    }

    public void setVote(String vote) {
        this.vote = vote;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public OffsetDateTime getVotedAt() {
        return votedAt;
    }

    public void setVotedAt(OffsetDateTime votedAt) {
        this.votedAt = votedAt;
    }
}
