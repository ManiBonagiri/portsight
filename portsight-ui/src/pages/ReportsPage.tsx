import { useState, useEffect } from 'react';
import { FileText, Download, Trash2, Plus, ChevronDown, RefreshCw } from 'lucide-react';
import { reportService, portfolioService } from '../services/api';
import './ReportsPage.css';

// ── Types ─────────────────────────────────────────────────────────────────────
interface Portfolio {
    id: string;
    portfolioName: string;
    riskProfile: string;
}

interface Report {
    id: string;
    portfolioId: string;
    reportType: string;
    status: string;
    generatedAt: string | null;
    createdAt: string;
}

// ── Constants ─────────────────────────────────────────────────────────────────
const REPORT_TYPES = [
    { key: 'PORTFOLIO', label: 'Portfolio Report', icon: '📊', desc: 'Full portfolio overview with holdings, risk and performance' },
    { key: 'RISK', label: 'Risk Report', icon: '🛡️', desc: 'Volatility, Beta, Sharpe Ratio and VaR metrics' },
    { key: 'PERFORMANCE', label: 'Performance Report', icon: '📈', desc: 'Returns, CAGR and gain/loss breakdown' },
    { key: 'ALLOCATION', label: 'Allocation Report', icon: '🥧', desc: 'Sector-wise allocation weights and position sizes' },
];

const STATUS_STYLES: Record<string, string> = {
    COMPLETED: 'status--completed',
    GENERATING: 'status--generating',
    FAILED: 'status--failed',
};

// ── Helpers ───────────────────────────────────────────────────────────────────
const formatDate = (iso: string | null) => {
    if (!iso) return '—';
    return new Date(iso).toLocaleDateString('en-IN', {
        day: 'numeric', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
};

// ── Component ─────────────────────────────────────────────────────────────────
export default function ReportsPage() {
    const [portfolios, setPortfolios] = useState<Portfolio[]>([]);
    const [selectedId, setSelectedId] = useState<string | null>(null);
    const [reports, setReports] = useState<Report[]>([]);
    const [loading, setLoading] = useState(true);
    const [generating, setGenerating] = useState<string | null>(null); // reportType being generated
    const [downloading, setDownloading] = useState<string | null>(null); // reportId being downloaded
    const [deleting, setDeleting] = useState<string | null>(null); // reportId being deleted
    const [error, setError] = useState<string | null>(null);
    const [successMsg, setSuccessMsg] = useState<string | null>(null);

    // Load portfolios on mount
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

    // Load reports when portfolio selected
    useEffect(() => {
        if (!selectedId) return;
        loadReports(selectedId);
    }, [selectedId]);

    const loadReports = (portfolioId: string) => {
        setLoading(true);
        setError(null);
        reportService.list(portfolioId)
            .then(res => setReports(res.data.data ?? []))
            .catch(() => setError('Unable to load reports.'))
            .finally(() => setLoading(false));
    };

    const handleGenerate = async (reportType: string) => {
        if (!selectedId) return;
        setGenerating(reportType);
        setError(null);
        setSuccessMsg(null);
        try {
            await reportService.generate(selectedId, reportType);
            setSuccessMsg(`${reportType} report generated successfully.`);
            // Reload list to show new report
            loadReports(selectedId);
        } catch {
            setError(`Failed to generate ${reportType} report. Try again.`);
        } finally {
            setGenerating(null);
        }
    };

    const handleDownload = async (report: Report) => {
        setDownloading(report.id);
        setError(null);
        try {
            const res = await reportService.download(report.id);
            // Create blob URL and trigger download
            const blob = new Blob([res.data], { type: 'application/pdf' });
            const url = window.URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = `portsight-${report.reportType.toLowerCase()}-report.pdf`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
            window.URL.revokeObjectURL(url);
        } catch {
            setError('Failed to download report. Try again.');
        } finally {
            setDownloading(null);
        }
    };

    const handleDelete = async (reportId: string) => {
        setDeleting(reportId);
        setError(null);
        try {
            await reportService.delete(reportId);
            setReports(prev => prev.filter(r => r.id !== reportId));
        } catch {
            setError('Failed to delete report.');
        } finally {
            setDeleting(null);
        }
    };

    const selectedPortfolio = portfolios.find(p => p.id === selectedId);

    // ── Render ─────────────────────────────────────────────────────────────────
    if (portfolios.length === 0 && !loading) {
        return (
            <div className="reports-page">
                <div className="dashboard-empty">No portfolios found. Create a portfolio to generate reports.</div>
            </div>
        );
    }

    return (
        <div className="reports-page">

            {/* Header */}
            <div className="reports-header">
                <div>
                    <h1 className="page-title">Reports</h1>
                    <p className="page-sub">Generate and download PDF reports for your portfolios</p>
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

            {/* Feedback messages */}
            {error && (
                <div className="reports-alert reports-alert--error">{error}</div>
            )}
            {successMsg && (
                <div className="reports-alert reports-alert--success">{successMsg}</div>
            )}

            {/* Generate Section */}
            <div className="surface reports-section">
                <div className="reports-section-title">
                    <Plus size={18} />
                    Generate New Report
                    {selectedPortfolio && (
                        <span className="reports-portfolio-badge">{selectedPortfolio.portfolioName}</span>
                    )}
                </div>
                <div className="report-types-grid">
                    {REPORT_TYPES.map(rt => (
                        <button
                            key={rt.key}
                            className={`report-type-card ${generating === rt.key ? 'report-type-card--loading' : ''}`}
                            onClick={() => handleGenerate(rt.key)}
                            disabled={!!generating}
                        >
                            <div className="report-type-icon">{rt.icon}</div>
                            <div className="report-type-label">{rt.label}</div>
                            <div className="report-type-desc">{rt.desc}</div>
                            {generating === rt.key ? (
                                <div className="report-type-spinner">
                                    <RefreshCw size={14} className="spinning" />
                                    Generating...
                                </div>
                            ) : (
                                <div className="report-type-action">Generate PDF</div>
                            )}
                        </button>
                    ))}
                </div>
            </div>

            {/* Reports List */}
            <div className="surface reports-section">
                <div className="reports-section-title">
                    <FileText size={18} />
                    Generated Reports
                    <span className="reports-count">{reports.length}</span>
                    <button
                        className="reports-refresh-btn"
                        onClick={() => selectedId && loadReports(selectedId)}
                        title="Refresh"
                    >
                        <RefreshCw size={14} />
                    </button>
                </div>

                {loading ? (
                    <div className="dashboard-loading">
                        <div className="loading-spinner" />
                        <p>Loading reports...</p>
                    </div>
                ) : reports.length === 0 ? (
                    <div className="reports-empty">
                        <FileText size={40} className="reports-empty-icon" />
                        <p>No reports generated yet.</p>
                        <p className="reports-empty-sub">Use the buttons above to generate your first report.</p>
                    </div>
                ) : (
                    <table className="reports-table">
                        <thead>
                            <tr>
                                <th>Report Type</th>
                                <th>Status</th>
                                <th>Generated At</th>
                                <th>Created At</th>
                                <th>Actions</th>
                            </tr>
                        </thead>
                        <tbody>
                            {reports.map(report => (
                                <tr key={report.id}>
                                    <td>
                                        <div className="report-type-cell">
                                            <span className="report-type-dot">
                                                {REPORT_TYPES.find(r => r.key === report.reportType)?.icon ?? '📄'}
                                            </span>
                                            {REPORT_TYPES.find(r => r.key === report.reportType)?.label ?? report.reportType}
                                        </div>
                                    </td>
                                    <td>
                                        <span className={`report-status ${STATUS_STYLES[report.status] ?? ''}`}>
                                            {report.status}
                                        </span>
                                    </td>
                                    <td className="report-date">{formatDate(report.generatedAt)}</td>
                                    <td className="report-date">{formatDate(report.createdAt)}</td>
                                    <td>
                                        <div className="report-actions">
                                            {report.status === 'COMPLETED' && (
                                                <button
                                                    className="report-btn report-btn--download"
                                                    onClick={() => handleDownload(report)}
                                                    disabled={downloading === report.id}
                                                    title="Download PDF"
                                                >
                                                    {downloading === report.id
                                                        ? <RefreshCw size={14} className="spinning" />
                                                        : <Download size={14} />
                                                    }
                                                </button>
                                            )}
                                            <button
                                                className="report-btn report-btn--delete"
                                                onClick={() => handleDelete(report.id)}
                                                disabled={deleting === report.id}
                                                title="Delete"
                                            >
                                                {deleting === report.id
                                                    ? <RefreshCw size={14} className="spinning" />
                                                    : <Trash2 size={14} />
                                                }
                                            </button>
                                        </div>
                                    </td>
                                </tr>
                            ))}
                        </tbody>
                    </table>
                )}
            </div>
        </div>
    );
}