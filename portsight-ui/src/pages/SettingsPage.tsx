import { useState, useEffect } from 'react';
import {
  Sun, Moon, User, Bell, Shield, Trash2,
  LogOut, Clock, ChevronRight
} from 'lucide-react';
import './SettingsPage.css';

// ─── JWT decode helper ────────────────────────────────────────────────────────
function decodeJwt(token: string): Record<string, any> | null {
  try {
    return JSON.parse(atob(token.split('.')[1]));
  } catch {
    return null;
  }
}

function getRoleLabel(role: string): string {
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

function formatExpiry(exp: number): string {
  const d = new Date(exp * 1000);
  return d.toLocaleString('en-IN', {
    day: '2-digit', month: 'short', year: 'numeric',
    hour: '2-digit', minute: '2-digit',
  });
}

// ─── Notification prefs keys ──────────────────────────────────────────────────
const NOTIF_KEYS = {
  riskAlerts: 'notif_riskAlerts',
  reportReady: 'notif_reportReady',
  marketUpdates: 'notif_marketUpdates',
} as const;

export default function SettingsPage() {
  // Theme
  const [darkMode, setDarkMode] = useState(() => !!localStorage.getItem('themeDark'));

  // Notification toggles — persisted in localStorage
  const [notifRisk, setNotifRisk] = useState(() => localStorage.getItem(NOTIF_KEYS.riskAlerts) !== 'false');
  const [notifReport, setNotifReport] = useState(() => localStorage.getItem(NOTIF_KEYS.reportReady) !== 'false');
  const [notifMarket, setNotifMarket] = useState(() => localStorage.getItem(NOTIF_KEYS.marketUpdates) !== 'false');

  // Profile from JWT
  const token = localStorage.getItem('accessToken');
  const decoded = token ? decodeJwt(token) : null;
  const email: string = decoded?.email ?? '—';
  const authorities: string[] = decoded?.authorities ?? [];
  const role = authorities[0] ?? 'ROLE_USER';
  const expiry: number | null = decoded?.exp ?? null;

  // Cleared confirmation
  const [cleared, setCleared] = useState(false);

  const toggleTheme = () => {
    const next = !darkMode;
    setDarkMode(next);
    const root = document.documentElement;
    if (next) {
      localStorage.setItem('themeDark', 'true');
      root.classList.add('theme-dark');
    } else {
      localStorage.removeItem('themeDark');
      root.classList.remove('theme-dark');
    }
  };

  const toggleNotif = (
    key: keyof typeof NOTIF_KEYS,
    value: boolean,
    setter: (v: boolean) => void
  ) => {
    const next = !value;
    setter(next);
    localStorage.setItem(NOTIF_KEYS[key], String(next));
  };

  const handleClearCache = () => {
    // Only clear non-auth cached data
    const keysToRemove: string[] = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i)!;
      if (k !== 'accessToken' && k !== 'refreshToken' && k !== 'themeDark'
        && !k.startsWith('notif_')) {
        keysToRemove.push(k);
      }
    }
    keysToRemove.forEach(k => localStorage.removeItem(k));
    setCleared(true);
    setTimeout(() => setCleared(false), 2500);
  };

  return (
    <div className="settings-page">

      {/* ── Profile Card ─────────────────────────────────────────── */}
      <section className="settings-card">
        <div className="settings-card-header">
          <User size={16} />
          <span>Profile</span>
        </div>
        <div className="profile-row">
          <div className="profile-avatar">{getInitials(email)}</div>
          <div className="profile-info">
            <div className="profile-name">{email.split('@')[0]}</div>
            <div className="profile-email">{email}</div>
            <span className={`role-badge role-badge--${role.toLowerCase().replace('role_', '')}`}>
              {getRoleLabel(role)}
            </span>
          </div>
        </div>
      </section>

      {/* ── Appearance ───────────────────────────────────────────── */}
      <section className="settings-card">
        <div className="settings-card-header">
          <Sun size={16} />
          <span>Appearance</span>
        </div>
        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Theme</div>
            <div className="setting-desc">{darkMode ? 'Dark mode active' : 'Light mode active'}</div>
          </div>
          <button
            className={`toggle-switch ${darkMode ? 'toggle-switch--on' : ''}`}
            onClick={toggleTheme}
            aria-label="Toggle theme"
          >
            <span className="toggle-thumb">
              {darkMode ? <Moon size={10} /> : <Sun size={10} />}
            </span>
          </button>
        </div>
      </section>

      {/* ── Notifications ────────────────────────────────────────── */}
      <section className="settings-card">
        <div className="settings-card-header">
          <Bell size={16} />
          <span>Notifications</span>
        </div>

        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Risk Alerts</div>
            <div className="setting-desc">Notify when VaR or concentration thresholds are breached</div>
          </div>
          <button
            className={`toggle-switch ${notifRisk ? 'toggle-switch--on' : ''}`}
            onClick={() => toggleNotif('riskAlerts', notifRisk, setNotifRisk)}
            aria-label="Toggle risk alerts"
          />
        </div>

        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Report Ready</div>
            <div className="setting-desc">Notify when a generated report is available to download</div>
          </div>
          <button
            className={`toggle-switch ${notifReport ? 'toggle-switch--on' : ''}`}
            onClick={() => toggleNotif('reportReady', notifReport, setNotifReport)}
            aria-label="Toggle report notifications"
          />
        </div>

        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Market Updates</div>
            <div className="setting-desc">Notify after each simulated market day advances</div>
          </div>
          <button
            className={`toggle-switch ${notifMarket ? 'toggle-switch--on' : ''}`}
            onClick={() => toggleNotif('marketUpdates', notifMarket, setNotifMarket)}
            aria-label="Toggle market update notifications"
          />
        </div>
      </section>

      {/* ── Session ──────────────────────────────────────────────── */}
      <section className="settings-card">
        <div className="settings-card-header">
          <Clock size={16} />
          <span>Session</span>
        </div>
        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Token Expiry</div>
            <div className="setting-desc">
              {expiry ? formatExpiry(expiry) : 'No active session'}
            </div>
          </div>
          <Shield size={16} className="settings-muted-icon" />
        </div>
      </section>

      {/* ── Data ─────────────────────────────────────────────────── */}
      <section className="settings-card">
        <div className="settings-card-header">
          <Trash2 size={16} />
          <span>Data</span>
        </div>
        <div className="setting-row">
          <div className="setting-info">
            <div className="setting-title">Clear Cached Data</div>
            <div className="setting-desc">
              Removes locally cached data. Your account and session are not affected.
            </div>
          </div>
          <button
            className={`btn-clear ${cleared ? 'btn-clear--done' : ''}`}
            onClick={handleClearCache}
            disabled={cleared}
          >
            {cleared ? '✓ Cleared' : 'Clear'}
          </button>
        </div>
      </section>

      {/* ── Version ──────────────────────────────────────────────── */}
      <div className="settings-version">
        PortSight Analytics &nbsp;·&nbsp; v1.0.0
      </div>

    </div>
  );
}