import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import LoadingSpinner from './LoadingSpinner';

const ProtectedRoute = ({ children, requiredRole }) => {
  // Grab what we need from our auth context. We need to know if we're still checking auth status,
  // if the user is actually logged in, and their specific details (like role).
  const { user, loading, isAuthenticated } = useAuth();

  // If the auth state is still resolving (e.g., waiting on an API call), 
  // show a spinner so we don't accidentally kick a valid user out or flash unprotected content.
  if (loading) {
    return <LoadingSpinner />;
  }

  // If we've confirmed they aren't authenticated, bounce them back to the login page.
  // We use `replace` here so they don't get stuck in a redirect loop if they hit the back button.
  if (!isAuthenticated()) {
    return <Navigate to="/login" replace />;
  }

  // If this route is locked down to a specific role (like Admin) and the current user doesn't match,
  // we redirect them to a safe default page, like their dashboard. The optional chaining (`user?.role`)
  // guards against user being null just in case.
  if (requiredRole && user?.role !== requiredRole) {
    return <Navigate to="/dashboard" replace />;
  }

  // If they passed all the checks, go ahead and render the protected content they actually asked for!
  return children;
};

export default ProtectedRoute;
