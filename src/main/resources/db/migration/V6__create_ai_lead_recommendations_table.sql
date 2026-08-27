CREATE TABLE ai_lead_recommendations (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    lead_id BIGINT NOT NULL,
    priority_score INTEGER NOT NULL,
    summary TEXT NOT NULL,
    conversion_signals_json TEXT NOT NULL,
    risk_signals_json TEXT NOT NULL,
    next_best_action TEXT NOT NULL,
    suggested_message TEXT NOT NULL,
    confidence VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    useful BOOLEAN,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    generated_at TIMESTAMP NOT NULL,
    reviewed_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_ai_recommendations_owner FOREIGN KEY (owner_id) REFERENCES users (id),
    CONSTRAINT fk_ai_recommendations_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE CASCADE,
    CONSTRAINT chk_ai_recommendations_priority CHECK (priority_score BETWEEN 0 AND 100)
);

CREATE INDEX idx_ai_recommendations_owner_active_priority
    ON ai_lead_recommendations (owner_id, active, priority_score DESC, generated_at DESC);

CREATE INDEX idx_ai_recommendations_owner_status
    ON ai_lead_recommendations (owner_id, status);

CREATE INDEX idx_ai_recommendations_lead_generated
    ON ai_lead_recommendations (lead_id, generated_at DESC);
