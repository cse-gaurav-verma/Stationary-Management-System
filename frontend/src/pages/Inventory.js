import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import api from '../api/axiosConfig';
import { useAuth } from '../context/AuthContext';
import './Inventory.css';

const Inventory = () => {
  const [items, setItems] = useState([]);
  const [search, setSearch] = useState('');
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [total, setTotal] = useState(0);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // We handle both standard pagination and search in this single function.
  // Note that the search endpoint currently returns a flat array instead of a 
  // standard page object, so we have to handle the response shape dynamically based on whether we're searching.
  const loadItems = async () => {
    setLoading(true);
    setError('');

    try {
      const url = search ? '/api/inventory/search' : '/api/inventory';
      const response = search
        ? await api.get(url, { params: { keyword: search } })
        : await api.get(url, { params: { page, size, sortBy: 'name' } });

      if (search) {
        setItems(response.data || []);
        setTotal(response.data?.length ?? 0);
      } else {
        // Regular inventory fetch gives us a paginated response, so the items are in `content`.
        setItems(response.data?.content || []);
        setTotal(response.data?.totalElements ?? 0);
      }
    } catch (err) {
      // Always fallback gracefully. It's better to show an empty state with an error message
      // than letting the whole component crash.
      setError('Failed to load inventory.');
      setItems([]);
      setTotal(0);
    } finally {
      setLoading(false);
    }
  };

  // We intentionally leave `search` out of the dependency array here.
  // We only want to trigger a fetch automatically when pagination changes.
  // For searches, we want to wait until they actually submit the form.
  useEffect(() => {
    loadItems();
  }, [page, size]);

  // Client-side role check so we can conditionally render the admin-only actions.
  // Remember that the backend is still validating permissions for any actual requests!
  const { isAdmin } = useAuth();

  const handleSearch = async (e) => {
    // Standard react stuff: prevent the default form submission so we don't reload the page.
    e.preventDefault();
    await loadItems();
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Inventory</h1>
          <p className="page-subtitle">Browse stationery items and search by name.</p>
        </div>
        <div className="page-actions">
          {isAdmin() && (
            <Link to="/inventory/add" className="btn btn-primary">
              Add New Item
            </Link>
          )}
        </div>
      </div>

      <form className="page-search" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search items by name"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          className="input-search"
        />
        <button type="submit" className="btn btn-secondary">
          Search
        </button>
      </form>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Name</th>
              <th>Category</th>
              <th>Quantity</th>
              <th>Min Qty</th>
              <th>Unit</th>
              <th>Description</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {items.length ? (
              items.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>{item.name}</td>
                  <td>{item.category}</td>
                  <td>{item.availableQuantity}</td>
                  <td>{item.minimumQuantity}</td>
                  <td>{item.unit}</td>
                  <td>{item.description || '—'}</td>
                  <td>
                    {isAdmin() && (
                      <Link to={`/inventory/edit/${item.id}`} className="action-link">
                        Edit
                      </Link>
                    )}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="8" className="empty-row">
                  {loading ? 'Loading items...' : 'No items found.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {/* 
        Hiding pagination when searching because our search API doesn't support it yet. 
        Once the backend adds pagination to search, we can drop this condition.
      */}
      {!search && (
        <div className="pagination-controls">
          <button
            className="pagination-btn"
            disabled={page === 0}
            onClick={() => setPage((prev) => Math.max(prev - 1, 0))}
          >
            Previous
          </button>
          <span>
            Page {page + 1} • {total} items
          </span>
          <button
            className="pagination-btn"
            disabled={(page + 1) * size >= total}
            onClick={() => setPage((prev) => prev + 1)}
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
};

export default Inventory;
