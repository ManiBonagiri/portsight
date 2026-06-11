import { useState, useEffect } from 'react';
import {
  AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, PieChart, Pie, Cell, Legend
} from 'recharts';
import {
  ArrowUpRight, ArrowDownRight, TrendingUp,
  DollarSign, Activity, Shield, ChevronDown
} from 'lucide-react';
import {
  analyticsService, riskService,
  portfolioService, transactionService, holdingsService
} from '../services/api';
import './Dashboard.css';

const COLORS = ['#0055FF', '#6366F1', '#00C853', '#FF9500', '#A855F7', '#FF3B30', '#00BCD4', '#FFC107'];

// ── Types ─────────────────────────────────────────────────────────────────────
interface Portfolio {
  id: string;
  portfolioName: string;
  riskProfile: string;
  benchmark: string;
  status: string;
}

interface Analytics {
  portfolioValue: number;
  investedAmount: number;
  gainLoss: number;
  returnPercentage: number;
}

interface Risk {
  volatility: number;
  beta: number;
  sharpeRatio: number;
  var95: number;
}

interface Transaction {
  id: string;
  asset?: { ticker: string };
  type: string;
  quantity: number;
  totalAmount: number;
  createdAt: string;
}

interface GrowthPoint {
  month: string;
  date: string;
  value: number;
}

interface AllocationSlice {
  name: string;
  value: number;
}

// ── Helpers ───────────────────────────────────────────────────────────────────
const formatINR = (v: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency',
    currency: 'INR',
    maximumFractionDigits: 0,
  }).format(v);

const formatDate = (iso: string) =>
  new Date(iso).toLocaleDateString('en-IN', {
    day: 'numeric', month: 'short', year: 'numeric',
  });

// ── Component ─────────────────────────────────────────────────────────────────
export default function Dashboard() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [analytics, setAnalytics] = useState<Analytics | null>(null);
  const [risk, setRisk] = useState<Risk | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [growthHistory, setGrowthHistory] = useState<GrowthPoint[]>([]);
  const [allocationData, setAllocationData] = useState<AllocationSlice[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Step 1 — load portfolios on mount, auto-select first
  useEffect(() => {
    portfolioService.getAll()
      .then(res => {
        const list: Portfolio[] = res.data.data ?? [];
        setPortfolios(list);
        if (list.length > 0) setSelectedId(list[0].id);
        else setLoading(false);
      })
      .catch(() => {
        setError('Unable to load portfolios.');
        setLoading(false);
      });
  }, []);

  // Step 2 — when portfolio selected, fetch all data
  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);
    setError(null);

    const fetchAll = async () => {

      // Analytics — CRITICAL
      try {
        const aRes = await analyticsService.getPortfolioAnalytics(selectedId);
        setAnalytics(aRes.data.data);
      } catch {
        setError('Unable to load portfolio analytics.');
        setLoading(false);
        return;
      }

      // Transactions — non-critical
      try {
        const tRes = await transactionService.getByPortfolio(selectedId);
        const allTx: Transaction[] = tRes.data.data ?? [];
        setTransactions(allTx.slice(0, 5));
      } catch {
        setTransactions([]);
      }

      // Risk — non-critical
      try {
        const rRes = await riskService.getRiskMetrics(selectedId);
        setRisk(rRes.data.data);
      } catch {
        setRisk(null);
      }

      // Growth history from portfolio_snapshots — non-critical
      try {
        const gRes = await analyticsService.getGrowthHistory(selectedId);
        const points: GrowthPoint[] = (gRes.data.data ?? []).map((p: any) => ({
          month: p.month,
          date: p.date,
          value: Number(p.value),
        }));
        setGrowthHistory(points);
      } catch {
        setGrowthHistory([]);
      }

      // Asset Allocation from real holdings — non-critical
      try {
        const hRes = await holdingsService.getByPortfolio(selectedId);
        const holdings: any[] = hRes.data.data ?? [];

        // Group by sector, sum currentValue per sector
        const sectorMap: Record<string, number> = {};
        let total = 0;
        for (const h of holdings) {
          const sector: string = h.asset?.sector ?? 'Other';
          const val = Number(h.currentValue ?? 0);
          sectorMap[sector] = (sectorMap[sector] ?? 0) + val;
          total += val;
        }

        // Convert to percentage slices, sorted descending
        if (total > 0) {
          const slices: AllocationSlice[] = Object.entries(sectorMap)
            .map(([name, val]) => ({
              name,
              value: Math.round((val / total) * 100 * 10) / 10, // 1 decimal
            }))
            .sort((a, b) => b.value - a.value);
          setAllocationData(slices);
        } else {
          setAllocationData([]);
        }
      } catch {
        setAllocationData([]);
      }

      setLoading(false);
    };

    fetchAll();
  }, [selectedId]);

  // ── Derived display values ─────────────────────────────────────────────────
  const totalValue = analytics?.portfolioValue ?? 0;
  const invested = analytics?.investedAmount ?? 0;
  const gainLoss = analytics?.gainLoss ?? 0;
  const returnPct = analytics?.returnPercentage ?? 0;
  const isPositive = gainLoss >= 0;

  const vol = risk?.volatility ?? 0;
  const beta = risk?.beta ?? 0;
  const sharpe = risk?.sharpeRatio ?? 0;
  const var95 = risk?.var95 ?? 0;

  const selectedPortfolio = portfolios.find(p => p.id === selectedId);

  // If snapshots exist use them; otherwise fall back to a single
  // "current value" point so the chart always renders something
  const chartData: GrowthPoint[] = growthHistory.length > 0
    ? growthHistory
    : (totalValue > 0
      ? [{ month: 'Now', date: new Date().toISOString().split('T')[0], value: Number(totalValue) }]
      : []);

  const chartSubtitle = growthHistory.length > 1
    ? `${growthHistory[0].month} – ${growthHistory[growthHistory.length - 1].month} (real data)`
    : growthHistory.length === 1
      ? 'Single snapshot — grows daily'
      : 'No snapshot data yet';

  // ── Render ─────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="dashboard-page">
        <div className="dashboard-loading">
          <div className="loading-spinner" />
          <p>Loading dashboard...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="dashboard-page">
        <div className="dashboard-error">{error}</div>
      </div>
    );
  }

  if (portfolios.length === 0) {
    return (
      <div className="dashboard-page">
        <div className="dashboard-empty">
          <p>No portfolios found. Create a portfolio to get started.</p>
        </div>
      </div>
    );
  }

  return (
    <div className="dashboard-page">

      {/* Portfolio Selector */}
      {portfolios.length > 1 && (
        <div className="portfolio-selector">
          <label className="selector-label">Viewing Portfolio</label>
          <div className="selector-wrapper">
            <select
              className="portfolio-select"
              value={selectedId ?? ''}
              onChange={e => setSelectedId(e.target.value)}
            >
              {portfolios.map(p => (
                <option key={p.id} value={p.id}>
                  {p.portfolioName} — {p.riskProfile}
                </option>
              ))}
            </select>
            <ChevronDown size={16} className="selector-icon" />
          </div>
        </div>
      )}

      {/* Single portfolio label */}
      {portfolios.length === 1 && selectedPortfolio && (
        <div className="portfolio-label">
          {selectedPortfolio.portfolioName}
          <span className="portfolio-badge">{selectedPortfolio.riskProfile}</span>
        </div>
      )}

      {/* KPI Row */}
      <div className="kpi-grid">
        <div className="kpi-card surface surface-float">
          <div className="kpi-icon kpi-icon--blue">
            <DollarSign size={22} />
          </div>
          <div className="kpi-body">
            <div className="kpi-label">Total Portfolio Value</div>
            <div className="kpi-value">{formatINR(Number(totalValue))}</div>
            <div className={`kpi-change ${isPositive ? 'kpi-change--up' : 'kpi-change--down'}`}>
              {isPositive ? <ArrowUpRight size={15} /> : <ArrowDownRight size={15} />}
              {isPositive ? '+' : ''}{formatINR(Number(gainLoss))} ({Number(returnPct).toFixed(2)}%)
            </div>
          </div>
        </div>

        <div className="kpi-card surface surface-float">
          <div className="kpi-icon kpi-icon--indigo">
            <TrendingUp size={22} />
          </div>
          <div className="kpi-body">
            <div className="kpi-label">Invested Capital</div>
            <div className="kpi-value">{formatINR(Number(invested))}</div>
            <div className="kpi-sub">Cost Basis</div>
          </div>
        </div>

        <div className="kpi-card surface surface-float">
          <div className="kpi-icon kpi-icon--green">
            <Activity size={22} />
          </div>
          <div className="kpi-body">
            <div className="kpi-label">Sharpe Ratio</div>
            <div className="kpi-value">{Number(sharpe).toFixed(2)}</div>
            <div className="kpi-sub">Risk-Adjusted Return</div>
          </div>
        </div>

        <div className="kpi-card surface surface-float">
          <div className="kpi-icon kpi-icon--orange">
            <Shield size={22} />
          </div>
          <div className="kpi-body">
            <div className="kpi-label">Beta (Market Sensitivity)</div>
            <div className="kpi-value">{Number(beta).toFixed(2)}</div>
            <div className={`kpi-sub ${Number(beta) > 1 ? 'text-danger' : ''}`}>
              {Number(beta) > 1 ? 'Higher Risk than Market' : 'Lower Risk than Market'}
            </div>
          </div>
        </div>
      </div>

      {/* Charts Row */}
      <div className="charts-grid">
        <div className="surface chart-card">
          <div className="chart-card-header">
            <div>
              <div className="chart-title">Portfolio Growth</div>
              <div className="chart-subtitle">{chartSubtitle}</div>
            </div>
            <div className={`chart-badge ${isPositive ? 'chart-badge--up' : 'chart-badge--down'}`}>
              {isPositive ? <ArrowUpRight size={14} /> : <ArrowDownRight size={14} />}
              {Number(returnPct).toFixed(2)}%
            </div>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <AreaChart data={chartData} margin={{ top: 5, right: 5, left: 0, bottom: 0 }}>
              <defs>
                <linearGradient id="valueGradient" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="5%" stopColor="#0055FF" stopOpacity={0.15} />
                  <stop offset="95%" stopColor="#0055FF" stopOpacity={0} />
                </linearGradient>
              </defs>
              <CartesianGrid strokeDasharray="3 3" stroke="#F0F2F5" vertical={false} />
              <XAxis
                dataKey="month"
                tick={{ fontSize: 12, fill: '#9CA3AF' }}
                axisLine={false}
                tickLine={false}
              />
              <YAxis
                tickFormatter={(v) => `₹${(v / 100000).toFixed(1)}L`}
                tick={{ fontSize: 12, fill: '#9CA3AF' }}
                axisLine={false}
                tickLine={false}
              />
              <Tooltip
                formatter={(v) => formatINR(Number(v))}
                labelFormatter={(label) => {
                  const point = chartData.find(p => p.month === label);
                  return point ? point.date : label;
                }}
                contentStyle={{
                  border: 'none',
                  borderRadius: '10px',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
                  fontSize: '13px'
                }}
              />
              <Area
                type="monotone"
                dataKey="value"
                stroke="#0055FF"
                strokeWidth={2.5}
                fill="url(#valueGradient)"
                dot={chartData.length <= 12}
                activeDot={{ r: 5, fill: '#0055FF' }}
              />
            </AreaChart>
          </ResponsiveContainer>
        </div>

        <div className="surface chart-card">
          <div className="chart-card-header">
            <div>
              <div className="chart-title">Asset Allocation</div>
              <div className="chart-subtitle">
                {allocationData.length > 0 ? 'By sector weight (real holdings)' : 'Loading allocation...'}
              </div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={260}>
            <PieChart>
              <Pie
                data={allocationData}
                cx="50%"
                cy="50%"
                innerRadius={68}
                outerRadius={100}
                paddingAngle={3}
                dataKey="value"
              >
                {allocationData.map((_, i) => (
                  <Cell key={i} fill={COLORS[i % COLORS.length]} />
                ))}
              </Pie>
              <Tooltip
                formatter={(v) => `${v}%`}
                contentStyle={{
                  border: 'none',
                  borderRadius: '10px',
                  boxShadow: '0 10px 25px rgba(0,0,0,0.1)',
                  fontSize: '13px'
                }}
              />
              <Legend
                iconType="circle"
                iconSize={10}
                formatter={(v) => <span style={{ fontSize: 12, color: '#4B5563' }}>{v}</span>}
              />
            </PieChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Risk & Transactions Row */}
      <div className="bottom-grid">
        <div className="surface risk-card">
          <div className="chart-title" style={{ marginBottom: '1.25rem' }}>Risk Metrics</div>
          <div className="risk-metrics-grid">
            <div className="risk-metric">
              <div className="risk-metric-label">Volatility (Annual)</div>
              <div className="risk-metric-value">{Number(vol).toFixed(2)}%</div>
              <div className="risk-progress-bar">
                <div className="risk-progress-fill" style={{ width: `${Math.min(Number(vol) / 30 * 100, 100)}%`, background: '#FF9500' }} />
              </div>
            </div>
            <div className="risk-metric">
              <div className="risk-metric-label">VaR 95 (Daily)</div>
              <div className="risk-metric-value text-danger">{Number(var95).toFixed(2)}%</div>
              <div className="risk-progress-bar">
                <div className="risk-progress-fill" style={{ width: `${Math.min(Math.abs(Number(var95)) / 20 * 100, 100)}%`, background: '#FF3B30' }} />
              </div>
            </div>
            <div className="risk-metric">
              <div className="risk-metric-label">Beta</div>
              <div className="risk-metric-value">{Number(beta).toFixed(2)}</div>
              <div className="risk-progress-bar">
                <div className="risk-progress-fill" style={{ width: `${Math.min(Number(beta) / 2 * 100, 100)}%`, background: '#6366F1' }} />
              </div>
            </div>
            <div className="risk-metric">
              <div className="risk-metric-label">Sharpe Ratio</div>
              <div className="risk-metric-value text-success">{Number(sharpe).toFixed(2)}</div>
              <div className="risk-progress-bar">
                <div className="risk-progress-fill" style={{ width: `${Math.min(Number(sharpe) / 3 * 100, 100)}%`, background: '#00C853' }} />
              </div>
            </div>
          </div>
        </div>

        <div className="surface tx-card">
          <div className="chart-title" style={{ marginBottom: '1.25rem' }}>Recent Transactions</div>
          {transactions.length === 0 ? (
            <div className="tx-empty">No transactions found for this portfolio.</div>
          ) : (
            <table className="tx-table">
              <thead>
                <tr>
                  <th>Asset</th>
                  <th>Type</th>
                  <th>Qty</th>
                  <th>Total</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                {transactions.map((tx) => (
                  <tr key={tx.id}>
                    <td><span className="ticker-badge">{tx.asset?.ticker ?? '—'}</span></td>
                    <td><span className={`type-badge type-badge--${tx.type.toLowerCase()}`}>{tx.type}</span></td>
                    <td>{Number(tx.quantity).toFixed(0)}</td>
                    <td>{formatINR(Number(tx.totalAmount))}</td>
                    <td style={{ color: 'var(--text-tertiary)' }}>{formatDate(tx.createdAt)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}