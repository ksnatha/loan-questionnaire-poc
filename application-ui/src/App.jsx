import { useState, useEffect, useCallback } from 'react'
import LoanApplicationForm from './components/LoanApplicationForm'

export default function App() {
  const [applications, setApplications] = useState([])
  const [loading, setLoading] = useState(true)
  const [creating, setCreating] = useState(false)
  const [error, setError] = useState(null)
  const [openId, setOpenId] = useState(null)

  const loadApplications = useCallback(async () => {
    try {
      const resp = await fetch('/api/applications')
      if (!resp.ok) throw new Error('Failed to load')
      setApplications(await resp.json())
      setError(null)
    } catch {
      setError('Could not load applications — is application-service running?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { loadApplications() }, [loadApplications])

  async function handleCreate() {
    setCreating(true)
    setError(null)
    try {
      const resp = await fetch('/api/applications', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ createdUser: 'user' }),
      })
      if (!resp.ok) throw new Error('Failed to create')
      const data = await resp.json()
      setOpenId(data.id)
    } catch {
      setError('Could not create application')
    } finally {
      setCreating(false)
    }
  }

  function handleBack() {
    setOpenId(null)
    setLoading(true)
    loadApplications()
  }

  if (openId != null) {
    return <LoanApplicationForm applicationId={openId} onBack={handleBack} />
  }

  return (
    <div className="app">
      <header className="banner">
        <div className="banner-left">
          <div>
            <div className="banner-name">Loan Applications</div>
            <div className="banner-sub">Dynamic Questionnaire Engine</div>
          </div>
        </div>
      </header>

      <div className="dashboard">
        <div className="dashboard-header">
          <div>
            <h2 className="dashboard-title">All Applications</h2>
          </div>
          <button className="btn-primary" onClick={handleCreate} disabled={creating}>
            {creating ? 'Creating…' : '+ New Application'}
          </button>
        </div>

        {error && <div className="error-msg" style={{ marginBottom: 16 }}>{error}</div>}

        {loading ? (
          <div className="loan-empty" style={{ color: 'var(--text-muted)' }}>Loading…</div>
        ) : (
          <div className="loan-table-wrap">
            <table className="loan-table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Proposal name</th>
                  <th>Amount</th>
                  <th>Risk rating</th>
                  <th>Status</th>
                  <th>Created</th>
                </tr>
              </thead>
              <tbody>
                {applications.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="loan-empty">
                      No applications yet. Click <strong>+ New Application</strong> to get started.
                    </td>
                  </tr>
                ) : (
                  applications.map(app => (
                    <tr key={app.id} onClick={() => setOpenId(app.id)}>
                      <td className="loan-id-cell">{app.humanReadableId}</td>
                      <td className="loan-name-cell">{app.proposalName || <span style={{ color: 'var(--text-muted)' }}>—</span>}</td>
                      <td>{app.loanAmount ? formatAmount(app.loanAmount) : <span style={{ color: 'var(--text-muted)' }}>—</span>}</td>
                      <td>{app.riskRating
                        ? <span className={`badge risk-${app.riskRating.toLowerCase()}`}>{capitalize(app.riskRating)}</span>
                        : <span style={{ color: 'var(--text-muted)' }}>—</span>}
                      </td>
                      <td>
                        <span className={`badge ${app.status === 'SUBMITTED' ? 'status-submitted-inline' : 'status-draft-inline'}`}>
                          {capitalize(app.status)}
                        </span>
                      </td>
                      <td style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>{app.createdDate || '—'}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  )
}

function formatAmount(raw) {
  const n = parseFloat(raw)
  if (isNaN(n)) return raw
  return '$' + n.toLocaleString('en-US', { maximumFractionDigits: 2 })
}

function capitalize(s) {
  if (!s) return s
  return s.charAt(0).toUpperCase() + s.slice(1).toLowerCase()
}
