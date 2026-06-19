import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import api from '../api/axiosConfig';
import './FormPage.css';

const EditItem = () => {
  // Grab the item ID from the URL params. We'll use this to fetch and update the specific item.
  const { id } = useParams();
  const navigate = useNavigate();
  
  // We keep all form fields in a single state object to make handling changes easier,
  // rather than having six different useState calls.
  const [form, setForm] = useState({
    name: '',
    category: '',
    unit: '',
    availableQuantity: '',
    minimumQuantity: '',
    description: '',
  });
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);

  // We want to fetch the item's existing data right when the component mounts 
  // or if the ID somehow changes.
  useEffect(() => {
    const loadItem = async () => {
      setLoading(true);
      setError('');
      try {
        const response = await api.get(`/api/inventory/${id}`);
        const item = response.data;
        
        // Populate the form state with the fetched data. 
        // We use the nullish coalescing operator (??) for quantities because 0 is a valid value we don't want to overwrite with an empty string.
        setForm({
          name: item.name || '',
          category: item.category || '',
          unit: item.unit || '',
          availableQuantity: item.availableQuantity ?? '',
          minimumQuantity: item.minimumQuantity ?? '',
          description: item.description || '',
        });
      } catch (err) {
        setError('Unable to load item details.');
      } finally {
        setLoading(false);
      }
    };

    loadItem();
  }, [id]);

  // A generic change handler. Since our inputs have 'name' attributes that match 
  // our state keys, we can just spread the previous state and update the changed field.
  const handleChange = (e) => {
    const { name, value } = e.target;
    setForm((prev) => ({ ...prev, [name]: value }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setSuccess('');

    // Basic frontend validation to catch missing fields before making an API call.
    // It saves an unnecessary network request if the user missed something.
    if (!form.name || !form.category || !form.unit || form.availableQuantity === '' || form.minimumQuantity === '') {
      setError('Please fill in all required fields.');
      return;
    }

    setLoading(true);
    try {
      // The API expects numbers for quantities, so we explicitly cast them using Number().
      await api.put(`/api/inventory/${id}`, {
        name: form.name,
        category: form.category,
        unit: form.unit,
        availableQuantity: Number(form.availableQuantity),
        minimumQuantity: Number(form.minimumQuantity),
        description: form.description,
      });
      
      setSuccess('Item updated successfully.');
      // Give the user a brief moment to see the success message before kicking them back to the inventory list.
      setTimeout(() => navigate('/inventory'), 1000);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to update item.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="page-card form-card">
      <div className="page-header">
        <div>
          <h1>Edit Inventory Item</h1>
          <p className="page-subtitle">Update the details of your stationery item.</p>
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
          <input name="category" value={form.category} onChange={handleChange} disabled={loading} />
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
            {loading ? 'Updating...' : 'Update Item'}
          </button>
        </div>
      </form>
    </div>
  );
};

export default EditItem;
