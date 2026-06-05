import { useState, useEffect } from 'react';
import { RefreshCw, Loader } from 'lucide-react';
import { portfolioService, transactionService } from '../services/api';
import './TransactionsPage.css';

// ─── Types ────────────────────────────────────────────────────────────────────
interface Portfolio {
  id: string;
  portfolioName: string;
  riskProfile: string;
}

interface Asset {
  ticker: string;
  name: string;
  sector: string;
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

// ─── Helpers ──────────────────────────────────────────────────────────────────
const formatINR = (val: number) =>
  '₹' + Number(val).toLocaleString('en-IN', { maximumFractionDigits: 2 });

const TYPE_COLORS: Record<string, string> = {
  BUY: '#00C853',
  SELL: '#FF3B30',
  DIVIDEND: '#0055FF',
  DEPOSIT: '#FF9500',
  WITHDRAWAL: '#9B1FE8',
};

const PAGE_SIZE = 10;

// ─── Component ────────────────────────────────────────────────────────────────
export default function TransactionsPage() {
  // Portfolio list
  const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
  const [selectedId, setSelectedId] = useState<string>('');

  // Transactions (full loaded set)
  const [allTx, setAllTx] = useState<Transaction[]>([]);
  const [fetching, setFetching] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Filters
  const [filterType, setFilterType] = useState<string>('ALL');
  const [filterFrom, setFilterFrom] = useState<string>('');
  const [filterTo, setFilterTo] = useState<string>('');

  // Pagination
  const [page, setPage] = useState(1);

  // ── Load portfolio list on mount ────────────────────────────────────────────
  useEffect(() => {
    (async () => {
      try {
        const res = await portfolioService.getAll();
        const list: Portfolio[] = res.data.data || [];
        setPortfolios(list);
        if (list.length > 0) setSelectedId(list[0].id);
      } catch {
        setError('Unable to load portfolios.');
      }
    })();
  }, []);

  // ── Fetch transactions when portfolio changes ───────────────────────────────
  useEffect(() => {
    if (!selectedId) return;
    fetchTransactions();
  }, [selectedId]);

  const fetchTransactions = async () => {
    setFetching(true);
    setError(null);
    setPage(1);
    try {
      const res = await transactionService.getByPortfolio(selectedId);
      setAllTx(res.data.data || []);
    } catch {
      setError('Unable to load transactions. Please try again.');
      setAllTx([]);
    } finally {
      setFetching(false);
    }
  };

  // ── Apply filters ───────────────────────────────────────────────────────────
  const filtered = allTx.filter(t => {
    if (filterType !== 'ALL' && t.type !== filterType) return false;
    if (filterFrom) {
      const txDate = new Date(t.createdAt).toISOString().slice(0, 10);
      if (txDate < filterFrom) return false;
    }
    if (filterTo) {
      const txDate = new Date(t.createdAt).toISOString().slice(0, 10);
      if (txDate > filterTo) return false;
    }
    return true;
  });

  // ── Pagination ──────────────────────────────────────────────────────────────
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE));
  const paginated = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE);

  const totalAmount = filtered.reduce(
    (s, t) => s + Number(t.totalAmount), 0
  );

  const resetFilters = () => {
    setFilterType('ALL');
    setFilterFrom('');
    setFilterTo('');
    setPage(1);
  };

  // ── Render ──────────────────────────────────────────────────────────────────
  return (
    <div className="transactions-page">

      {/* Header */}
      <div className="tx-header">
        <div>
          <h1 className="page-title">Transaction History</h1>
          <p className="page-sub">All buy, sell and dividend activity</p>
        </div>
        <button
          className="btn btn-outline tx-refresh"
          onClick={fetchTransactions}
          disabled={fetching || !selectedId}
        >
          {fetching
            ? <Loader size={15} className="spin" />
            : <RefreshCw size={15} />}
          Refresh
        </button>
      </div>

      {/* Portfolio Selector */}
      {portfolios.length > 1 && (
        <div className="tx-selector-row">
          <label className="tx-label">Portfolio</label>
          <select
            className="tx-select"
            value={selectedId}
            onChange={e => { setSelectedId(e.target.value); setPage(1); }}
          >
            {portfolios.map(p => (
              <option key={p.id} value={p.id}>
                {p.portfolioName} — {p.riskProfile}
              </option>
            ))}
          </select>
        </div>
      )}

      {/* Filters */}
      <div className="tx-filters surface">
        <div className="tx-filter-group">
          <label className="tx-label">Type</label>
          <select
            className="tx-select"
            value={filterType}
            onChange={e => { setFilterType(e.target.value); setPage(1); }}
          >
            <option value="ALL">All Types</option>
            <option value="BUY">BUY</option>
            <option value="SELL">SELL</option>
            <option value="DIVIDEND">DIVIDEND</option>
            <option value="DEPOSIT">DEPOSIT</option>
            <option value="WITHDRAWAL">WITHDRAWAL</option>
          </select>
        </div>

        <div className="tx-filter-group">
          <label className="tx-label">From</label>
          <input
            type="date"
            className="tx-date"
            value={filterFrom}
            onChange={e => { setFilterFrom(e.target.value); setPage(1); }}
          />
        </div>

        <div className="tx-filter-group">
          <label className="tx-label">To</label>
          <input
            type="date"
            className="tx-date"
            value={filterTo}
            onChange={e => { setFilterTo(e.target.value); setPage(1); }}
          />
        </div>

        <button className="btn btn-outline tx-reset" onClick={resetFilters}>
          Clear Filters
        </button>
      </div>

      {/* Summary bar */}
      {!fetching && !error && allTx.length > 0 && (
        <div className="tx-summary">
          <span>{filtered.length} transaction{filtered.length !== 1 ? 's' : ''}</span>
          <span className="tx-summary-amount">
            Total: <strong>{formatINR(totalAmount)}</strong>
          </span>
        </div>
      )}

      {/* Loading */}
      {fetching && (
        <div className="tx-state">
          <Loader size={22} className="spin" />
          <span>Loading transactions...</span>
        </div>
      )}

      {/* Error */}
      {!fetching && error && (
        <div className="tx-state tx-error">
          <p>{error}</p>
          <button className="btn btn-outline" onClick={fetchTransactions}>Retry</button>
        </div>
      )}

      {/* Empty */}
      {!fetching && !error && allTx.length === 0 && (
        <div className="tx-state">
          <p>No transactions found for this portfolio.</p>
        </div>
      )}

      {/* No results after filter */}
      {!fetching && !error && allTx.length > 0 && filtered.length === 0 && (
        <div className="tx-state">
          <p>No transactions match the current filters.</p>
          <button className="btn btn-outline" onClick={resetFilters}>Clear Filters</button>
        </div>
      )}

      {/* Table */}
      {!fetching && !error && paginated.length > 0 && (
        <div className="tx-table-wrap surface">
          <table className="tx-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Asset</th>
                <th>Sector</th>
                <th>Type</th>
                <th>Qty</th>
                <th>Price</th>
                <th>Total</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              {paginated.map(t => {
                const color = TYPE_COLORS[t.type] ?? '#888';
                return (
                  <tr key={t.id}>
                    <td className="tx-date-cell">
                      {new Date(t.createdAt).toLocaleDateString('en-IN')}
                    </td>
                    <td>
                      <div className="tx-asset-cell">
                        <span className="tx-ticker">{t.asset?.ticker}</span>
                        <span className="tx-name">{t.asset?.name}</span>
                      </div>
                    </td>
                    <td className="tx-secondary">{t.asset?.sector}</td>
                    <td>
                      <span
                        className="tx-type-badge"
                        style={{
                          background: `${color}18`,
                          color,
                        }}
                      >
                        {t.type}
                      </span>
                    </td>
                    <td>{Number(t.quantity).toLocaleString('en-IN')}</td>
                    <td>{formatINR(Number(t.price))}</td>
                    <td className="tx-total">{formatINR(Number(t.totalAmount))}</td>
                    <td>
                      <span className="tx-status-badge">{t.status}</span>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      )}

      {/* Pagination */}
      {!fetching && !error && filtered.length > PAGE_SIZE && (
        <div className="tx-pagination">
          <button
            className="btn btn-outline tx-pg-btn"
            onClick={() => setPage(p => Math.max(1, p - 1))}
            disabled={page === 1}
          >
            ← Prev
          </button>
          <span className="tx-pg-info">
            Page {page} of {totalPages} &nbsp;·&nbsp; {filtered.length} results
          </span>
          <button
            className="btn btn-outline tx-pg-btn"
            onClick={() => setPage(p => Math.min(totalPages, p + 1))}
            disabled={page === totalPages}
          >
            Next →
          </button>
        </div>
      )}

    </div>
  );
}