package com.leadfy.api.entity;

import com.leadfy.api.enums.ProposalStatus;
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
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "proposals")
public class Proposal {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, precision = 12, scale = 2)
	private BigDecimal amount;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private ProposalStatus status;

	@Column(nullable = false)
	private LocalDate sentAt;

	private LocalDate respondedAt;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "lead_id", nullable = false)
	private Lead lead;

	protected Proposal() {
	}

	public Proposal(BigDecimal amount, LocalDate sentAt, Lead lead) {
		this.amount = amount;
		this.sentAt = sentAt;
		this.status = ProposalStatus.SENT;
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

	public void updateDetails(BigDecimal amount, LocalDate sentAt) {
		this.amount = amount;
		this.sentAt = sentAt;
	}

	public void updateStatus(ProposalStatus status, LocalDate respondedAt) {
		this.status = status;
		this.respondedAt = status == ProposalStatus.SENT ? null : respondedAt;
	}

	public Long getId() {
		return id;
	}

	public BigDecimal getAmount() {
		return amount;
	}

	public ProposalStatus getStatus() {
		return status;
	}

	public LocalDate getSentAt() {
		return sentAt;
	}

	public LocalDate getRespondedAt() {
		return respondedAt;
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
