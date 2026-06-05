import { useState } from 'react';
import { authService } from '../services/api';
import './Login.css';

interface LoginPageProps {
  onLogin: (token: string) => void;
}

export default function LoginPage({ onLogin }: LoginPageProps) {
  const [isRegister, setIsRegister] = useState(false);
  const [form, setForm] = useState({
    email: '',
    password: '',
    firstName: '',
    lastName: '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, [e.target.name]: e.target.value });
    setError('');
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError('');
    try {
      if (isRegister) {
        await authService.register(form);
        setIsRegister(false);
      } else {
        const res = await authService.login(form.email, form.password);
        // The backend returns snake_case fields.
        const { access_token, refresh_token } = res.data || {};
        if (access_token) {
          localStorage.setItem('accessToken', access_token);
          if (refresh_token) {
            localStorage.setItem('refreshToken', refresh_token);
          }
          onLogin(access_token);
        } else {
          throw new Error('Login failed: Unexpected response');
        }
      }
    } catch (err: unknown) {
      const message = (err as { response?: { data?: { error?: { message?: string } } } })
        ?.response?.data?.error?.message || 'Something went wrong.';
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* Left Panel - Branding */}
      <div className="login-left">
        <div className="login-brand">
          <div className="login-brand-icon">
            <svg width="28" height="28" viewBox="0 0 28 28" fill="none">
              <path d="M4 20L10 12L15 16L22 6" stroke="white" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" />
              <circle cx="22" cy="6" r="2" fill="white" />
            </svg>
          </div>
          <span>PortSight</span>
        </div>
        <div className="login-hero">
          <h1 className="login-hero-title">
            Smart Portfolio<br />Analytics Platform
          </h1>
          <p className="login-hero-sub">
            Real-time risk metrics, stress testing, and market simulation — designed for the modern investor.
          </p>
          <div className="login-features">
            {['Live Market Simulation', 'Risk & Stress Testing', 'AI-Powered Insights'].map((f) => (
              <div className="login-feature-item" key={f}>
                <div className="login-feature-dot" />
                {f}
              </div>
            ))}
          </div>
        </div>
        <div className="login-metrics-preview">
          <div className="preview-card">
            <div className="preview-label">Portfolio Value</div>
            <div className="preview-value">$1,245,000</div>
            <div className="preview-change">+24.5% all time</div>
          </div>
          <div className="preview-card">
            <div className="preview-label">Sharpe Ratio</div>
            <div className="preview-value">1.45</div>
            <div className="preview-change">Risk-Adjusted Return</div>
          </div>
        </div>
      </div>

      {/* Right Panel - Form */}
      <div className="login-right">
        <div className="login-card">
          <div className="login-card-header">
            <h2>{isRegister ? 'Create Account' : 'Welcome back'}</h2>
            <p>{isRegister ? 'Start your PortSight journey' : 'Sign in to your portfolio dashboard'}</p>
          </div>

          {error && <div className="login-error">{error}</div>}

          <form className="login-form" onSubmit={handleSubmit}>
            {isRegister && (
              <div className="form-row">
                <div className="form-group">
                  <label>First Name</label>
                  <input name="firstName" type="text" placeholder="John" value={form.firstName} onChange={handleChange} required />
                </div>
                <div className="form-group">
                  <label>Last Name</label>
                  <input name="lastName" type="text" placeholder="Doe" value={form.lastName} onChange={handleChange} required />
                </div>
              </div>
            )}
            <div className="form-group">
              <label>Email Address</label>
              <input name="email" type="email" placeholder="you@example.com" value={form.email} onChange={handleChange} required />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input name="password" type="password" placeholder="••••••••" value={form.password} onChange={handleChange} required />
            </div>
            <button type="submit" className="btn btn-primary login-submit" disabled={loading}>
              {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
            </button>
          </form>

          <div className="login-toggle">
            {isRegister ? 'Already have an account?' : "Don't have an account?"}
            <button className="toggle-btn" onClick={() => setIsRegister(!isRegister)}>
              {isRegister ? ' Sign in' : ' Register'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
