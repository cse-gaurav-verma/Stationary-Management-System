import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../api/axiosConfig';
import './Dashboard.css';

const Dashboard = () => {
  // Grab the user from context to check their role. This dictates what stats and actions they see.
  const { user } = useAuth();
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // We keep all our dashboard metrics in a single object state. 
  // It's cleaner to manage and update them together, even if some fields (like lowStock) are admin-only.
  const [stats, setStats] = useState({
    totalItems: 0,
    lowStock: 0,
    totalRequests: 0,
    pendingRequests: 0,
    myRequests: 0,
  });

  useEffect(() => {
    const loadStats = async () => {
      setLoading(true);
      setError('');

      try {
        // First up, let's grab the total inventory items. 
        // We do a lightweight fetch here—just grabbing 1 item (size: 1)—because we only care about the totalElements from the pagination metadata.
        const inventoryResponse = await api.get('/api/inventory', {
          params: { page: 0, size: 1, sortBy: 'name' },
        });

        const totalItems = inventoryResponse.data?.totalElements ?? 0;
        let lowStock = 0;
        let totalRequests = 0;
        let pendingRequests = 0;
        let myRequests = 0;

        // The remaining stats split based on role. 
        // Admins get the global view (low stock alerts, all requests), while regular users just see their own slices of data.
        if (user?.role === 'ADMIN') {
          const lowResponse = await api.get('/api/inventory/low-stock');
          lowStock = Number(lowResponse.data?.length ?? 0);

          const allRequests = await api.get('/api/requests');
          totalRequests = Number(allRequests.data?.length ?? 0);
          
          // Manually filtering for 'PENDING' since the API returns everything in one go.
          pendingRequests = Number(
            allRequests.data?.filter((request) => request.status === 'PENDING')?.length ?? 0
          );
        } else {
          // Regular users (like students) only see their own request history.
          const myResponse = await api.get('/api/requests/my');
          myRequests = Number(myResponse.data?.length ?? 0);
          pendingRequests = Number(
            myResponse.data?.filter((request) => request.status === 'PENDING')?.length ?? 0
          );
        }

        // Commit all our derived stats at once to avoid multiple re-renders.
        setStats({ totalItems, lowStock, totalRequests, pendingRequests, myRequests });
      } catch (err) {
        setError('Unable to load dashboard stats. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadStats();
  // Trigger whenever the user's role changes, just in case their session context gets updated on the fly.
  }, [user?.role]);

  return (
    <div className="dashboard-root">
      <div className="dashboard-header page-card">
        <div className="dashboard-title">
          <h1>Dashboard</h1>
          <p className="page-subtitle">
            {user?.role === 'ADMIN'
              ? 'Admin overview of inventory and request activity.'
              : 'Student overview of inventory and your requests.'}
          </p>
        </div>

        <div className="dashboard-actions">
          {user?.role !== 'ADMIN' && (
            <Link className="btn btn-primary" to="/requests/new">
              New Request
            </Link>
          )}

          <Link className="btn btn-secondary" to="/inventory">
            View Inventory
          </Link>

          {user?.role === 'ADMIN' && (
            <>
              <Link className="btn btn-secondary" to="/requests/manage">
                Manage Requests
              </Link>
              <Link className="btn btn-primary" to="/inventory/add">
                Add Inventory
              </Link>
            </>
          )}
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="dashboard-grid">
        <aside className="dashboard-main page-card">
          <h2 className="small-title">Summary</h2>
          <div className="summary-block">
            <div className="summary-label">Inventory Items</div>
            <div className="summary-value">{stats.totalItems}</div>
          </div>

          <div className="summary-block">
            <div className="summary-label">Pending Requests</div>
            <div className="summary-value">{stats.pendingRequests}</div>
          </div>

          {user?.role === 'ADMIN' ? (
            <>
              <div className="summary-block">
                <div className="summary-label">Low Stock</div>
                <div className="summary-value">{stats.lowStock}</div>
              </div>
              <div className="summary-block">
                <div className="summary-label">Total Requests</div>
                <div className="summary-value">{stats.totalRequests}</div>
              </div>
            </>
          ) : (
            <div className="summary-block">
              <div className="summary-label">My Requests</div>
              <div className="summary-value">{stats.myRequests}</div>
            </div>
          )}
        </aside>

        <section className="dashboard-side page-card">
          <h2 className="small-title">Quick Actions & Info</h2>
          <div className="side-grid">
            <div className="side-card">
              <div className="side-title">Recent Activity</div>
              <div className="side-body">No recent activity to show.</div>
            </div>

            <div className="side-card">
              <div className="side-title">Notes</div>
              <div className="side-body">Use the actions above to manage inventory and requests.</div>
            </div>
          </div>
        </section>
      </div>

      {loading && <div className="page-loading">Loading dashboard...</div>}
    </div>
  );
};

export default Dashboard;
