import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';
import './Login.css';

const Login = () => {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  // We check the auth state immediately to prevent logged-in users from seeing the login screen.
  // Using replace: true replaces the current entry in the history stack,
  // so the user can't hit the back button and accidentally end up back here.
  if (isAuthenticated()) {
    navigate('/dashboard', { replace: true });
    return null;
  }

  const handleSubmit = async (e) => {
    // Prevent the default form submission to avoid a full page reload.
    // This allows us to handle the login request asynchronously.
    e.preventDefault();
    setError('');

    // Basic client-side validation to save a round-trip to the server.
    if (!username.trim() || !password.trim()) {
      setError('Please fill in all fields');
      return;
    }

    setLoading(true);
    try {
      const response = await api.post('/api/auth/login', {
        username: username.trim(),
        password,
      });

      // Different backends might return the token under different keys.
      // We check a few common ones here to make this resilient to API contract changes.
      const data = response.data;
      const token = data.token || data.jwt || data.accessToken;
      const role = data.role || 'STUDENT';
      const user = data.username || username.trim();

      if (token) {
        // Save auth details to our context and redirect.
        // We use replace: true so the user can't navigate back to the login page.
        login(token, user, role);
        navigate('/dashboard', { replace: true });
      } else {
        setError('Invalid response from server');
      }
    } catch (err) {
      // Differentiating between server errors, network issues, and application errors
      // allows us to provide much more helpful feedback to the user.
      if (err.response) {
        const msg = err.response.data?.message || err.response.data?.error;
        setError(msg || `Login failed (${err.response.status})`);
      } else if (err.request) {
        setError('Unable to reach server. Please check your connection.');
      } else {
        setError('An unexpected error occurred');
      }
    } finally {
      // Always clear the loading state, whether the request succeeded or threw an error.
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      {/* 
        These purely visual background elements are grouped together.
        We keep them separate from the main card for cleaner styling and layering.
      */}
      <div className="login-bg-decor">
        <div className="bg-orb bg-orb-1" />
        <div className="bg-orb bg-orb-2" />
        <div className="bg-orb bg-orb-3" />
      </div>

      <div className="login-card">
        <div className="login-header">
          <div className="login-logo">SMS</div>
          <h1 className="login-title">Welcome Back</h1>
          <p className="login-subtitle">Sign in to your SMS account</p>
        </div>

        {error && (
          <div className="login-error">
            <span>{error}</span>
          </div>
        )}

        <form className="login-form" onSubmit={handleSubmit}>
          <div className="form-group">
            <label className="form-label" htmlFor="username">Username</label>
            <input
              id="username"
              type="text"
              className="form-input"
              placeholder="Enter your username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={loading}
              autoComplete="username"
              autoFocus
            />
          </div>

          <div className="form-group">
            <label className="form-label" htmlFor="password">Password</label>
            <div className="password-wrapper">
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                className="form-input"
                placeholder="Enter your password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                disabled={loading}
                autoComplete="current-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                tabIndex={-1}
              >
                {showPassword ? 'Hide' : 'Show'}
              </button>
            </div>
          </div>

          <button
            type="submit"
            className={`login-btn ${loading ? 'loading' : ''}`}
            disabled={loading}
          >
            {loading ? (
              <span className="btn-spinner" />
            ) : (
              <>
                <span>Sign In</span>
                <span className="btn-arrow">→</span>
              </>
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>
            Don't have an account?{' '}
            <Link to="/register" className="register-link">
              Create one
            </Link>
          </p>
        </div>
      </div>
    </div>
  );
};

export default Login;
