import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api/axiosConfig';
import './FormPage.css';

const AddItem = () => {
  // Keeping all form fields in a single state object here. It cuts down on boilerplate
  // compared to having six different useStates, and makes the generic change handler super clean.
  const [form, setForm] = useState({
    name: '',
    category: '',
    unit: '',
    availableQuantity: '',
    minimumQuantity: '',
    description: '',
  });
  
  // We track UI feedback states separately. This lets us show alerts and disable the submit button
  // so users don't accidentally double-submit while the request is in flight.
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [categories, setCategories] = useState([]);
  const navigate = useNavigate();

  useEffect(() => {
    const loadCategories = async () => {
      try {
        const resp = await api.get('/api/inventory/categories');
        setCategories(resp.data || []);
      } catch (e) {
        // ignore - fall back to free text if categories not available
      }
    };
    loadCategories();
  }, []);

  // Standard controlled component pattern. We rely on the input's "name" attribute 
  // to dynamically update the right property in our form state object.
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault(); // Stop the browser from reloading the page
    setError('');
    setSuccess('');

    // Quick frontend validation to ensure required fields are present. 
    // This saves us a network roundtrip if the user missed something obvious.
    if (!form.name || !form.category || !form.unit || !form.availableQuantity || !form.minimumQuantity) {
      setError('Please fill in all required fields.');
      return;
    }

    setLoading(true);
    try {
      // Even though inputs might be type="number", their value in state is often a string.
      // We explicitly cast the quantities to Number so the backend validation doesn't complain.
      await api.post('/api/inventory', {
        name: form.name,
        category: form.category,
        unit: form.unit,
        availableQuantity: Number(form.availableQuantity),
        minimumQuantity: Number(form.minimumQuantity),
        description: form.description,
      });
      
      setSuccess('Item created successfully.');
      // Give the user a brief moment to actually see the success message before routing them away.
      setTimeout(() => navigate('/inventory'), 1200);
    } catch (err) {
      // Safely dig into the error response. If the backend didn't send a nicely formatted message,
      // we fall back to a generic one so the UI doesn't break.
      setError(err.response?.data?.message || 'Failed to create item.');
    } finally {
      // Always turn off the loading state, whether the request succeeded or failed.
      setLoading(false);
    }
  };

  return (
    <div className="page-card form-card">
      <div className="page-header">
        <div>
          <h1>Add Inventory Item</h1>
          <p className="page-subtitle">Create a new stationery item for the inventory.</p>
        </div>
      </div>

      {error && <div className="alert alert-error">{error}</div>}
      {success && <div className="alert alert-success">{success}</div>}

      <form className="form-grid" onSubmit={handleSubmit}>
        <label>
          Name <span className="required">*</span>
          <input name="name" value={form.name} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Category <span className="required">*</span>
          {categories.length ? (
            <select name="category" value={form.category} onChange={handleChange} disabled={loading}>
              <option value="">Select category</option>
              {categories.map((c) => (
                <option key={c.id} value={c.name}>{c.name}</option>
              ))}
            </select>
          ) : (
            <input name="category" value={form.category} onChange={handleChange} disabled={loading} />
          )}
        </label>
        <label>
          Unit <span className="required">*</span>
          <input name="unit" value={form.unit} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Available Quantity <span className="required">*</span>
          <input name="availableQuantity" type="number" min="0" value={form.availableQuantity} onChange={handleChange} disabled={loading} />
        </label>
        <label>
          Minimum Quantity <span className="required">*</span>
          <input name="minimumQuantity" type="number" min="0" value={form.minimumQuantity} onChange={handleChange} disabled={loading} />
        </label>
        <label className="full-width">
          Description
          <textarea name="description" value={form.description} onChange={handleChange} disabled={loading} />
        </label>

        <div className="form-actions full-width">
          <button type="submit" className="btn btn-primary" disabled={loading}>
            {loading ? 'Creating...' : 'Create Item'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default AddItem;
