CREATE TABLE leads (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    company VARCHAR(120),
    email VARCHAR(160),
    phone VARCHAR(30),
    source VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    notes VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    closed_at TIMESTAMP,
    owner_id BIGINT NOT NULL,
    CONSTRAINT fk_leads_owner FOREIGN KEY (owner_id) REFERENCES users (id)
);

CREATE INDEX idx_leads_owner_id ON leads (owner_id);
CREATE INDEX idx_leads_owner_status ON leads (owner_id, status);
CREATE INDEX idx_leads_owner_source ON leads (owner_id, source);
