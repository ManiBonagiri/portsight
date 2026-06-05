-- Enable UUID-OSSP extension
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Users Table
CREATE TABLE users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash TEXT NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- User Sessions Table
CREATE TABLE user_sessions (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    refresh_token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Outbox Events Table
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(255) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

-- Idempotency Keys Table
CREATE TABLE idempotency_keys (
    key_hash VARCHAR(255) PRIMARY KEY,
    response_payload TEXT,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Assets Table (matches Asset.java)
CREATE TABLE assets (
    id UUID PRIMARY KEY,
    ticker VARCHAR(20) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sector VARCHAR(50),
    asset_type VARCHAR(50) NOT NULL,
    current_price NUMERIC(19,4),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(ticker)
);

-- Asset Price History Table (matches AssetPriceHistory.java)
CREATE TABLE asset_price_history (
    id UUID PRIMARY KEY,
    asset_id UUID NOT NULL REFERENCES assets(id) ON DELETE CASCADE,
    record_date DATE NOT NULL,
    closing_price NUMERIC(19,4) NOT NULL,
    UNIQUE(asset_id, record_date)
);

-- Portfolios Table (matches Portfolio.java)
CREATE TABLE portfolios (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    portfolio_name VARCHAR(100) NOT NULL,
    risk_profile VARCHAR(50) NOT NULL,
    benchmark VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Holdings Table (matches Holding.java)
CREATE TABLE holdings (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    asset_id UUID NOT NULL REFERENCES assets(id),
    quantity NUMERIC(19,4) NOT NULL,
    average_price NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(portfolio_id, asset_id)
);

-- Transactions Table (matches Transaction.java)
CREATE TABLE transactions (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    asset_id UUID,
    transaction_type VARCHAR(50) NOT NULL,
    quantity NUMERIC(19,4) NOT NULL,
    price NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Risk Metrics Table (matches RiskMetrics.java)
CREATE TABLE risk_metrics (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    volatility NUMERIC(19,4),
    beta NUMERIC(19,4),
    sharpe_ratio NUMERIC(19,4),
    var_95 NUMERIC(19,4),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    UNIQUE(portfolio_id)
);

-- Portfolio Snapshots Table (matches PortfolioSnapshot.java)
CREATE TABLE portfolio_snapshots (
    id UUID PRIMARY KEY,
    portfolio_id UUID NOT NULL REFERENCES portfolios(id) ON DELETE CASCADE,
    snapshot_date DATE NOT NULL,
    total_value NUMERIC(19,4),
    invested_amount NUMERIC(19,4),
    realized_gain NUMERIC(19,4),
    unrealized_gain NUMERIC(19,4),
    created_at TIMESTAMP,
    UNIQUE(portfolio_id, snapshot_date)
);

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_user_sessions_token ON user_sessions(refresh_token);
CREATE INDEX idx_outbox_events_status ON outbox_events(status) WHERE status = 'PENDING';
CREATE INDEX idx_assets_ticker ON assets(ticker);
CREATE INDEX idx_asset_price_history_asset_date ON asset_price_history(asset_id, record_date);
CREATE INDEX idx_portfolios_user_id ON portfolios(user_id);
CREATE INDEX idx_holdings_portfolio_id ON holdings(portfolio_id);
CREATE INDEX idx_transactions_portfolio_id ON transactions(portfolio_id);
CREATE INDEX idx_risk_metrics_portfolio_id ON risk_metrics(portfolio_id);
CREATE INDEX idx_portfolio_snapshots_portfolio_date ON portfolio_snapshots(portfolio_id, snapshot_date);
