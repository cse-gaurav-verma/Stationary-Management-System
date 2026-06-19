import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import './FormPage.css';

const CreateRequest = () => {
  // Keeping track of our inventory list and the dynamic rows the user builds out.
  // We start the requestItems with one empty row so the form isn't blank on load.
  const [items, setItems] = useState([]);
  const [selectedItems, setSelectedItems] = useState([]);
  const [requestItems, setRequestItems] = useState([{ itemId: '', itemName: '', quantity: 1 }]);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  // Load up the available inventory when the component mounts. 
  // Grabbing a big chunk (size: 100) right away rather than dealing with pagination here.
  useEffect(() => {
    const loadItems = async () => {
      try {
        const response = await api.get('/api/inventory', { params: { page: 0, size: 100, sortBy: 'name' } });
        setItems(response.data.content || []);
      } catch (err) {
        setError('Failed to load inventory items.');
      }
    };

    loadItems();
  }, []);

  const addItem = () => {
    setRequestItems((prev) => [...prev, { itemId: '', itemName: '', quantity: 1 }]);
  };

  // We update individual fields within a specific row here.
  // If the user selects a new item, we look up its name from the inventory list 
  // so we can ship a clean payload to the backend later.
  const handleItemChange = (index, field, value) => {
    setRequestItems((prev) => {
      const next = [...prev];
      next[index] = { ...next[index], [field]: value };
      if (field === 'itemId') {
        const selected = items.find((item) => String(item.id) === String(value));
        next[index].itemName = selected?.name || '';
      }
      return next;
    });
  };

  const removeItem = (index) => {
    setRequestItems((prev) => prev.filter((_, idx) => idx !== index));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Quick sanity check to avoid submitting an empty request
    if (!requestItems.length) {
      setError('Add at least one item to create a request.');
      return;
    }

    // Clean up the payload. The API expects strict numbers for IDs and quantities.
    const payload = requestItems.map((row) => ({
      itemId: Number(row.itemId),
      itemName: row.itemName,
      quantity: Number(row.quantity),
    }));

    // Prevent bad data from hitting the backend. Every row needs a valid item and at least 1 qty.
    if (payload.some((row) => !row.itemId || row.quantity < 1)) {
      setError('Select valid items and quantities.');
      return;
    }

    setLoading(true);
    try {
      await api.post('/api/requests', { items: payload });
      setSuccess('Request created successfully.');
      // Give the user a brief moment to read the success message before we boot them back to their requests list
      setTimeout(() => navigate('/requests/my'), 1200);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to create request.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-card form-card">
      <div className="page-header">
        <div>
          <h1>Create Request</h1>
          <p className="page-subtitle">Submit a new stationery request from available inventory.</p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      {/* The form dynamically maps over requestItems to build our row fields */}
      <form className="request-grid" onSubmit={handleSubmit}>
        {requestItems.map((row, index) => (
          <div className="request-row" key={index}>
            <select
              value={row.itemId}
              onChange={(e) => handleItemChange(index, 'itemId', e.target.value)}
              disabled={loading}
            >
              <option value="">Select item</option>
              {items.map((item) => (
                <option key={item.id} value={item.id}>
                  {item.name} ({item.availableQuantity} available)
                </option>
              ))}
            </select>
            <input
              type="number"
              min="1"
              value={row.quantity}
              onChange={(e) => handleItemChange(index, 'quantity', e.target.value)}
              disabled={loading}
              placeholder="Qty"
            />
            <button type="button" className="btn btn-danger" disabled={loading} onClick={() => removeItem(index)}>
              Remove
            </button>
          </div>
        ))}

        <div className="form-actions full-width">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Submitting...' : 'Submit Request'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default CreateRequest;
