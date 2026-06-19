import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';
import './Requests.css';

const ManageRequests = () => {
  // We're keeping track of a bunch of state here. 'requests' holds our main data,
  // while 'status' lets us filter the view. The rest handle UI feedback.
  const [requests, setRequests] = useState([]);
  const [status, setStatus] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  // Pull down the latest requests from the backend. 
  // We conditionally pass the 'status' param so we can reuse this for both the unfiltered and filtered views.
  const loadRequests = async () => {
    setLoading(true);
    setError('');
    try {
      const response = await api.get('/api/requests', {
        params: status ? { status } : {},
      });
      // Fallback to an empty array just in case the API gives us back something weird
      setRequests(response.data || []);
    } catch (err) {
      setError('Failed to load requests.');
      setRequests([]);
    } finally {
      setLoading(false);
    }
  };

  // Re-fetch whenever the user changes the filter dropdown.
  // It's cleaner than handling the fetch inside the select's onChange handler.
  useEffect(() => {
    loadRequests();
  }, [status]);

  // Handles approving, rejecting, or fulfilling a request.
  // We use a generic 'action' param to keep things DRY instead of writing three separate functions.
  const updateRequest = async (id, action) => {
    setMessage('');
    setError('');
    try {
      // The backend expects the action right in the URL path.
      const url = `/api/requests/${id}/${action}`;
      
      // If we're rejecting, the backend requires a reason. For now, we're hardcoding a default reason,
      // but ideally we'd want to pop open a modal here to let the admin type a specific reason.
      await api.put(url, action === 'reject' ? { rejectionReason: 'Rejected by admin' } : {});
      
      setMessage(`Request ${action}ed successfully.`);
      
      // Refresh the list so the UI reflects the new state immediately
      loadRequests();
    } catch (err) {
      // Bubble up any specific error messages from the API if they exist, 
      // otherwise fall back to a generic user-friendly message.
      const backendMessage = err.response?.data?.message;
      setError(backendMessage || `Failed to ${action} request.`);
    }
  };

  return (
    <div className="page-card">
      <div className="page-header">
        <div>
          <h1>Manage Requests</h1>
          <p className="page-subtitle">Approve, reject, or fulfill student requests.</p>
        </div>
      </div>

      <div className="request-filter">
        <label>
          Filter by status
          <select value={status} onChange={(e) => setStatus(e.target.value)}>
            <option value="">All</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
            <option value="FULFILLED">Fulfilled</option>
          </select>
        </label>
      </div>

      {message && <div className="alert alert-success">{message}</div>}
      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Request ID</th>
              <th>Student</th>
              <th>Status</th>
              <th>Items</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            {requests.length ? (
              requests.map((request) => (
                <tr key={request.id}>
                  <td>{request.id}</td>
                  <td>{request.requestId}</td>
                  <td>{request.studentUsername}</td>
                  <td>{request.status}</td>
                  <td>{request.items?.map((item) => `${item.itemName} x${item.quantity}`).join(', ')}</td>
                  <td className="action-cell">
                    {request.status === 'PENDING' && (
                      <>
                        <button className="action-btn approve" onClick={() => updateRequest(request.id, 'approve')}>
                          Approve
                        </button>
                        <button className="action-btn reject" onClick={() => updateRequest(request.id, 'reject')}>
                          Reject
                        </button>
                      </>
                    )}
                    {request.status === 'APPROVED' && (
                      <button className="action-btn fulfill" onClick={() => updateRequest(request.id, 'fulfill')}>
                        Fulfill
                      </button>
                    )}
                    {request.status !== 'PENDING' && request.status !== 'APPROVED' && '—'}
                  </td>
                </tr>
              ))
            ) : (
              <tr>
                <td colSpan="6" className="empty-row">
                  {loading ? 'Loading requests...' : 'No requests found.'}
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default ManageRequests;
