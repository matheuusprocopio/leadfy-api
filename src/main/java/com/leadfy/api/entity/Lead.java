package com.leadfy.api.entity;

import com.leadfy.api.enums.LeadSource;
import com.leadfy.api.enums.LeadStatus;
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
@Table(name = "leads")
public class Lead {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, length = 120)
	private String name;

	@Column(length = 120)
	private String company;

	@Column(length = 160)
	private String email;

	@Column(length = 30)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LeadSource source;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private LeadStatus status;

	@Column(length = 1000)
	private String notes;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	@Column(nullable = false)
	private LocalDateTime updatedAt;

	private LocalDateTime closedAt;

	@Column(name = "stale_lead", nullable = false)
	private boolean staleLead;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "owner_id", nullable = false)
	private User owner;

	protected Lead() {
	}

	public Lead(
			String name,
			String company,
			String email,
			String phone,
			LeadSource source,
			String notes,
			User owner
	) {
		this.name = name;
		this.company = company;
		this.email = email;
		this.phone = phone;
		this.source = source;
		this.status = LeadStatus.NEW;
		this.notes = notes;
		this.owner = owner;
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

	public void updateDetails(
			String name,
			String company,
			String email,
			String phone,
			LeadSource source,
			String notes
	) {
		this.name = name;
		this.company = company;
		this.email = email;
		this.phone = phone;
		this.source = source;
		this.notes = notes;
	}

	public void updateStatus(LeadStatus status) {
		if (this.status == status) {
			return;
		}

		this.status = status;
		this.closedAt = status == LeadStatus.CLOSED ? LocalDateTime.now() : null;

		if (status == LeadStatus.CLOSED || status == LeadStatus.LOST) {
			this.staleLead = false;
		}
	}

	public void markAsStale() {
		this.staleLead = true;
	}

	public void clearStaleFlag() {
		this.staleLead = false;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getCompany() {
		return company;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public LeadSource getSource() {
		return source;
	}

	public LeadStatus getStatus() {
		return status;
	}

	public String getNotes() {
		return notes;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public LocalDateTime getClosedAt() {
		return closedAt;
	}

	public boolean isStaleLead() {
		return staleLead;
	}

	public User getOwner() {
		return owner;
	}
}
