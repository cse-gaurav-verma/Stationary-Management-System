import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Inventory from './pages/Inventory';
import AddItem from './pages/AddItem';
import EditItem from './pages/EditItem';
import CreateRequest from './pages/CreateRequest';
import MyRequests from './pages/MyRequests';
import ManageRequests from './pages/ManageRequests';
import './App.css';

function App() {
  return (
    <AuthProvider>
      {/* We wrap the entire router in AuthProvider so user context and auth state are available anywhere in the component tree */}
      <Router>
        <div className="app">
          <Routes>
            {/* Unauthenticated routes - just login and registration */}
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />

            {/* 
              Protected routes use our ProtectedRoute wrapper to enforce auth checks. 
              We're injecting the Navbar here inside the protected layout so it only renders for authenticated users.
              Role-based access is handled by passing a requiredRole prop where needed (e.g., ADMIN vs STUDENT).
            */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Navbar />
                  <main className="main-content">
                    <Dashboard />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory"
              element={
                <ProtectedRoute>
                  <Navbar />
                  <main className="main-content">
                    <Inventory />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory/add"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <AddItem />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/inventory/edit/:id"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <EditItem />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/new"
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <Navbar />
                  <main className="main-content">
                    <CreateRequest />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/my"
              element={
                <ProtectedRoute requiredRole="STUDENT">
                  <Navbar />
                  <main className="main-content">
                    <MyRequests />
                  </main>
                </ProtectedRoute>
              }
            />
            <Route
              path="/requests/manage"
              element={
                <ProtectedRoute requiredRole="ADMIN">
                  <Navbar />
                  <main className="main-content">
                    <ManageRequests />
                  </main>
                </ProtectedRoute>
              }
            />

            {/* Catch-alls: bounce root and any unknown URLs to the dashboard where the user's role will dictate what they see next */}
            <Route path="/" element={<Navigate to="/dashboard" replace />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Routes>
        </div>
      </Router>
    </AuthProvider>
  );
}

export default App;
