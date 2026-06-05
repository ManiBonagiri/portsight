import { useState, useEffect } from 'react';
import { Search, RefreshCw, TrendingUp, TrendingDown, Zap } from 'lucide-react';
import { marketService } from '../services/api';
import './MarketPage.css';

type AssetType = 'ALL' | 'STOCK' | 'ETF' | 'BOND' | 'BENCHMARK';

const MOCK_ASSETS = [
  { ticker: 'TECH01', name: 'Tech Innovations Inc', sector: 'Technology', type: 'STOCK', price: 152.45, change: 2.45, changePct: 1.63 },
  { ticker: 'TECH02', name: 'Cloud Solutions LLC', sector: 'Technology', type: 'STOCK', price: 253.10, change: 3.10, changePct: 1.24 },
  { ticker: 'TECH03', name: 'AI Systems Corp', sector: 'Technology', type: 'STOCK', price: 94.20, change: -1.30, changePct: -1.36 },
  { ticker: 'BANK01', name: 'Global Finance Bank', sector: 'Financials', type: 'STOCK', price: 86.75, change: 1.75, changePct: 2.06 },
  { ticker: 'BANK02', name: 'National Trust', sector: 'Financials', type: 'STOCK', price: 44.90, change: -0.35, changePct: -0.77 },
  { ticker: 'PHARM01', name: 'HealthGen Pharma', sector: 'Healthcare', type: 'STOCK', price: 121.80, change: 1.05, changePct: 0.87 },
  { ticker: 'PHARM02', name: 'MedTech Devices', sector: 'Healthcare', type: 'STOCK', price: 63.50, change: -1.50, changePct: -2.30 },
  { ticker: 'SPY', name: 'SPDR S&P 500 ETF', sector: 'Index', type: 'ETF', price: 452.80, change: 2.80, changePct: 0.62 },
  { ticker: 'QQQ', name: 'Invesco QQQ Trust', sector: 'Index', type: 'ETF', price: 382.45, change: 2.45, changePct: 0.64 },
  { ticker: 'BND01', name: 'US Treasury 10Y', sector: 'Government', type: 'BOND', price: 100.15, change: 0.15, changePct: 0.15 },
  { ticker: 'BND02', name: 'Corp Bond High Yield', sector: 'Corporate', type: 'BOND', price: 102.80, change: 0.30, changePct: 0.29 },
  { ticker: 'SIM_NIFTY', name: 'Simulated Nifty 50', sector: 'Benchmark', type: 'BENCHMARK', price: 19742.50, change: 242.50, changePct: 1.24 },
  { ticker: 'SIM_SENSEX', name: 'Simulated Sensex', sector: 'Benchmark', type: 'BENCHMARK', price: 65820.00, change: 820.00, changePct: 1.26 },
];

const TYPE_COLORS: Record<string, string> = {
  STOCK: '#0055FF', ETF: '#6366F1', BOND: '#00C853', BENCHMARK: '#FF9500',
};

export default function MarketPage() {
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<AssetType>('ALL');
  const [advancing, setAdvancing] = useState(false);
  const [dayCount, setDayCount] = useState(0);
  const [lastUpdated, setLastUpdated] = useState(new Date().toLocaleTimeString());

  const handleAdvanceDay = async () => {
    setAdvancing(true);
    try {
      await marketService.advanceDay();
      setDayCount(d => d + 1);
      setLastUpdated(new Date().toLocaleTimeString());
    } catch { /* demo mode */ setDayCount(d => d + 1); }
    finally { setAdvancing(false); }
  };

  const filtered = MOCK_ASSETS.filter(a => {
    const matchFilter = filter === 'ALL' || a.type === filter;
    const matchSearch = a.ticker.toLowerCase().includes(search.toLowerCase()) ||
                        a.name.toLowerCase().includes(search.toLowerCase());
    return matchFilter && matchSearch;
  });

  const gainers = [...MOCK_ASSETS].sort((a, b) => b.changePct - a.changePct).slice(0, 3);
  const losers  = [...MOCK_ASSETS].sort((a, b) => a.changePct - b.changePct).slice(0, 3);

  return (
    <div className="market-page">
      {/* Top Banner */}
      <div className="market-header">
        <div>
          <h1 className="page-title">Market Overview</h1>
          <p className="page-sub">
            Simulated market · Day <strong>{dayCount}</strong> · Last updated: {lastUpdated}
          </p>
        </div>
        <button className={`btn btn-primary market-advance-btn ${advancing ? 'advancing' : ''}`} onClick={handleAdvanceDay} disabled={advancing}>
          {advancing ? <RefreshCw size={16} className="spin" /> : <Zap size={16} />}
          {advancing ? 'Simulating...' : 'Advance Day'}
        </button>
      </div>

      {/* Movers */}
      <div className="movers-grid">
        <div className="surface movers-card">
          <div className="movers-title text-success">🚀 Top Gainers</div>
          {gainers.map(a => (
            <div key={a.ticker} className="mover-row">
              <span className="ticker-badge">{a.ticker}</span>
              <span className="mover-name">{a.name}</span>
              <span className="text-success mover-pct">+{a.changePct.toFixed(2)}%</span>
            </div>
          ))}
        </div>
        <div className="surface movers-card">
          <div className="movers-title text-danger">📉 Top Losers</div>
          {losers.map(a => (
            <div key={a.ticker} className="mover-row">
              <span className="ticker-badge">{a.ticker}</span>
              <span className="mover-name">{a.name}</span>
              <span className="text-danger mover-pct">{a.changePct.toFixed(2)}%</span>
            </div>
          ))}
        </div>
        <div className="surface movers-card movers-card--stat">
          <div className="movers-title">📊 Market Summary</div>
          <div className="market-stat"><span>Total Assets</span><strong>{MOCK_ASSETS.length}</strong></div>
          <div className="market-stat"><span>Gainers</span><strong className="text-success">{MOCK_ASSETS.filter(a => a.changePct > 0).length}</strong></div>
          <div className="market-stat"><span>Losers</span><strong className="text-danger">{MOCK_ASSETS.filter(a => a.changePct < 0).length}</strong></div>
          <div className="market-stat"><span>Unchanged</span><strong>{MOCK_ASSETS.filter(a => a.changePct === 0).length}</strong></div>
        </div>
      </div>

      {/* Filters */}
      <div className="surface market-table-card">
        <div className="market-controls">
          <div className="market-search-wrap">
            <Search size={16} className="market-search-icon" />
            <input className="market-search" placeholder="Search ticker or name..." value={search} onChange={e => setSearch(e.target.value)} />
          </div>
          <div className="type-filters">
            {(['ALL', 'STOCK', 'ETF', 'BOND', 'BENCHMARK'] as AssetType[]).map(t => (
              <button key={t} className={`type-filter-btn ${filter === t ? 'active' : ''}`} onClick={() => setFilter(t)}>{t}</button>
            ))}
          </div>
        </div>

        <table className="market-table">
          <thead>
            <tr>
              <th>Ticker</th>
              <th>Name</th>
              <th>Sector</th>
              <th>Type</th>
              <th style={{ textAlign: 'right' }}>Price</th>
              <th style={{ textAlign: 'right' }}>Change</th>
              <th style={{ textAlign: 'right' }}>Change %</th>
            </tr>
          </thead>
          <tbody>
            {filtered.map(a => (
              <tr key={a.ticker}>
                <td><span className="ticker-badge">{a.ticker}</span></td>
                <td className="asset-name-cell">{a.name}</td>
                <td><span className="sector-chip">{a.sector}</span></td>
                <td>
                  <span className="asset-type-chip" style={{ color: TYPE_COLORS[a.type], background: `${TYPE_COLORS[a.type]}15` }}>
                    {a.type}
                  </span>
                </td>
                <td style={{ textAlign: 'right', fontWeight: 700 }}>
                  {a.price >= 1000 ? a.price.toLocaleString('en-US', { minimumFractionDigits: 2 }) : `$${a.price.toFixed(2)}`}
                </td>
                <td style={{ textAlign: 'right' }}>
                  <span className={a.change >= 0 ? 'text-success' : 'text-danger'}>
                    {a.change >= 0 ? '+' : ''}{a.change.toFixed(2)}
                  </span>
                </td>
                <td style={{ textAlign: 'right' }}>
                  <span className={`pct-badge ${a.changePct >= 0 ? 'pct-badge--up' : 'pct-badge--down'}`}>
                    {a.changePct >= 0 ? <TrendingUp size={12} /> : <TrendingDown size={12} />}
                    {a.changePct >= 0 ? '+' : ''}{a.changePct.toFixed(2)}%
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        {filtered.length === 0 && (
          <div className="empty-state">
            <div className="empty-icon">🔍</div>
            <div className="empty-text">No assets found for "{search}"</div>
          </div>
        )}
      </div>
    </div>
  );
}
