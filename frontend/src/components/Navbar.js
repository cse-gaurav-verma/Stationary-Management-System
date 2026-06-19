import React, { useState } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import './Navbar.css';

const Navbar = () => {
  // Grab our auth context to figure out who's logged in and what permissions they have.
  // This centralizes our auth logic so we don't have to prop-drill the user state everywhere.
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  
  // Local state for the mobile hamburger menu toggle. Simple boolean flag.
  const [mobileOpen, setMobileOpen] = useState(false);

  // We clear the auth context and immediately boot the user back to the login screen.
  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const toggleMobile = () => {
    setMobileOpen(!mobileOpen);
  };

  // Useful for when a user clicks a link or clicks outside the menu on mobile.
  // We want to force the menu shut.
  const closeMobile = () => {
    setMobileOpen(false);
  };

  // Quick helper to check if a path matches the current route.
  // We use this to highlight the active link in the navigation bar.
  const isActive = (path) => location.pathname === path;

  // We're defining the routing structure for different roles here.
  // Keeping these separate makes it super easy to add new roles or modify routes later
  // without having to clutter the JSX with a ton of conditional rendering.
  const adminLinks = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/inventory', label: 'Inventory' },
    { path: '/inventory/add', label: 'Add Item' },
    { path: '/requests/manage', label: 'Manage Requests' },
    { path: '/logs', label: 'Logs' },
  ];

  const studentLinks = [
    { path: '/dashboard', label: 'Dashboard' },
    { path: '/inventory', label: 'Inventory' },
    { path: '/requests/new', label: 'New Request' },
    { path: '/requests/my', label: 'My Requests' },
  ];

  // Dynamically swap out the navigation array based on whether the user has admin privileges.
  const navLinks = isAdmin() ? adminLinks : studentLinks;

  return (
    <nav className="navbar">
      <div className="navbar-container">
        {/* Brand */}
        <Link to="/dashboard" className="navbar-brand" onClick={closeMobile}>
          <span className="brand-icon">SMS</span>
          <div className="brand-text">
            <span className="brand-name">SMS</span>
            <span className="brand-subtitle">Stationery Management</span>
          </div>
        </Link>

        {/* Desktop Navigation */}
        <div className={`navbar-links ${mobileOpen ? 'mobile-open' : ''}`}>
          {navLinks.map((link) => (
            <Link
              key={link.path}
              to={link.path}
              className={`nav-link ${isActive(link.path) ? 'active' : ''}`}
              onClick={closeMobile}
            >
                <span className="nav-link-label">{link.label}</span>
              {isActive(link.path) && <span className="active-indicator" />}
            </Link>
          ))}
        </div>

        {/* User Section */}
        <div className="navbar-user">
          <div className="user-info">
            <span className="user-avatar">
              {/* Optional chaining safely grabs the first letter of the username for the avatar. 
                  If user or username isn't loaded yet, it falls back to a generic 'U'. */}
              {user?.username?.charAt(0).toUpperCase() || 'U'}
            </span>
            <div className="user-details">
              <span className="user-name">{user?.username}</span>
              <span className={`role-badge ${user?.role?.toLowerCase()}`}>
                {user?.role}
              </span>
            </div>
          </div>
          <button className="logout-btn" onClick={handleLogout} title="Logout">
            <span className="logout-text">Logout</span>
          </button>
        </div>

        {/* Mobile Hamburger */}
        <button
          className={`hamburger ${mobileOpen ? 'open' : ''}`}
          onClick={toggleMobile}
          aria-label="Toggle navigation"
        >
          <span className="hamburger-line" />
          <span className="hamburger-line" />
          <span className="hamburger-line" />
        </button>
      </div>

      {/* Mobile Overlay */}
      {/* Acts as a backdrop on mobile. Clicking it dismisses the open menu for better UX. */}
      {mobileOpen && <div className="mobile-overlay" onClick={closeMobile} />}
    </nav>
  );
};

export default Navbar;
