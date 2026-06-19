import React, { useEffect, useState } from 'react';
import api from '../api/axiosConfig';

const Logs = () => {
  const [logs, setLogs] = useState([]);
  const [error, setError] = useState('');

  useEffect(() => {
    const load = async () => {
      try {
        const inv = await api.get('/api/inventory/logs');
        const req = await api.get('/api/requests/logs');
        // Combine and sort by timestamp desc
        const combined = [...(inv.data || []), ...(req.data || [])];
        combined.sort((a, b) => new Date(b.timestamp) - new Date(a.timestamp));
        setLogs(combined);
      } catch (err) {
        setError('Failed to load logs.');
      }
    };
    load();
  }, []);

  return (
    <div className="page-card">
      <div className="page-header">
        <h1>Audit Logs</h1>
        <p className="page-subtitle">Recent create/update/delete/approval actions across services.</p>
      </div>

      {error && <div className="alert alert-error">{error}</div>}

      <div className="table-wrapper">
        <table className="data-table">
          <thead>
            <tr>
              <th>User</th>
              <th>Action</th>
              <th>New</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
            {logs.length === 0 && (
              <tr>
                <td colSpan={4} className="empty-row">No logs available</td>
              </tr>
            )}
            {logs.map((l) => (
              <tr key={l.id}>
                <td>{l.username || ''}</td>
                <td>{l.action || ''}</td>
                <td style={{ maxWidth: 400, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{l.newValue || ''}</td>
                <td>{l.timestamp ? new Date(l.timestamp).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' }) : ''}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
};

export default Logs;
