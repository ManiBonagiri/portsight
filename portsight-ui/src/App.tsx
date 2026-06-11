import { useState, useEffect } from 'react';
import {
  LayoutDashboard, Briefcase, Activity, Settings,
  Bell, Search, ActivityIcon, LogOut, Cpu, ChevronDown,
  BarChart2, Copy, Layers, ShieldAlert, Scale, FileText
} from 'lucide-react';
import SettingsPage from './pages/SettingsPage';
import LoginPage from './pages/LoginPage';
import Dashboard from './pages/Dashboard';
import PortfoliosPage from './pages/PortfoliosPage';
import RiskAnalyticsPage from './pages/RiskAnalyticsPage';
import MarketPage from './pages/MarketPage';
import TransactionsPage from './pages/TransactionsPage';
import HoldingsPage from './pages/HoldingsPage';
import ReportsPage from './pages/ReportsPage';
import { marketService } from './services/api';
import './App.css';

// ─── JWT decode helper ────────────────────────────────────────────────────────
function decodeJwt(token: string): Record<string, any> | null {
  try {
    const payload = token.split('.')[1];
    return JSON.parse(atob(payload));
  } catch {
    return null;
  }
}

// ─── Role helpers ─────────────────────────────────────────────────────────────
type UserRole = 'ROLE_USER' | 'ROLE_ANALYST' | 'ROLE_ADMIN';

function getRoleLabel(role: UserRole): string {
  switch (role) {
    case 'ROLE_ADMIN': return 'Admin';
    case 'ROLE_ANALYST': return 'Portfolio Manager';
    case 'ROLE_USER': return 'Investor';
    default: return 'User';
  }
}

function getInitials(email: string): string {
  const parts = email.split('@')[0].split(/[._-]/);
  return parts.map(p => p[0]?.toUpperCase() ?? '').join('').slice(0, 2);
}

// ─── Nav config ───────────────────────────────────────────────────────────────
type Tab =
  | 'dashboard' | 'portfolios' | 'analytics' | 'market'
  | 'transactions' | 'holdings' | 'reports' | 'settings' | 'admin';

interface NavItem {
  id: Tab;
  label: string;
  icon: React.ReactNode;
  roles: UserRole[];
}

const NAV_ITEMS: NavItem[] = [
  {
    id: 'dashboard',
    label: 'Dashboard',
    icon: <LayoutDashboard size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'portfolios',
    label: 'Portfolios',
    icon: <Briefcase size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'analytics',
    label: 'Risk & Analytics',
    icon: <Activity size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'market',
    label: 'Market',
    icon: <BarChart2 size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'transactions',
    label: 'Transactions',
    icon: <Copy size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'holdings',
    label: 'Holdings',
    icon: <Layers size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'reports',
    label: 'Reports',
    icon: <FileText size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'settings',
    label: 'Settings',
    icon: <Settings size={20} />,
    roles: ['ROLE_USER', 'ROLE_ANALYST', 'ROLE_ADMIN'],
  },
  {
    id: 'admin',
    label: 'Administration',
    icon: <Scale size={20} />,
    roles: ['ROLE_ADMIN'],
  },
];

const TAB_TITLES: Record<Tab, string> = {
  dashboard: 'Dashboard',
  portfolios: 'Portfolios',
  analytics: 'Risk & Analytics',
  transactions: 'Transactions',
  holdings: 'Holdings',
  market: 'Market',
  reports: 'Reports',
  settings: 'Settings',
  admin: 'Administration',
};

// ─── App ──────────────────────────────────────────────────────────────────────
export default function App() {
  const [token, setToken] = useState(() => localStorage.getItem('accessToken'));
  const [activeTab, setActiveTab] = useState<Tab>('dashboard');
  const [selectedPortfolioId, setSelectedPortfolioId] = useState<string | null>(null);
  const [advancingDay, setAdvancingDay] = useState(false);
  const [dayCount, setDayCount] = useState(0);

  const decoded = token ? decodeJwt(token) : null;
  const email: string = decoded?.email ?? '';
  const authorities: string[] = decoded?.authorities ?? [];
  const role = (authorities[0] ?? 'ROLE_USER') as UserRole;

  useEffect(() => {
    const root = document.documentElement;
    if (localStorage.getItem('themeDark')) {
      root.classList.add('theme-dark');
    } else {
      root.classList.remove('theme-dark');
    }
  }, []);

  const handleLogin = (t: string) => {
    localStorage.setItem('accessToken', t);
    setToken(t);
  };

  const handleLogout = () => {
    localStorage.removeItem('accessToken');
    setToken(null);
  };

  const handleAdvanceDay = async () => {
    setAdvancingDay(true);
    try {
      await marketService.advanceDay();
      setDayCount(d => d + 1);
    } catch { /* noop */ } finally {
      setAdvancingDay(false);
    }
  };

  if (!token) return <LoginPage onLogin={handleLogin} />;

  const visibleNav = NAV_ITEMS.filter(item => item.roles.includes(role));

  return (
    <div className="app-container">
      {/* Sidebar */}
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-icon">
            <ActivityIcon size={20} />
          </div>
          PortSight
        </div>

        <nav className="nav-links">
          {visibleNav.map((item) => (
            <div
              key={item.id + item.label}
              className={`nav-item ${activeTab === item.id ? 'active' : ''}`}
              onClick={() => setActiveTab(item.id)}
            >
              {item.icon}
              {item.label}
            </div>
          ))}
        </nav>

        <div style={{ flex: 1 }} />

        {/* Market Simulator */}
        <div className="market-sim-panel">
          <div className="sim-title">
            <Cpu size={15} /> Market Simulator
          </div>
          <div className="sim-day">Day {dayCount}</div>
          <button
            className={`btn btn-primary sim-btn ${advancingDay ? 'sim-btn--loading' : ''}`}
            onClick={handleAdvanceDay}
            disabled={advancingDay}
          >
            {advancingDay ? 'Simulating...' : '▶ Advance Day'}
          </button>
        </div>

        {/* User footer */}
        <div className="sidebar-footer">
          <div className="user-info">
            <div className="avatar-sm">{getInitials(email)}</div>
            <div className="user-meta">
              <div className="user-name">{email.split('@')[0]}</div>
              <div className="user-role">{getRoleLabel(role)}</div>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout} title="Logout">
            <LogOut size={18} />
          </button>
        </div>
      </aside>

      {/* Main Content */}
      <main className="main-content">
        <header className="header">
          <div className="header-left">
            <div className="header-title">{TAB_TITLES[activeTab]}</div>
          </div>
          <div className="header-actions">
            <button className="icon-btn"><Search size={18} /></button>
            <button className="icon-btn"><Bell size={18} /></button>
            <div className="header-avatar">
              {getInitials(email)} <ChevronDown size={14} />
            </div>
          </div>
        </header>

        <div className="page-content">
          {activeTab === 'dashboard' && <Dashboard />}
          {activeTab === 'portfolios' && (
            <PortfoliosPage
              onViewDetails={(id) => setSelectedPortfolioId(id)}
              selectedPortfolioId={selectedPortfolioId}
              onBack={() => setSelectedPortfolioId(null)}
            />
          )}
          {activeTab === 'analytics' && <RiskAnalyticsPage />}
          {activeTab === 'market' && <MarketPage />}
          {activeTab === 'transactions' && <TransactionsPage />}
          {activeTab === 'holdings' && <HoldingsPage />}
          {activeTab === 'reports' && <ReportsPage />}
          {activeTab === 'settings' && <SettingsPage />}
          {activeTab === 'admin' && (
            <div style={{ padding: '2rem' }}>
              <h2>Administration Console</h2>
              <p style={{ color: '#888', marginTop: '0.5rem' }}>
                User management, system config, and audit logs — coming soon.
              </p>
            </div>
          )}
        </div>
      </main>
    </div>
  );
}