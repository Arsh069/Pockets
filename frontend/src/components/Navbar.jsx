import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Wallet, LogOut } from 'lucide-react';
import { getUser, api } from '../api';

export const Navbar = () => {
  const navigate = useNavigate();
  const user = getUser();

  const handleLogout = () => {
    api.auth.logout();
  };

  return (
    <header className="navbar">
      <Link to="/dashboard" className="brand">
        <div className="brand-icon">
          <Wallet size={22} />
        </div>
        <span className="brand-title">Pockets</span>
      </Link>

      {user && (
        <div className="nav-actions">
          <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-secondary)', marginRight: '8px' }}>
            {user.name || user.phoneNumber}
          </span>
          <button onClick={handleLogout} className="btn btn-ghost btn-sm" title="Log out">
            <LogOut size={16} />
            <span>Logout</span>
          </button>
        </div>
      )}
    </header>
  );
};
