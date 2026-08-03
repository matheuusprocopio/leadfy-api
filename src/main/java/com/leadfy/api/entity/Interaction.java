package com.leadfy.api.entity;

import com.leadfy.api.enums.InteractionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "interactions")
public class Interaction {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private InteractionType type;

	@Column(nullable = false, length = 1000)
	private String description;

	@Column(nullable = false)
	private LocalDateTime interactionDate;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lead_id", nullable = false)
	private Lead lead;

	protected Interaction() {
	}

	public Interaction(InteractionType type, String description, LocalDateTime interactionDate, Lead lead) {
		this.type = type;
		this.description = description;
		this.interactionDate = interactionDate;
		this.lead = lead;
	}

	@PrePersist
	void onCreate() {
		LocalDateTime now = LocalDateTime.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	@PreUpdate
	void onUpdate() {
		this.updatedAt = LocalDateTime.now();
	}

	public void updateDetails(InteractionType type, String description, LocalDateTime interactionDate) {
		this.type = type;
		this.description = description;
		this.interactionDate = interactionDate;
	}

	public Long getId() {
		return id;
	}

	public InteractionType getType() {
		return type;
	}

	public String getDescription() {
		return description;
	}

	public LocalDateTime getInteractionDate() {
		return interactionDate;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public Lead getLead() {
		return lead;
	}
}
