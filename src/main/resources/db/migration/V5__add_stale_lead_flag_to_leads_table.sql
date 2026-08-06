ALTER TABLE leads
    ADD COLUMN stale_lead BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_leads_owner_stale ON leads (owner_id, stale_lead);
