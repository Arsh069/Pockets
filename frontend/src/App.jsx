import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ToastProvider } from './context/ToastContext';
import { Navbar } from './components/Navbar';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { PocketDetailPage } from './pages/PocketDetailPage';
import { getAccessToken } from './api';

const ProtectedLayout = ({ children }) => {
  const token = getAccessToken();
  if (!token) {
    return <Navigate to="/login" replace />;
  }
  return (
    <div className="app-container">
      <Navbar />
      <main>{children}</main>
    </div>
  );
};

const PublicRoute = ({ children }) => {
  const token = getAccessToken();
  if (token) {
    return <Navigate to="/dashboard" replace />;
  }
  return children;
};

export default function App() {
  return (
    <ToastProvider>
      <BrowserRouter>
        <Routes>
          <Route
            path="/login"
            element={
              <PublicRoute>
                <AuthPage />
              </PublicRoute>
            }
          />
          <Route
            path="/register"
            element={
              <PublicRoute>
                <AuthPage />
              </PublicRoute>
            }
          />
          <Route
            path="/dashboard"
            element={
              <ProtectedLayout>
                <DashboardPage />
              </ProtectedLayout>
            }
          />
          <Route
            path="/pockets/:id"
            element={
              <ProtectedLayout>
                <PocketDetailPage />
              </ProtectedLayout>
            }
          />
          <Route path="*" element={<Navigate to="/dashboard" replace />} />
        </Routes>
      </BrowserRouter>
    </ToastProvider>
  );
}
