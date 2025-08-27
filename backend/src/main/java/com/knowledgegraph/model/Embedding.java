package com.knowledgegraph.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "embeddings", schema = "kg")
@Data
@EqualsAndHashCode(of = {"id"})
@ToString(exclude = {"node", "document", "vector"}) // Exclude large fields and relations
public class Embedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "node_id")
    @JsonIgnore
    private Node node;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    @JsonIgnore
    private Document document;

    @Column(name = "content_snippet", columnDefinition = "TEXT")
    private String contentSnippet;

    // Using float array for now, will be converted to vector type in production
    @Column(columnDefinition = "float4[]")
    private float[] vector;

    @Column(name = "model_version", length = 50)
    private String modelVersion = "mock";

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Convenience getters for node and document IDs
    public UUID getNodeId() {
        return node != null ? node.getId() : null;
    }

    public UUID getDocumentId() {
        return document != null ? document.getId() : null;
    }
}