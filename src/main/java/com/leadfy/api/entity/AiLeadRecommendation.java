package com.leadfy.api.entity;

import com.leadfy.api.enums.AiRecommendationStatus;
import com.leadfy.api.service.NormalizedAiLeadInsight;
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
import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_lead_recommendations")
public class AiLeadRecommendation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lead_id", nullable = false)
	private Lead lead;

	@Column(nullable = false)
	private Integer priorityScore;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String summary;

	@Column(name = "conversion_signals_json", nullable = false, columnDefinition = "TEXT")
	private String conversionSignalsJson;

	@Column(name = "risk_signals_json", nullable = false, columnDefinition = "TEXT")
	private String riskSignalsJson;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String nextBestAction;

	@Column(nullable = false, columnDefinition = "TEXT")
	private String suggestedMessage;

	@Column(nullable = false, length = 20)
	private String confidence;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private AiRecommendationStatus status;

	private Boolean useful;

	@Column(nullable = false)
	private boolean active;

	@Column(nullable = false)
	private Instant generatedAt;

	private Instant reviewedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	protected AiLeadRecommendation() {
	}

	public AiLeadRecommendation(
			Lead lead,
			String conversionSignalsJson,
			String riskSignalsJson,
			NormalizedAiLeadInsight insight,
			Instant generatedAt
	) {
		this.owner = lead.getOwner();
		this.lead = lead;
		this.priorityScore = insight.priorityScore();
		this.summary = insight.summary();
		this.conversionSignalsJson = conversionSignalsJson;
		this.riskSignalsJson = riskSignalsJson;
		this.nextBestAction = insight.nextBestAction();
		this.suggestedMessage = insight.suggestedMessage();
		this.confidence = insight.confidence();
		this.status = AiRecommendationStatus.PENDING;
		this.active = true;
		this.generatedAt = generatedAt;
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

	public void markInactive() {
		this.active = false;
	}

	public void updateFeedback(AiRecommendationStatus status, Boolean useful) {
		this.status = status;
		this.useful = status == AiRecommendationStatus.PENDING ? null : useful;
		this.active = status == AiRecommendationStatus.PENDING;
		this.reviewedAt = status == AiRecommendationStatus.PENDING ? null : Instant.now();
	}

	public Long getId() {
		return id;
	}

	public User getOwner() {
		return owner;
	}

	public Lead getLead() {
		return lead;
	}

	public Integer getPriorityScore() {
		return priorityScore;
	}

	public String getSummary() {
		return summary;
	}

	public String getConversionSignalsJson() {
		return conversionSignalsJson;
	}

	public String getRiskSignalsJson() {
		return riskSignalsJson;
	}

	public String getNextBestAction() {
		return nextBestAction;
	}

	public String getSuggestedMessage() {
		return suggestedMessage;
	}

	public String getConfidence() {
		return confidence;
	}

	public AiRecommendationStatus getStatus() {
		return status;
	}

	public Boolean getUseful() {
		return useful;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getGeneratedAt() {
		return generatedAt;
	}

	public Instant getReviewedAt() {
		return reviewedAt;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}
}
