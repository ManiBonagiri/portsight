-- Generated Reports Table (matches GeneratedReport.java)
CREATE TABLE generated_reports (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    report_type VARCHAR(50) NOT NULL,
    storage_path TEXT,
    status VARCHAR(50) NOT NULL,
    generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_generated_reports_portfolio_id ON generated_reports(portfolio_id);