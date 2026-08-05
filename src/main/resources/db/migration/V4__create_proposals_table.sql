CREATE TABLE proposals (
    id BIGSERIAL PRIMARY KEY,
    amount NUMERIC(12, 2) NOT NULL,
    status VARCHAR(30) NOT NULL,
    sent_at DATE NOT NULL,
    responded_at DATE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    lead_id BIGINT NOT NULL,
    CONSTRAINT fk_proposals_lead FOREIGN KEY (lead_id) REFERENCES leads (id) ON DELETE CASCADE
);

CREATE INDEX idx_proposals_lead_id ON proposals (lead_id);
CREATE INDEX idx_proposals_lead_status ON proposals (lead_id, status);
CREATE INDEX idx_proposals_lead_sent_at ON proposals (lead_id, sent_at DESC);
