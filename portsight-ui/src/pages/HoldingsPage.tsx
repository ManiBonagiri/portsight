import { useState, useEffect } from 'react';
import { RefreshCw, TrendingUp, TrendingDown } from 'lucide-react';
import { holdingsService, portfolioService } from '../services/api';
import './HoldingsPage.css';

// ── Types ─────────────────────────────────────────────────────────────────────
interface Asset {
  id: string;
  ticker: string;
  name: string;
  sector: string;
  assetType: string;
  currentPrice: number;
}

interface Holding {
  id: string;
  portfolioId: string;
  assetId: string;
  asset: Asset;
  quantity: number;
  averagePrice: number;
  currentValue: number;
  totalInvestment: number;
  unrealizedGain: number;
}

interface Portfolio {
  id: string;
  portfolioName: string;
  riskProfile: string;
}

// ── Helpers ───────────────────────────────────────────────────────────────────
const formatINR = (v: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(v);

const formatINRDecimal = (v: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(v);

// ── Component ─────────────────────────────────────────────────────────────────
export default function HoldingsPage() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Step 1 — load portfolios on mount, auto-select first
  useEffect(() => {
    portfolioService.getAll()
      .then(res => {
        const list: Portfolio[] = res.data.data ?? [];
        setPortfolios(list);
        if (list.length > 0) {
          setSelectedId(list[0].id);
        } else {
          setLoading(false);
        }
      })
      .catch(() => {
        setError('Unable to load portfolios.');
        setLoading(false);
      });
  }, []);

  // Step 2 — fetch holdings when portfolio selected
  useEffect(() => {
    if (!selectedId) return;
    fetchHoldings(selectedId);
  }, [selectedId]);

  const fetchHoldings = async (portfolioId: string) => {
    setLoading(true);
    setError(null);
    try {
      const res = await holdingsService.getByPortfolio(portfolioId);
      const data: Holding[] = res.data.data ?? [];
      setHoldings(data);
    } catch {
      setError('Unable to load holdings.');
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = () => {
    if (selectedId) fetchHoldings(selectedId);
  };

  // ── Derived totals ─────────────────────────────────────────────────────────
  const totalCurrentValue = holdings.reduce((s, h) => s + Number(h.currentValue), 0);
  const totalInvested = holdings.reduce((s, h) => s + Number(h.totalInvestment), 0);
  const totalUnrealizedGain = holdings.reduce((s, h) => s + Number(h.unrealizedGain), 0);
  const isPositive = totalUnrealizedGain >= 0;

  const selectedPortfolio = portfolios.find(p => p.id === selectedId);

  // ── Render states ──────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="holdings-page">
        <div className="holdings-loading">
          <RefreshCw className="spin" size={24} />
          <p>Loading holdings...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="holdings-page">
        <div className="holdings-error">{error}</div>
      </div>
    );
  }

  if (portfolios.length === 0) {
    return (
      <div className="holdings-page">
        <div className="holdings-empty">
          No portfolios found. Create a portfolio to get started.
        </div>
      </div>
    );
  }

  return (
    <div className="holdings-page">

      {/* Header */}
      <div className="holdings-header">
        <div>
          <h1 className="page-title">Holdings</h1>
          {selectedPortfolio && (
            <p className="holdings-subtitle">
              {selectedPortfolio.portfolioName}
              <span className="risk-badge">{selectedPortfolio.riskProfile}</span>
            </p>
          )}
        </div>
        <div className="holdings-header-actions">
          {/* Portfolio selector */}
          {portfolios.length > 1 && (
            <select
              className="portfolio-select-sm"
              value={selectedId ?? ''}
              onChange={e => setSelectedId(e.target.value)}
            >
              {portfolios.map(p => (
                <option key={p.id} value={p.id}>
                  {p.portfolioName}
                </option>
              ))}
            </select>
          )}
          <button
            className="btn btn-secondary"
            onClick={handleRefresh}
            disabled={loading}
          >
            <RefreshCw size={15} className={loading ? 'spin' : ''} />
            Refresh
          </button>
        </div>
      </div>

      {/* Summary Cards */}
      <div className="holdings-summary-grid">
        <div className="holdings-summary-card surface">
          <div className="summary-label">Current Value</div>
          <div className="summary-value">{formatINR(totalCurrentValue)}</div>
        </div>
        <div className="holdings-summary-card surface">
          <div className="summary-label">Total Invested</div>
          <div className="summary-value">{formatINR(totalInvested)}</div>
        </div>
        <div className="holdings-summary-card surface">
          <div className="summary-label">Unrealised Gain / Loss</div>
          <div className={`summary-value ${isPositive ? 'text-success' : 'text-danger'}`}>
            {isPositive
              ? <TrendingUp size={18} style={{ display: 'inline', marginRight: 4 }} />
              : <TrendingDown size={18} style={{ display: 'inline', marginRight: 4 }} />
            }
            {isPositive ? '+' : ''}{formatINR(totalUnrealizedGain)}
          </div>
        </div>
        <div className="holdings-summary-card surface">
          <div className="summary-label">Total Positions</div>
          <div className="summary-value">{holdings.length}</div>
        </div>
      </div>

      {/* Holdings Table */}
      {holdings.length === 0 ? (
        <div className="holdings-empty surface">
          No holdings found for this portfolio.
        </div>
      ) : (
        <div className="surface holdings-table-wrapper">
          <table className="holdings-table">
            <thead>
              <tr>
                <th>Ticker</th>
                <th>Name</th>
                <th>Sector</th>
                <th>Qty</th>
                <th>Avg Cost</th>
                <th>Current Price</th>
                <th>Current Value</th>
                <th>Invested</th>
                <th>Unrealised P&L</th>
              </tr>
            </thead>
            <tbody>
              {holdings.map(h => {
                const gain = Number(h.unrealizedGain);
                const gainPct = Number(h.totalInvestment) > 0
                  ? (gain / Number(h.totalInvestment)) * 100
                  : 0;
                const positive = gain >= 0;

                return (
                  <tr key={h.id}>
                    <td>
                      <span className="ticker-badge">{h.asset?.ticker ?? '—'}</span>
                    </td>
                    <td className="asset-name">{h.asset?.name ?? '—'}</td>
                    <td>
                      <span className="sector-badge">{h.asset?.sector ?? '—'}</span>
                    </td>
                    <td>{Number(h.quantity).toLocaleString('en-IN')}</td>
                    <td>{formatINRDecimal(Number(h.averagePrice))}</td>
                    <td>{formatINRDecimal(Number(h.asset?.currentPrice ?? 0))}</td>
                    <td className="fw-600">{formatINR(Number(h.currentValue))}</td>
                    <td>{formatINR(Number(h.totalInvestment))}</td>
                    <td>
                      <span className={positive ? 'text-success' : 'text-danger'}>
                        {positive ? '+' : ''}{formatINR(gain)}
                        <span className="gain-pct">
                          ({positive ? '+' : ''}{gainPct.toFixed(2)}%)
                        </span>
                      </span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}