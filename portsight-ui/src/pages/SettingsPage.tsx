import { useState } from 'react';
import { Sun, Moon, Check, X } from 'lucide-react';
import './SettingsPage.css';

export default function SettingsPage() {
  const [darkMode, setDarkMode] = useState(() => !!localStorage.getItem('themeDark'));


  const toggleTheme = () => {
    setDarkMode(!darkMode);
    const root = document.documentElement;
    if (darkMode) {
      localStorage.removeItem('themeDark');
      root.classList.remove('theme-dark');
    } else {
      localStorage.setItem('themeDark', 'true');
      root.classList.add('theme-dark');
    }

  };

  return (
    <div className="settings-page surface">
      <h1 className="page-title">Application Settings</h1>
      <section className="setting-item">
        <div className="setting-label">
          <Sun size={20} className="icon-light" />
          <Moon size={20} className="icon-dark" />
          <span>Theme</span>
        </div>
        <button className="btn btn-toggle" onClick={toggleTheme} title={darkMode ? 'Switch to Light Mode' : 'Switch to Dark Mode'}>
          {darkMode ? <Moon size={16} /> : <Sun size={16} />}
        </button>
      </section>
      <section className="setting-item">
        <div className="setting-label">
          <Check size={20} />
          <span>Enable Notifications</span>
        </div>
        <button className="btn btn-toggle disabled" title="Coming soon">Toggle</button>
      </section>
      <section className="setting-item">
        <div className="setting-label">
          <X size={20} />
          <span>Clear Local Data</span>
        </div>
        <button className="btn btn-danger" onClick={() => localStorage.clear()}>Clear</button>
      </section>
    </div>
  );
}
