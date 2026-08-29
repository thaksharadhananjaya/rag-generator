package com.rag.raggeneratorapplication.knowledgebase.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A named set of documents that questions are answered against. Owned by the
 * knowledgebase module; other modules reference it only by {@link #getId()}.
 *
 * <p>State changes go through the intent-revealing methods below, so this entity
 * exposes {@code @Getter} but no {@code @Setter}.
 */
@Entity
@Table(name = "knowledge_base")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "text")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
