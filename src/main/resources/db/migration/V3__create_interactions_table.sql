CREATE TABLE interactions (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(30) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    interaction_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    lead_id BIGINT NOT NULL,
    CONSTRAINT fk_interactions_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE CASCADE
);

CREATE INDEX idx_interactions_lead_id ON interactions (lead_id);
CREATE INDEX idx_interactions_lead_date ON interactions (lead_id, interaction_date DESC);
