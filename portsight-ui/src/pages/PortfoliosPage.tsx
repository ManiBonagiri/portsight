import { useState, useEffect } from 'react';
import { Plus, ChevronRight, TrendingUp, Loader, ArrowLeft } from 'lucide-react';
import { portfolioService, holdingsService, transactionService } from '../services/api';
import './Portfolios.css';

interface Portfolio {
  id: string;
  portfolioName: string;
  riskProfile: string;
  benchmark: string;
  status: string;
  createdAt: string;
}

interface Asset {
  id: string;
  ticker: string;
  name: string;
  sector: string;
  currentPrice: number;
}

interface Holding {
  id: string;
  asset: Asset;
  quantity: number;
  averagePrice: number;
  currentValue: number;
  totalInvestment: number;
  unrealizedGain: number;
}

interface Transaction {
  id: string;
  asset: Asset;
  type: string;
  quantity: number;
  price: number;
  totalAmount: number;
  status: string;
  createdAt: string;
}

const RISK_COLOR: Record<string, string> = {
  CONSERVATIVE: '#00C853',
  MODERATE: '#FF9500',
  AGGRESSIVE: '#FF3B30',
  VERY_AGGRESSIVE: '#9B1FE8',
};

const RISK_LABEL: Record<string, string> = {
  CONSERVATIVE: 'Conservative',
  MODERATE: 'Moderate',
  AGGRESSIVE: 'Aggressive',
  VERY_AGGRESSIVE: 'Very Aggressive',
};

const fmt = (n: number) =>
  '₹' + Number(n).toLocaleString('en-IN', { maximumFractionDigits: 2 });

// ─── Portfolio Detail View ────────────────────────────────────────────────────
function PortfolioDetail({
  portfolio,
  onBack,
}: {
  portfolio: Portfolio;
  onBack: () => void;
}) {
  const [activeTab, setActiveTab] = useState<'holdings' | 'transactions'>('holdings');
  const [holdings, setHoldings] = useState<Holding[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [loadingH, setLoadingH] = useState(true);
  const [loadingT, setLoadingT] = useState(false);
  const color = RISK_COLOR[portfolio.riskProfile] ?? '#888';

  useEffect(() => {
    fetchHoldings();
  }, [portfolio.id]);

  useEffect(() => {
    if (activeTab === 'transactions' && transactions.length === 0) {
      fetchTransactions();
    }
  }, [activeTab]);

  const fetchHoldings = async () => {
    setLoadingH(true);
    try {
      const res = await holdingsService.getByPortfolio(portfolio.id);
      setHoldings(res.data.data || []);
    } catch { /* noop */ }
    finally { setLoadingH(false); }
  };

  const fetchTransactions = async () => {
    setLoadingT(true);
    try {
      const res = await transactionService.getByPortfolio(portfolio.id);
      setTransactions(res.data.data || []);
    } catch { /* noop */ }
    finally { setLoadingT(false); }
  };

  const totalValue = holdings.reduce((s, h) => s + Number(h.currentValue), 0);
  const totalInvested = holdings.reduce((s, h) => s + Number(h.totalInvestment), 0);
  const totalGain = holdings.reduce((s, h) => s + Number(h.unrealizedGain), 0);
  const returnPct = totalInvested > 0 ? ((totalGain / totalInvested) * 100).toFixed(2) : '0.00';

  return (
    <div className="portfolios-page">
      {/* Back + Header */}
      <div className="detail-header">
        <button className="btn-back" onClick={onBack}>
          <ArrowLeft size={18} /> Back to Portfolios
        </button>
      </div>

      {/* Portfolio Summary Card */}
      <div className="detail-summary surface surface-float">
        <div className="detail-summary-left">
          <div
            className="portfolio-avatar"
            style={{ background: `${color}20`, color, width: 56, height: 56, fontSize: '1.6rem' }}
          >
            {portfolio.portfolioName.charAt(0).toUpperCase()}
          </div>
          <div>
            <div className="detail-name">{portfolio.portfolioName}</div>
            <div className="portfolio-meta" style={{ marginTop: '0.4rem' }}>
              <span className="risk-chip" style={{ background: `${color}15`, color }}>
                {RISK_LABEL[portfolio.riskProfile] ?? portfolio.riskProfile}
              </span>
              <span className="bench-chip">vs {portfolio.benchmark}</span>
              <span className="portfolio-status-badge">
                {portfolio.status === 'ACTIVE' ? 'Active' : portfolio.status}
              </span>
            </div>
          </div>
        </div>

        <div className="detail-kpis">
          <div className="detail-kpi">
            <div className="kpi-label">Portfolio Value</div>
            <div className="kpi-value">{fmt(totalValue)}</div>
          </div>
          <div className="detail-kpi">
            <div className="kpi-label">Invested</div>
            <div className="kpi-value">{fmt(totalInvested)}</div>
          </div>
          <div className="detail-kpi">
            <div className="kpi-label">Unrealized Gain</div>
            <div className={`kpi-value ${totalGain >= 0 ? 'text-success' : 'text-danger'}`}>
              {totalGain >= 0 ? '+' : ''}{fmt(totalGain)}
            </div>
          </div>
          <div className="detail-kpi">
            <div className="kpi-label">Return</div>
            <div className={`kpi-value ${Number(returnPct) >= 0 ? 'text-success' : 'text-danger'}`}>
              {Number(returnPct) >= 0 ? '+' : ''}{returnPct}%
            </div>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="detail-tabs">
        <button
          className={`detail-tab ${activeTab === 'holdings' ? 'active' : ''}`}
          onClick={() => setActiveTab('holdings')}
        >
          Holdings ({holdings.length})
        </button>
        <button
          className={`detail-tab ${activeTab === 'transactions' ? 'active' : ''}`}
          onClick={() => setActiveTab('transactions')}
        >
          Transactions
        </button>
      </div>

      {/* Holdings Tab */}
      {activeTab === 'holdings' && (
        <div className="detail-table-wrap surface">
          {loadingH ? (
            <div className="portfolios-loading">
              <Loader size={20} className="spin" /> Loading holdings...
            </div>
          ) : holdings.length === 0 ? (
            <div className="portfolios-empty">No holdings in this portfolio.</div>
          ) : (
            <table className="detail-table">
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Sector</th>
                  <th>Qty</th>
                  <th>Avg Price</th>
                  <th>Current Value</th>
                  <th>Invested</th>
                  <th>Unrealized G/L</th>
                </tr>
              </thead>
              <tbody>
                {holdings.map((h) => {
                  const gain = Number(h.unrealizedGain);
                  return (
                    <tr key={h.id}>
                      <td>
                        <div className="asset-cell">
                          <span className="ticker-badge">{h.asset?.ticker}</span>
                          <span className="asset-name">{h.asset?.name}</span>
                        </div>
                      </td>
                      <td className="text-secondary">{h.asset?.sector}</td>
                      <td>{Number(h.quantity).toLocaleString('en-IN')}</td>
                      <td>{fmt(Number(h.averagePrice))}</td>
                      <td className="font-semibold">{fmt(Number(h.currentValue))}</td>
                      <td>{fmt(Number(h.totalInvestment))}</td>
                      <td className={gain >= 0 ? 'text-success' : 'text-danger'}>
                        {gain >= 0 ? '+' : ''}{fmt(gain)}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          )}
        </div>
      )}

      {/* Transactions Tab */}
      {activeTab === 'transactions' && (
        <div className="detail-table-wrap surface">
          {loadingT ? (
            <div className="portfolios-loading">
              <Loader size={20} className="spin" /> Loading transactions...
            </div>
          ) : transactions.length === 0 ? (
            <div className="portfolios-empty">No transactions found.</div>
          ) : (
            <table className="detail-table">
              <thead>
                <tr>
                  <th>Date</th>
                  <th>Asset</th>
                  <th>Type</th>
                  <th>Qty</th>
                  <th>Price</th>
                  <th>Total</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((t) => (
                  <tr key={t.id}>
                    <td className="text-secondary">
                      {new Date(t.createdAt).toLocaleDateString('en-IN')}
                    </td>
                    <td>
                      <div className="asset-cell">
                        <span className="ticker-badge">{t.asset?.ticker}</span>
                        <span className="asset-name">{t.asset?.name}</span>
                      </div>
                    </td>
                    <td>
                      <span className={`type-badge type-${t.type.toLowerCase()}`}>
                        {t.type}
                      </span>
                    </td>
                    <td>{Number(t.quantity).toLocaleString('en-IN')}</td>
                    <td>{fmt(Number(t.price))}</td>
                    <td className="font-semibold">{fmt(Number(t.totalAmount))}</td>
                    <td>
                      <span className="status-badge">{t.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      )}
    </div>
  );
}

// ─── Main PortfoliosPage ──────────────────────────────────────────────────────
interface Props {
  onViewDetails: (id: string) => void;
  selectedPortfolioId: string | null;
  onBack: () => void;
}

export default function PortfoliosPage({ onViewDetails, selectedPortfolioId, onBack }: Props) {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [showModal, setShowModal] = useState(false);
  const [form, setForm] = useState({
    portfolioName: '',
    riskProfile: 'MODERATE',
    benchmark: 'SIM_NIFTY',
  });
  const [loading, setLoading] = useState(false);
  const [fetching, setFetching] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchPortfolios = async () => {
    setFetching(true);
    setError(null);
    try {
      const res = await portfolioService.getAll();
      setPortfolios(res.data.data || []);
    } catch {
      setError('Unable to load portfolios. Please try again.');
    } finally {
      setFetching(false);
    }
  };

  useEffect(() => { fetchPortfolios(); }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    try {
      await portfolioService.create(form);
      setShowModal(false);
      setForm({ portfolioName: '', riskProfile: 'MODERATE', benchmark: 'SIM_NIFTY' });
      fetchPortfolios();
    } catch { /* noop */ }
    finally { setLoading(false); }
  };

  // Show detail view if a portfolio is selected
  if (selectedPortfolioId) {
    const portfolio = portfolios.find((p) => p.id === selectedPortfolioId);
    if (portfolio) {
      return <PortfolioDetail portfolio={portfolio} onBack={onBack} />;
    }
  }

  return (
    <div className="portfolios-page">
      <div className="portfolios-header">
        <div>
          <h1 className="page-title">My Portfolios</h1>
          <p className="page-sub">Manage and monitor your investment portfolios</p>
        </div>
        <button className="btn btn-primary" onClick={() => setShowModal(true)}>
          <Plus size={18} style={{ marginRight: '0.4rem' }} /> New Portfolio
        </button>
      </div>

      {fetching && (
        <div className="portfolios-loading">
          <Loader size={24} className="spin" />
          <span>Loading portfolios...</span>
        </div>
      )}

      {!fetching && error && (
        <div className="portfolios-error">
          <p>{error}</p>
          <button className="btn btn-outline" onClick={fetchPortfolios}>Retry</button>
        </div>
      )}

      {!fetching && !error && portfolios.length === 0 && (
        <div className="portfolios-empty">
          <p>No portfolios yet. Create your first portfolio to get started.</p>
          <button className="btn btn-primary" onClick={() => setShowModal(true)}>
            <Plus size={18} style={{ marginRight: '0.4rem' }} /> Create Portfolio
          </button>
        </div>
      )}

      {!fetching && !error && portfolios.length > 0 && (
        <div className="portfolios-grid">
          {portfolios.map((p) => {
            const color = RISK_COLOR[p.riskProfile] ?? '#888';
            return (
              <div key={p.id} className="portfolio-card surface surface-float">
                <div className="portfolio-card-top">
                  <div className="portfolio-avatar" style={{ background: `${color}20`, color }}>
                    {p.portfolioName.charAt(0).toUpperCase()}
                  </div>
                  <div className="portfolio-status-badge">
                    {p.status === 'ACTIVE' ? 'Active' : p.status}
                  </div>
                </div>
                <div className="portfolio-name">{p.portfolioName}</div>
                <div className="portfolio-meta">
                  <span className="risk-chip" style={{ background: `${color}15`, color }}>
                    {RISK_LABEL[p.riskProfile] ?? p.riskProfile}
                  </span>
                  <span className="bench-chip">vs {p.benchmark}</span>
                </div>
                <div className="portfolio-value">
                  <TrendingUp size={15} style={{ marginRight: '0.3rem', opacity: 0.6 }} />
                  Benchmark: {p.benchmark}
                </div>
                <div className="portfolio-created">
                  Created {new Date(p.createdAt).toLocaleDateString('en-IN')}
                </div>
                <button
                  className="portfolio-cta btn btn-outline"
                  onClick={() => onViewDetails(p.id)}
                >
                  View Details <ChevronRight size={16} />
                </button>
              </div>
            );
          })}

          <div className="portfolio-card portfolio-card--add" onClick={() => setShowModal(true)}>
            <div className="add-icon"><Plus size={28} /></div>
            <div className="add-text">Create New Portfolio</div>
            <div className="add-sub">Set risk profile, benchmark & strategy</div>
          </div>
        </div>
      )}

      {showModal && (
        <div className="modal-backdrop" onClick={() => setShowModal(false)}>
          <div className="modal" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2>Create Portfolio</h2>
              <button className="modal-close" onClick={() => setShowModal(false)}>✕</button>
            </div>
            <form onSubmit={handleCreate} className="modal-form">
              <div className="form-group">
                <label>Portfolio Name</label>
                <input
                  value={form.portfolioName}
                  onChange={(e) => setForm({ ...form, portfolioName: e.target.value })}
                  placeholder="e.g. Growth Portfolio"
                  required
                />
              </div>
              <div className="form-group">
                <label>Risk Profile</label>
                <select
                  value={form.riskProfile}
                  onChange={(e) => setForm({ ...form, riskProfile: e.target.value })}
                >
                  <option value="CONSERVATIVE">Conservative</option>
                  <option value="MODERATE">Moderate</option>
                  <option value="AGGRESSIVE">Aggressive</option>
                  <option value="VERY_AGGRESSIVE">Very Aggressive</option>
                </select>
              </div>
              <div className="form-group">
                <label>Benchmark</label>
                <select
                  value={form.benchmark}
                  onChange={(e) => setForm({ ...form, benchmark: e.target.value })}
                >
                  <option value="SIM_NIFTY">Simulated Nifty 50</option>
                  <option value="SIM_SENSEX">Simulated Sensex</option>
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn btn-outline" onClick={() => setShowModal(false)}>
                  Cancel
                </button>
                <button type="submit" className="btn btn-primary" disabled={loading}>
                  {loading ? 'Creating...' : 'Create Portfolio'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}