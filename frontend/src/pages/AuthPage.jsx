import React, { useState } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { Wallet, ArrowRight, Lock, Phone, User as UserIcon, Calendar } from 'lucide-react';
import { api, setAuthSession } from '../api';
import { useToast } from '../context/ToastContext';

export const AuthPage = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { showToast } = useToast();

  const isRegisterInitial = location.pathname === '/register';
  const [isRegister, setIsRegister] = useState(isRegisterInitial);

  const [phoneNumber, setPhoneNumber] = useState('');
  const [password, setPassword] = useState('');
  const [name, setName] = useState('');
  const [payDay, setPayDay] = useState('1');
  const [loading, setLoading] = useState(false);

  const switchMode = (toRegister) => {
    setIsRegister(toRegister);
    setPhoneNumber('');
    setPassword('');
    setName('');
    setPayDay('1');
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);

    // Client-side phone validation
    const phoneRegex = /^[6-9]\d{9}$/;
    if (!phoneRegex.test(phoneNumber.trim())) {
      showToast('Phone number must be a valid 10-digit Indian mobile number starting with 6, 7, 8, or 9');
      setLoading(false);
      return;
    }

    try {
      let authResponse;
      if (isRegister) {
        const parsedPayDay = parseInt(payDay, 10);
        if (isNaN(parsedPayDay) || parsedPayDay < 1 || parsedPayDay > 31) {
          showToast('Pay day must be between 1 and 31');
          setLoading(false);
          return;
        }
        authResponse = await api.auth.register(phoneNumber.trim(), password, name.trim(), parsedPayDay);
        showToast('Account created successfully!', 'success');
      } else {
        authResponse = await api.auth.login(phoneNumber.trim(), password);
        showToast('Welcome back!', 'success');
      }

      setAuthSession(authResponse);
      navigate('/dashboard');
    } catch (err) {
      showToast(err.message || 'Authentication failed');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-wrapper">
      <div className="auth-card">
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', marginBottom: '28px' }}>
          <div className="brand-icon" style={{ width: '48px', height: '48px', marginBottom: '16px' }}>
            <Wallet size={26} />
          </div>
          <h1 style={{ fontSize: '1.65rem', fontWeight: 800, letterSpacing: '-0.03em', color: 'var(--color-text-primary)' }}>
            Pockets
          </h1>
          <p style={{ fontSize: '0.9rem', color: 'var(--color-text-secondary)', marginTop: '4px' }}>
            UPI-Native Envelope Budgeting
          </p>
        </div>

        <div className="auth-tabs">
          <button
            type="button"
            className={`auth-tab ${!isRegister ? 'active' : ''}`}
            onClick={() => switchMode(false)}
          >
            Log In
          </button>
          <button
            type="button"
            className={`auth-tab ${isRegister ? 'active' : ''}`}
            onClick={() => switchMode(true)}
          >
            Register
          </button>
        </div>

        <form onSubmit={handleSubmit} autoComplete="off">
          <div className="form-group">
            <label className="form-label">Phone Number (10 Digits)</label>
            <div style={{ position: 'relative' }}>
              <input
                type="tel"
                className="form-input"
                placeholder="9876543210"
                value={phoneNumber}
                onChange={(e) => setPhoneNumber(e.target.value)}
                maxLength={10}
                autoComplete="off"
                required
              />
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Password</label>
            <input
              type="password"
              className="form-input"
              placeholder="••••••••"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              autoComplete={isRegister ? 'new-password' : 'current-password'}
              required
            />
          </div>

          {isRegister && (
            <>
              <div className="form-group">
                <label className="form-label">Full Name (Optional)</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="Alex Mercer"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  autoComplete="off"
                />
              </div>

              <div className="form-group">
                <label className="form-label">Monthly Salary / Pay Day (1 - 31)</label>
                <input
                  type="number"
                  className="form-input"
                  min="1"
                  max="31"
                  value={payDay}
                  onChange={(e) => setPayDay(e.target.value)}
                  autoComplete="off"
                  required
                />
              </div>
            </>
          )}

          <button
            type="submit"
            className="btn btn-primary btn-block"
            style={{ marginTop: '24px', height: '48px' }}
            disabled={loading}
          >
            {loading ? 'Please wait...' : isRegister ? 'Create Account' : 'Sign In'}
            {!loading && <ArrowRight size={18} />}
          </button>
        </form>
      </div>
    </div>
  );
};
