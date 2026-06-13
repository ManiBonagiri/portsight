import { useState, useEffect, useCallback } from 'react';
import { Users, ScrollText, Activity, Search, RefreshCw, Shield, CheckCircle } from 'lucide-react';
import { adminService } from '../services/api';
import './AdminPage.css';

// ─── Types ────────────────────────────────────────────────────────────────────
interface AdminUser {
    id: string;
    email: string;
    firstName: string;
    lastName: string;
    role: string;
    status: string;
    createdAt: string;
}

interface AuditLog {
    id: string;
    userEmail: string;
    action: string;
    entityName: string;
    entityId: string;
    details: string;
    createdAt: string;
}

interface SystemHealth {
    totalUsers: number;
    totalAuditLogs: number;
    status: string;
}

type AdminTab = 'users' | 'audit' | 'health';

function getRoleBadgeClass(role: string): string {
    switch (role) {
        case 'ROLE_ADMIN': return 'badge badge--admin';
        case 'ROLE_ANALYST': return 'badge badge--analyst';
        default: return 'badge badge--user';
    }
}

function getRoleLabel(role: string): string {
    switch (role) {
        case 'ROLE_ADMIN': return 'Admin';
        case 'ROLE_ANALYST': return 'Analyst';
        default: return 'Investor';
    }
}

function formatDate(iso: string): string {
    if (!iso) return '—';
    return new Date(iso).toLocaleString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric',
        hour: '2-digit', minute: '2-digit',
    });
}

// ─── Component ────────────────────────────────────────────────────────────────
export default function AdminPage() {
    const [activeTab, setActiveTab] = useState<AdminTab>('users');

    // Users state
    const [users, setUsers] = useState<AdminUser[]>([]);
    const [usersLoading, setUsersLoading] = useState(false);
    const [usersError, setUsersError] = useState('');

    // Audit logs state
    const [logs, setLogs] = useState<AuditLog[]>([]);
    const [logsLoading, setLogsLoading] = useState(false);
    const [logsError, setLogsError] = useState('');
    const [logsPage, setLogsPage] = useState(0);
    const [logsTotalPages, setLogsTotalPages] = useState(0);
    const [logsSearch, setLogsSearch] = useState('');

    // Health state
    const [health, setHealth] = useState<SystemHealth | null>(null);
    const [healthLoading, setHealthLoading] = useState(false);
    const [healthError, setHealthError] = useState('');

    // ── Fetch users ─────────────────────────────────────────────────────────
    const fetchUsers = useCallback(async () => {
        setUsersLoading(true);
        setUsersError('');
        try {
            const res = await adminService.getUsers();
            setUsers(res.data);
        } catch {
            setUsersError('Unable to load users.');
        } finally {
            setUsersLoading(false);
        }
    }, []);

    // ── Fetch audit logs ─────────────────────────────────────────────────────
    const fetchLogs = useCallback(async (page: number, search: string) => {
        setLogsLoading(true);
        setLogsError('');
        try {
            const res = await adminService.getAuditLogs(page, 20, search);
            setLogs(res.data.content ?? []);
            setLogsTotalPages(res.data.totalPages ?? 0);
        } catch {
            setLogsError('Unable to load audit logs.');
        } finally {
            setLogsLoading(false);
        }
    }, []);

    // ── Fetch system health ──────────────────────────────────────────────────
    const fetchHealth = useCallback(async () => {
        setHealthLoading(true);
        setHealthError('');
        try {
            const res = await adminService.getSystemHealth();
            setHealth(res.data);
        } catch {
            setHealthError('Unable to load system health.');
        } finally {
            setHealthLoading(false);
        }
    }, []);

    // Load on tab switch
    useEffect(() => {
        if (activeTab === 'users') fetchUsers();
        if (activeTab === 'audit') fetchLogs(logsPage, logsSearch);
        if (activeTab === 'health') fetchHealth();
    }, [activeTab]);

    // Re-fetch logs when page changes
    useEffect(() => {
        if (activeTab === 'audit') fetchLogs(logsPage, logsSearch);
    }, [logsPage]);

    const handleLogsSearch = () => {
        setLogsPage(0);
        fetchLogs(0, logsSearch);
    };

    // ── Render ───────────────────────────────────────────────────────────────
    return (
        <div className="admin-page">

            {/* Header */}
            <div className="admin-header">
                <div className="admin-header-left">
                    <Shield size={20} className="admin-header-icon" />
                    <div>
                        <div className="admin-title">Administration Console</div>
                        <div className="admin-subtitle">Restricted to ROLE_ADMIN</div>
                    </div>
                </div>
            </div>

            {/* Tabs */}
            <div className="admin-tabs">
                <button
                    className={`admin-tab ${activeTab === 'users' ? 'admin-tab--active' : ''}`}
                    onClick={() => setActiveTab('users')}
                >
                    <Users size={15} /> User Management
                </button>
                <button
                    className={`admin-tab ${activeTab === 'audit' ? 'admin-tab--active' : ''}`}
                    onClick={() => setActiveTab('audit')}
                >
                    <ScrollText size={15} /> Audit Logs
                </button>
                <button
                    className={`admin-tab ${activeTab === 'health' ? 'admin-tab--active' : ''}`}
                    onClick={() => setActiveTab('health')}
                >
                    <Activity size={15} /> System Health
                </button>
            </div>

            {/* ── Users Tab ──────────────────────────────────────────────────────── */}
            {activeTab === 'users' && (
                <div className="admin-card">
                    <div className="admin-card-header">
                        <span>All Users ({users.length})</span>
                        <button className="admin-refresh-btn" onClick={fetchUsers} disabled={usersLoading}>
                            <RefreshCw size={14} className={usersLoading ? 'spin' : ''} />
                        </button>
                    </div>

                    {usersError && <div className="admin-error">{usersError}</div>}

                    {usersLoading ? (
                        <div className="admin-loading">Loading users...</div>
                    ) : (
                        <table className="admin-table">
                            <thead>
                                <tr>
                                    <th>Name</th>
                                    <th>Email</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                    <th>Joined</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.length === 0 ? (
                                    <tr><td colSpan={5} className="admin-empty">No users found.</td></tr>
                                ) : users.map(u => (
                                    <tr key={u.id}>
                                        <td className="admin-name">{u.firstName} {u.lastName}</td>
                                        <td className="admin-email">{u.email}</td>
                                        <td><span className={getRoleBadgeClass(u.role)}>{getRoleLabel(u.role)}</span></td>
                                        <td>
                                            <span className={`badge ${u.status === 'ACTIVE' ? 'badge--active' : 'badge--inactive'}`}>
                                                {u.status}
                                            </span>
                                        </td>
                                        <td className="admin-date">{formatDate(u.createdAt)}</td>
                                    </tr>
                                ))}
                            </tbody>
                        </table>
                    )}
                </div>
            )}

            {/* ── Audit Logs Tab ─────────────────────────────────────────────────── */}
            {activeTab === 'audit' && (
                <div className="admin-card">
                    <div className="admin-card-header">
                        <span>Audit Logs</span>
                        <div className="admin-search-row">
                            <input
                                className="admin-search"
                                placeholder="Search by email..."
                                value={logsSearch}
                                onChange={e => setLogsSearch(e.target.value)}
                                onKeyDown={e => e.key === 'Enter' && handleLogsSearch()}
                            />
                            <button className="admin-search-btn" onClick={handleLogsSearch}>
                                <Search size={14} />
                            </button>
                            <button className="admin-refresh-btn" onClick={() => fetchLogs(logsPage, logsSearch)} disabled={logsLoading}>
                                <RefreshCw size={14} className={logsLoading ? 'spin' : ''} />
                            </button>
                        </div>
                    </div>

                    {logsError && <div className="admin-error">{logsError}</div>}

                    {logsLoading ? (
                        <div className="admin-loading">Loading audit logs...</div>
                    ) : (
                        <>
                            <table className="admin-table">
                                <thead>
                                    <tr>
                                        <th>Time</th>
                                        <th>User</th>
                                        <th>Action</th>
                                        <th>Entity</th>
                                        <th>Details</th>
                                    </tr>
                                </thead>
                                <tbody>
                                    {logs.length === 0 ? (
                                        <tr>
                                            <td colSpan={5} className="admin-empty">
                                                No audit logs yet. Logs will appear as users perform actions.
                                            </td>
                                        </tr>
                                    ) : logs.map(log => (
                                        <tr key={log.id}>
                                            <td className="admin-date">{formatDate(log.createdAt)}</td>
                                            <td className="admin-email">{log.userEmail ?? '—'}</td>
                                            <td><span className="badge badge--action">{log.action}</span></td>
                                            <td className="admin-muted">{log.entityName ?? '—'}</td>
                                            <td className="admin-muted">{log.details ?? '—'}</td>
                                        </tr>
                                    ))}
                                </tbody>
                            </table>

                            {/* Pagination */}
                            {logsTotalPages > 1 && (
                                <div className="admin-pagination">
                                    <button
                                        className="admin-page-btn"
                                        disabled={logsPage === 0}
                                        onClick={() => setLogsPage(p => p - 1)}
                                    >← Prev</button>
                                    <span className="admin-page-info">Page {logsPage + 1} of {logsTotalPages}</span>
                                    <button
                                        className="admin-page-btn"
                                        disabled={logsPage >= logsTotalPages - 1}
                                        onClick={() => setLogsPage(p => p + 1)}
                                    >Next →</button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            )}

            {/* ── System Health Tab ──────────────────────────────────────────────── */}
            {activeTab === 'health' && (
                <div className="admin-card">
                    <div className="admin-card-header">
                        <span>System Health</span>
                        <button className="admin-refresh-btn" onClick={fetchHealth} disabled={healthLoading}>
                            <RefreshCw size={14} className={healthLoading ? 'spin' : ''} />
                        </button>
                    </div>

                    {healthError && <div className="admin-error">{healthError}</div>}

                    {healthLoading ? (
                        <div className="admin-loading">Loading system health...</div>
                    ) : health ? (
                        <div className="health-grid">

                            <div className="health-card">
                                <div className="health-card-label">Platform Status</div>
                                <div className="health-card-value health-card-value--up">
                                    <CheckCircle size={18} /> {health.status}
                                </div>
                            </div>

                            <div className="health-card">
                                <div className="health-card-label">Total Users</div>
                                <div className="health-card-value">{health.totalUsers}</div>
                            </div>

                            <div className="health-card">
                                <div className="health-card-label">Audit Log Entries</div>
                                <div className="health-card-value">{health.totalAuditLogs}</div>
                            </div>

                            <div className="health-card">
                                <div className="health-card-label">Actuator Health</div>
                                <div className="health-card-value">
                                    <a
                                        href="http://localhost:8080/actuator/health"
                                        target="_blank"
                                        rel="noreferrer"
                                        className="health-link"
                                    >
                                        Open /actuator/health ↗
                                    </a>
                                </div>
                            </div>

                        </div>
                    ) : (
                        <div className="admin-empty" style={{ padding: '2rem' }}>No health data available.</div>
                    )}
                </div>
            )}

        </div>
    );
}