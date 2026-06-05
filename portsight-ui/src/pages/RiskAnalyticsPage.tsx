import { useState, useEffect } from 'react';
import {
  BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip,
  ResponsiveContainer, RadarChart, PolarGrid, PolarAngleAxis, Radar
} from 'recharts';
import { AlertTriangle, Shield, ArrowDownRight, ChevronDown, RefreshCw } from 'lucide-react';
import { riskService, portfolioService, stressTestService } from '../services/api';
import './RiskAnalytics.css';

// ── Static placeholders (replaced in P4 with real snapshot data) ─────────────
const RISK_HISTORY = [
  { month: 'Jan', volatility: 11.2, beta: 1.1 },
  { month: 'Feb', volatility: 13.5, beta: 1.3 },
  { month: 'Mar', volatility: 10.8, beta: 1.0 },
  { month: 'Apr', volatility: 14.2, beta: 1.4 },
  { month: 'May', volatility: 11.9, beta: 1.2 },
  { month: 'Jun', volatility: 12.5, beta: 1.24 },
];

const STRESS_SCENARIOS = [
  { key: 'MARKET_CRASH_20', label: 'Market Crash -20%', icon: '📉', severity: 'HIGH' },
  { key: 'MARKET_CRASH_30', label: 'Market Crash -30%', icon: '💥', severity: 'CRITICAL' },
  { key: 'TECH_CRASH_15', label: 'Tech Sector Crash -15%', icon: '💻', severity: 'MEDIUM' },
  { key: 'RATE_HIKE_2', label: 'Interest Rate Hike +2%', icon: '📈', severity: 'LOW' },
];

const SEVERITY_COLORS: Record<string, string> = {
  LOW: '#00C853', MEDIUM: '#FF9500', HIGH: '#FF3B30', CRITICAL: '#9B1FE8'
};

// ── Types ─────────────────────────────────────────────────────────────────────
interface Portfolio {
  id: string;
  portfolioName: string;
  riskProfile: string;
}

interface Risk {
  volatility: number;
  beta: number;
  sharpeRatio: number;
  var95: number;
}

interface StressResult {
  scenario: string;
  preStressValue: number;
  postStressValue: number;
  impactAmount: number;
  impactPercent: number;
}

// ── Helpers ───────────────────────────────────────────────────────────────────
const formatINR = (v: number) =>
  new Intl.NumberFormat('en-IN', {
    style: 'currency', currency: 'INR', maximumFractionDigits: 0
  }).format(v);

// Compute a 0–100 health score from real risk metrics
function computeHealthScore(risk: Risk): number {
  // Sharpe: higher = better (cap at 3.0 = 100)
  const sharpeScore = Math.min(Math.max(Number(risk.sharpeRatio) / 3.0, 0), 1) * 30;
  // Volatility: lower = better (0% = 100, 50%+ = 0)
  const volScore = Math.max(1 - Number(risk.volatility) / 50, 0) * 30;
  // Beta: closest to 1.0 = best, >2 or <0 = worst
  const betaScore = Math.max(1 - Math.abs(Number(risk.beta) - 1.0) / 2, 0) * 20;
  // VaR: less negative = better (-20% = 0, 0% = 100)
  const varScore = Math.max(1 - Math.abs(Number(risk.var95)) / 20, 0) * 20;
  return Math.round(sharpeScore + volScore + betaScore + varScore);
}

function healthLabel(score: number): string {
  if (score >= 75) return 'Good';
  if (score >= 50) return 'Moderate Risk';
  if (score >= 30) return 'High Risk';
  return 'Critical Risk';
}

function healthColor(score: number): string {
  if (score >= 75) return '#00C853';
  if (score >= 50) return '#FF9500';
  if (score >= 30) return '#FF3B30';
  return '#9B1FE8';
}

// Build radar data from real metrics
function buildRadarData(risk: Risk) {
  return [
    { metric: 'Sharpe Ratio', score: Math.round(Math.min(Number(risk.sharpeRatio) / 3 * 100, 100)) },
    { metric: 'Diversification', score: 55 }, // static until P4
    { metric: 'Liquidity', score: 88 }, // static until P4
    { metric: 'Drawdown', score: Math.round(Math.max(1 - Math.abs(Number(risk.var95)) / 20, 0) * 100) },
    { metric: 'Beta Control', score: Math.round(Math.max(1 - Math.abs(Number(risk.beta) - 1) / 2, 0) * 100) },
    { metric: 'Consistency', score: Math.round(Math.max(1 - Number(risk.volatility) / 50, 0) * 100) },
  ];
}

// ── Component ─────────────────────────────────────────────────────────────────
export default function RiskAnalyticsPage() {
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [risk, setRisk] = useState<Risk | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<StressResult | null>(null);
  const [running, setRunning] = useState('');

  // Step 1 — load portfolios on mount
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

  // Step 2 — fetch risk when portfolio selected
  useEffect(() => {
    if (!selectedId) return;
    setLoading(true);
    setError(null);
    setTestResult(null);

    riskService.getRiskMetrics(selectedId)
      .then(res => setRisk(res.data.data))
      .catch(() => setError('Unable to load risk metrics.'))
      .finally(() => setLoading(false));
  }, [selectedId]);

  // Stress test
  const runStressTest = async (scenario: string) => {
    if (!selectedId) return;
    setRunning(scenario);
    try {
      const res = await stressTestService.run(selectedId, scenario);
      setTestResult(res.data.data);
    } catch {
      setTestResult(null);
    } finally {
      setRunning('');
    }
  };

  const selectedPortfolio = portfolios.find(p => p.id === selectedId);
  const score = risk ? computeHealthScore(risk) : 0;
  const radarData = risk ? buildRadarData(risk) : [];
  const color = healthColor(score);

  // Circumference for SVG circle = 2π × 34 ≈ 213.6
  const arcLength = (score / 100) * 213.6;

  // ── Render ─────────────────────────────────────────────────────────────────
  if (loading) {
    return (
      <div className="risk-page">
        <div className="dashboard-loading">
          <div className="loading-spinner" />
          <p>Loading risk metrics...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="risk-page">
        <div className="dashboard-error">{error}</div>
      </div>
    );
  }

  if (portfolios.length === 0) {
    return (
      <div className="risk-page">
        <div className="dashboard-empty">No portfolios found. Create a portfolio to get started.</div>
      </div>
    );
  }

  return (
    <div className="risk-page">

      {/* Header + Portfolio Selector */}
      <div className="risk-page-header">
        <div>
          <h1 className="page-title">Risk & Analytics</h1>
          <p className="page-sub">Portfolio risk metrics, stress testing, and performance analytics</p>
        </div>
        {portfolios.length > 1 && (
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
        )}
      </div>

      {/* Risk Score Banner */}
      <div className="risk-banner surface">
        <div className="risk-banner-left">
          <div className="risk-score-circle">
            <svg viewBox="0 0 80 80" width="80" height="80">
              <circle cx="40" cy="40" r="34" fill="none" stroke="#F0F2F5" strokeWidth="8" />
              <circle cx="40" cy="40" r="34" fill="none" stroke={color} strokeWidth="8"
                strokeDasharray={`${arcLength} 213.6`}
                strokeLinecap="round" strokeDashoffset="53.4" />
            </svg>
            <span className="risk-score-label">{score}</span>
          </div>
          <div>
            <div className="risk-banner-title">
              Portfolio Health Score — {selectedPortfolio?.portfolioName}
            </div>
            <div className="risk-banner-sub" style={{ color }}>
              {healthLabel(score)} — {selectedPortfolio?.riskProfile}
            </div>
          </div>
        </div>
        <div className="risk-banner-metrics">
          <div className="banner-metric">
            <Shield size={18} style={{ color: '#00C853' }} />
            Sharpe: {Number(risk?.sharpeRatio ?? 0).toFixed(2)}
          </div>
          <div className="banner-metric">
            <AlertTriangle size={18} style={{ color: '#FF9500' }} />
            Beta: {Number(risk?.beta ?? 0).toFixed(2)}
          </div>
          <div className="banner-metric">
            <ArrowDownRight size={18} style={{ color: '#FF3B30' }} />
            VaR 95: {Number(risk?.var95 ?? 0).toFixed(2)}%
          </div>
        </div>
      </div>

      {/* Risk Metrics Detail Row */}
      <div className="kpi-grid">
        <div className="kpi-card surface surface-float">
          <div className="kpi-body">
            <div className="kpi-label">Volatility (Annual)</div>
            <div className="kpi-value">{Number(risk?.volatility ?? 0).toFixed(2)}%</div>
            <div className="kpi-sub">Historical price variability</div>
          </div>
        </div>
        <div className="kpi-card surface surface-float">
          <div className="kpi-body">
            <div className="kpi-label">Beta</div>
            <div className="kpi-value">{Number(risk?.beta ?? 0).toFixed(2)}</div>
            <div className={`kpi-sub ${Number(risk?.beta ?? 0) > 1 ? 'text-danger' : ''}`}>
              {Number(risk?.beta ?? 0) > 1 ? 'More volatile than market' : 'Less volatile than market'}
            </div>
          </div>
        </div>
        <div className="kpi-card surface surface-float">
          <div className="kpi-body">
            <div className="kpi-label">Sharpe Ratio</div>
            <div className="kpi-value">{Number(risk?.sharpeRatio ?? 0).toFixed(2)}</div>
            <div className="kpi-sub">Risk-adjusted return (RF: 6.5%)</div>
          </div>
        </div>
        <div className="kpi-card surface surface-float">
          <div className="kpi-body">
            <div className="kpi-label">VaR 95% (Daily)</div>
            <div className="kpi-value text-danger">{Number(risk?.var95 ?? 0).toFixed(2)}%</div>
            <div className="kpi-sub">Max expected daily loss</div>
          </div>
        </div>
      </div>

      {/* Charts */}
      <div className="risk-charts-grid">
        <div className="surface chart-card">
          <div className="chart-card-header">
            <div>
              <div className="chart-title">Volatility Trend</div>
              <div className="chart-subtitle">Monthly annualized volatility vs beta (simulated — P4)</div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <BarChart data={RISK_HISTORY} barGap={4}>
              <CartesianGrid strokeDasharray="3 3" stroke="#F0F2F5" vertical={false} />
              <XAxis dataKey="month" tick={{ fontSize: 12, fill: '#9CA3AF' }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fontSize: 12, fill: '#9CA3AF' }} axisLine={false} tickLine={false} />
              <Tooltip contentStyle={{ border: 'none', borderRadius: '10px', boxShadow: '0 10px 25px rgba(0,0,0,0.1)', fontSize: '13px' }} />
              <Bar dataKey="volatility" name="Volatility %" fill="#FF9500" radius={[4, 4, 0, 0]} />
              <Bar dataKey="beta" name="Beta" fill="#0055FF" radius={[4, 4, 0, 0]} />
            </BarChart>
          </ResponsiveContainer>
        </div>

        <div className="surface chart-card">
          <div className="chart-card-header">
            <div>
              <div className="chart-title">Risk Profile Radar</div>
              <div className="chart-subtitle">Derived from real risk metrics</div>
            </div>
          </div>
          <ResponsiveContainer width="100%" height={240}>
            <RadarChart data={radarData} cx="50%" cy="50%" outerRadius="80%">
              <PolarGrid stroke="#F0F2F5" />
              <PolarAngleAxis dataKey="metric" tick={{ fontSize: 10, fill: '#9CA3AF' }} />
              <Radar name="Score" dataKey="score" stroke="#0055FF" fill="#0055FF" fillOpacity={0.12} strokeWidth={2} />
            </RadarChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Stress Testing */}
      <div className="surface stress-section">
        <div className="chart-card-header">
          <div>
            <div className="chart-title">Stress Testing Engine</div>
            <div className="chart-subtitle">Simulate adverse market scenarios on your portfolio</div>
          </div>
          {testResult && (
            <div className="stress-result-badge">
              <ArrowDownRight size={16} />
              {Number(testResult.impactPercent).toFixed(1)}% impact
            </div>
          )}
        </div>

        <div className="stress-grid">
          {STRESS_SCENARIOS.map((sc) => (
            <button
              key={sc.key}
              className={`stress-scenario-card ${running === sc.key ? 'running' : ''}`}
              onClick={() => runStressTest(sc.key)}
              disabled={!!running}
            >
              <div className="stress-icon">{sc.icon}</div>
              <div className="stress-label">{sc.label}</div>
              <div className="stress-severity" style={{ color: SEVERITY_COLORS[sc.severity] }}>
                {sc.severity} SEVERITY
              </div>
              {running === sc.key
                ? <div className="stress-loading" />
                : <div className="stress-run-label">▶ Run Test</div>
              }
            </button>
          ))}
        </div>

        {testResult && (
          <div className="stress-result-card">
            <div className="stress-result-row">
              <span className="stress-result-label">Scenario</span>
              <span className="stress-result-val">
                {STRESS_SCENARIOS.find(s => s.key === testResult.scenario)?.label ?? testResult.scenario}
              </span>
            </div>
            <div className="stress-result-row">
              <span className="stress-result-label">Pre-Stress Portfolio Value</span>
              <span className="stress-result-val">{formatINR(Number(testResult.preStressValue))}</span>
            </div>
            <div className="stress-result-row">
              <span className="stress-result-label">Post-Stress Portfolio Value</span>
              <span className="stress-result-val text-danger">{formatINR(Number(testResult.postStressValue))}</span>
            </div>
            <div className="stress-result-row">
              <span className="stress-result-label">Impact Amount</span>
              <span className="stress-result-val text-danger">{formatINR(Number(testResult.impactAmount))}</span>
            </div>
            <div className="stress-result-row">
              <span className="stress-result-label">Impact Percentage</span>
              <span className="stress-result-val text-danger">{Number(testResult.impactPercent).toFixed(2)}%</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
