import React, { createContext, useState, useContext, useEffect, useCallback } from 'react';

// We're setting up a Context here to hold our global authentication state.
// This saves us from prop-drilling user data and auth methods down through the component tree.
const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  // We use a loading state initially because while checking localStorage is synchronous,
  // it's a good pattern to have in case we ever switch to validating tokens via an API call on load.
  const [loading, setLoading] = useState(true);

  // On the first render, we try to hydrate our auth state from localStorage.
  // This ensures the user stays logged in even if they hit refresh or close the tab.
  useEffect(() => {
    try {
      const token = localStorage.getItem('sms_token');
      const username = localStorage.getItem('sms_user');
      const role = localStorage.getItem('sms_role');

      if (token && username && role) {
        setUser({ token, username, role });
      }
    } catch (err) {
      // If something gets corrupted in localStorage, we play it safe and clear everything out
      // rather than risk an invalid state throwing weird errors later on.
      console.error('Error restoring auth state:', err);
      localStorage.removeItem('sms_token');
      localStorage.removeItem('sms_user');
      localStorage.removeItem('sms_role');
    } finally {
      // Whether we successfully found a user or not, we're done checking, so we can drop the loading shield.
      setLoading(false);
    }
  }, []);

  // We wrap our auth methods in useCallback so their references remain stable.
  // If we didn't do this, every render of AuthProvider would create new function instances,
  // causing any components consuming this context to unnecessarily re-render.
  const login = useCallback((token, username, role) => {
    // Normalizing the role so we don't have to worry about 'ROLE_ADMIN' vs 'ADMIN' everywhere else in the app.
    const normalizedRole = role ? role.toUpperCase().replace('ROLE_', '') : 'STUDENT';
    localStorage.setItem('sms_token', token);
    localStorage.setItem('sms_user', username);
    localStorage.setItem('sms_role', normalizedRole);
    setUser({ token, username, role: normalizedRole });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('sms_token');
    localStorage.removeItem('sms_user');
    localStorage.removeItem('sms_role');
    setUser(null);
  }, []);

  // These helper methods make it cleaner to check permissions inside our components
  // without having to safely navigate the user object every single time.
  const isAdmin = useCallback(() => {
    return user?.role === 'ADMIN';
  }, [user]);

  const isStudent = useCallback(() => {
    return user?.role === 'STUDENT';
  }, [user]);

  const isAuthenticated = useCallback(() => {
    return !!user?.token;
  }, [user]);

  const value = {
    user,
    loading,
    login,
    logout,
    isAdmin,
    isStudent,
    isAuthenticated,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

// Creating a custom hook to consume the context.
// It acts as an abstraction layer so consuming components don't have to import both useContext and AuthContext.
// Plus, it lets us enforce that this hook is actually used inside the provider hierarchy.
export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
