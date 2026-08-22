import React, { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { Plus, RotateCcw, ChevronLeft, ChevronRight, X } from 'lucide-react';
import { api } from '../api';
import { useToast } from '../context/ToastContext';

export const DashboardPage = () => {
  const navigate = useNavigate();
  const { showToast } = useToast();

  const [pockets, setPockets] = useState([]);
  const [loading, setLoading] = useState(true);

  // Pagination state (6 per page)
  const [currentPage, setCurrentPage] = useState(1);
  const pageSize = 6;

  // New Pocket Modal
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [newPocketName, setNewPocketName] = useState('');
  const [newPocketLimit, setNewPocketLimit] = useState('');
  const [creating, setCreating] = useState(false);

  // Reset All Confirmation Modal
  const [isResetConfirmOpen, setIsResetConfirmOpen] = useState(false);
  const [resetting, setResetting] = useState(false);

  const fetchPockets = async () => {
    setLoading(true);
    try {
      const data = await api.pockets.list();
      setPockets(data || []);
    } catch (err) {
      showToast(err.message || 'Failed to fetch pockets');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchPockets();
  }, []);

  const handleCreatePocket = async (e) => {
    e.preventDefault();
    if (!newPocketName.trim()) {
      showToast('Pocket name is required');
      return;
    }
    const limit = parseFloat(newPocketLimit);
    if (isNaN(limit) || limit <= 0) {
      showToast('Monthly limit must be greater than 0');
      return;
    }

    setCreating(true);
    try {
      await api.pockets.create(newPocketName.trim(), limit);
      showToast('Pocket created successfully!', 'success');
      setIsModalOpen(false);
      setNewPocketName('');
      setNewPocketLimit('');
      await fetchPockets();
    } catch (err) {
      showToast(err.message || 'Failed to create pocket');
    } finally {
      setCreating(false);
    }
  };

  const handleResetAll = async () => {
    setResetting(true);
    try {
      await api.pockets.resetAll();
      showToast('All pockets reset to monthly limits!', 'success');
      setIsResetConfirmOpen(false);
      await fetchPockets();
    } catch (err) {
      showToast(err.message || 'Failed to reset pockets');
    } finally {
      setResetting(false);
    }
  };

  // Helper currency formatter
  const formatCurrency = (val) => {
    const num = Number(val || 0);
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 2,
    }).format(num);
  };

  // Pagination calculation
  const totalPages = Math.ceil(pockets.length / pageSize) || 1;
  const paginatedPockets = pockets.slice((currentPage - 1) * pageSize, currentPage * pageSize);

  return (
    <div>
      {/* Dashboard Top Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
        <div>
          <h1 style={{ fontSize: '1.85rem', fontWeight: 800, letterSpacing: '-0.03em' }}>Your Pockets</h1>
          <p style={{ fontSize: '0.925rem', color: 'var(--color-text-secondary)', marginTop: '2px' }}>
            Monzo-style budget envelopes for your UPI spending
          </p>
        </div>

        <div style={{ display: 'flex', gap: '12px' }}>
          <button
            onClick={() => setIsResetConfirmOpen(true)}
            className="btn btn-secondary btn-sm"
            disabled={pockets.length === 0}
          >
            <RotateCcw size={16} />
            <span>Reset All</span>
          </button>
          <button
            onClick={() => setIsModalOpen(true)}
            className="btn btn-primary btn-sm"
          >
            <Plus size={16} />
            <span>New Pocket</span>
          </button>
        </div>
      </div>

      {/* Content Grid */}
      {loading ? (
        <div style={{ padding: '60px 0', textAlign: 'center', color: 'var(--color-text-secondary)' }}>
          <div style={{ fontSize: '1.1rem', fontWeight: 600 }}>Loading your pockets...</div>
        </div>
      ) : pockets.length === 0 ? (
        <div className="card" style={{ marginTop: '24px', textAlign: 'center', padding: '60px 20px' }}>
          <h3 style={{ fontSize: '1.25rem', fontWeight: 700, marginBottom: '8px' }}>No pockets yet</h3>
          <p style={{ color: 'var(--color-text-secondary)', marginBottom: '24px' }}>
            Create your first pocket envelope to start managing your budget.
          </p>
          <button onClick={() => setIsModalOpen(true)} className="btn btn-primary">
            <Plus size={18} />
            <span>Create Pocket</span>
          </button>
        </div>
      ) : (
        <>
          <div className="pockets-grid">
            {paginatedPockets.map((pocket) => (
              <Link
                key={pocket.id}
                to={`/pockets/${pocket.id}`}
                className="pocket-tile"
              >
                <div className="tile-name">{pocket.name}</div>
                <div className="tile-balance">
                  {formatCurrency(pocket.balance)}
                </div>
              </Link>
            ))}
          </div>

          {/* Client-side Pagination */}
          {totalPages > 1 && (
            <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: '16px', marginTop: '36px' }}>
              <button
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                disabled={currentPage === 1}
                className="btn btn-secondary btn-sm"
              >
                <ChevronLeft size={16} />
                <span>Prev</span>
              </button>
              <span style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--color-text-secondary)' }}>
                Page {currentPage} of {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage((p) => Math.min(totalPages, p + 1))}
                disabled={currentPage === totalPages}
                className="btn btn-secondary btn-sm"
              >
                <span>Next</span>
                <ChevronRight size={16} />
              </button>
            </div>
          )}
        </>
      )}

      {/* New Pocket Modal */}
      {isModalOpen && (
        <div className="modal-overlay" onClick={() => setIsModalOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Create New Pocket</h2>
              <button onClick={() => setIsModalOpen(false)} className="btn btn-ghost btn-sm">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={handleCreatePocket}>
              <div className="form-group">
                <label className="form-label">Pocket Name</label>
                <input
                  type="text"
                  className="form-input"
                  placeholder="e.g. Groceries, Dining, Fuel"
                  value={newPocketName}
                  onChange={(e) => setNewPocketName(e.target.value)}
                  autoFocus
                  required
                />
              </div>

              <div className="form-group">
                <label className="form-label">Monthly Limit (₹)</label>
                <input
                  type="number"
                  step="0.01"
                  min="0.01"
                  className="form-input"
                  placeholder="5000.00"
                  value={newPocketLimit}
                  onChange={(e) => setNewPocketLimit(e.target.value)}
                  required
                />
              </div>

              <div style={{ display: 'flex', gap: '12px', marginTop: '28px' }}>
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="btn btn-secondary btn-block"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="btn btn-primary btn-block"
                  disabled={creating}
                >
                  {creating ? 'Creating...' : 'Create Pocket'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Reset All Confirmation Modal */}
      {isResetConfirmOpen && (
        <div className="modal-overlay" onClick={() => setIsResetConfirmOpen(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h2 className="modal-title">Reset All Pockets?</h2>
              <button onClick={() => setIsResetConfirmOpen(false)} className="btn btn-ghost btn-sm">
                <X size={18} />
              </button>
            </div>
            <p style={{ color: 'var(--color-text-secondary)', fontSize: '0.95rem', lineHeight: '1.5', marginBottom: '24px' }}>
              Are you sure you want to reset all pockets to their configured monthly limits? This will override current balances and update last reset timestamps.
            </p>
            <div style={{ display: 'flex', gap: '12px' }}>
              <button
                type="button"
                onClick={() => setIsResetConfirmOpen(false)}
                className="btn btn-secondary btn-block"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleResetAll}
                className="btn btn-danger btn-block"
                disabled={resetting}
              >
                {resetting ? 'Resetting...' : 'Yes, Reset All'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
