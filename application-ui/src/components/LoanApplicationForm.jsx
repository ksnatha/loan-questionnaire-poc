import { useState, useEffect } from 'react'
import SectionRenderer from './SectionRenderer'
import { isVisible, isRequired } from '../ruleEvaluator'

export default function LoanApplicationForm({ applicationId, onBack }) {
  const [renderData, setRenderData] = useState(null)
  const [answers, setAnswers] = useState({})
  const [gridAnswers, setGridAnswers] = useState({})
  const [status, setStatus] = useState(null)
  const [riskRating, setRiskRating] = useState(null)
  const [activeSection, setActiveSection] = useState(null)
  const [error, setError] = useState(null)
  const [saving, setSaving] = useState(false)

  useEffect(() => {
    fetch(`/api/applications/${applicationId}/render`)
      .then(r => r.json())
      .then(data => {
        setRenderData(data)
        setAnswers(data.answers || {})
        setGridAnswers(data.gridAnswers || {})
        setStatus(data.application.status)
        setRiskRating(data.application.riskRating || null)
        if (data.tab.sections.length > 0) {
          setActiveSection(data.tab.sections[0].sectionId)
        }
      })
      .catch(() => setError('Failed to load application'))
  }, [applicationId])

  function handleAnswerChange(fieldKey, value) {
    setAnswers(prev => {
      const next = { ...prev, [fieldKey]: value }
      if (renderData) {
        for (const section of renderData.tab.sections) {
          for (const field of section.fields) {
            if (!isVisible(field, next)) delete next[field.fieldKey]
          }
        }
      }
      return next
    })
  }

  function handleGridChange(gridKey, rows) {
    setGridAnswers(prev => ({ ...prev, [gridKey]: rows }))
  }

  async function handleSaveDraft() {
    setSaving(true)
    setError(null)
    try {
      const resp = await fetch(`/api/applications/${applicationId}/draft`, {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ answers, gridAnswers }),
      })
      if (!resp.ok) throw new Error('Save failed')
      const body = await resp.json()
      if (body.riskRating) setRiskRating(body.riskRating)
    } catch {
      setError('Failed to save draft')
    } finally {
      setSaving(false)
    }
  }

  async function handleSubmit() {
    setSaving(true)
    setError(null)
    try {
      const resp = await fetch(`/api/applications/${applicationId}/submit`, { method: 'POST' })
      if (resp.status === 422) {
        const body = await resp.json()
        setError(body.errors.map(e => `${e.fieldKey}: ${e.message}`).join(', '))
        return
      }
      if (!resp.ok) throw new Error('Submit failed')
      const body = await resp.json()
      setStatus(body.status)
      if (body.riskRating) setRiskRating(body.riskRating)
    } catch (e) {
      if (!error) setError('Failed to submit')
    } finally {
      setSaving(false)
    }
  }

  if (!renderData) {
    return (
      <div className="app">
        <header className="banner">
          <div className="banner-left">
            <button className="banner-back" onClick={onBack}>← Back</button>
          </div>
        </header>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
                      color: 'var(--text-secondary)', fontSize: 14 }}>
          {error || 'Loading…'}
        </div>
      </div>
    )
  }

  const submitted = status === 'SUBMITTED'
  const app = renderData.application
  const tabLabel = renderData.tab.tabId.charAt(0).toUpperCase() + renderData.tab.tabId.slice(1)
  const proposalName = answers.proposal_name || app.humanReadableId

  return (
    <div className="app">

      <header className="banner">
        <div className="banner-left">
          <button className="banner-back" onClick={onBack}>← Back</button>
          <span className="banner-id">{app.humanReadableId}</span>
          <div>
            <div className="banner-name">{proposalName}</div>
          </div>
        </div>
        <div className="banner-stats">
          {answers.loan_amount && (
            <div className="stat">
              <span className="stat-label">Loan amount</span>
              <span className="stat-value">{formatAmount(answers.loan_amount)}</span>
            </div>
          )}
          {riskRating && (
            <div className="stat">
              <span className="stat-label">Risk rating</span>
              <span className={`badge risk-${riskRating.toLowerCase()}`}>
                {riskRating.charAt(0) + riskRating.slice(1).toLowerCase()}
              </span>
            </div>
          )}
          <div className="stat">
            <span className="stat-label">Status</span>
            <span className={`badge ${submitted ? 'status-submitted' : 'status-draft'}`}>
              {status}
            </span>
          </div>
        </div>
      </header>

      <div className="tabstrip">
        <div className="tab active">{tabLabel}</div>
      </div>

      <div className="body">
        <nav className="sidebar">
          <ul>
            {renderData.tab.sections.map(section => {
              const isActive = activeSection === section.sectionId
              const hasData = isSectionTouched(section, answers, gridAnswers)
              return (
                <li
                  key={section.sectionId}
                  className={`nav-item${isActive ? ' active' : ''}`}
                  onClick={() => setActiveSection(section.sectionId)}
                >
                  <span className={`nav-indicator${hasData ? ' complete' : isActive ? ' current' : ''}`} />
                  {sectionLabel(section.labelKey)}
                </li>
              )
            })}
          </ul>
        </nav>

        <main className="content">
          {submitted && (
            <div className="submit-notice">Application submitted successfully.</div>
          )}

          {renderData.tab.sections
            .filter(s => s.sectionId === activeSection)
            .map(section => (
              <SectionRenderer
                key={section.sectionId}
                section={section}
                answers={answers}
                onAnswerChange={submitted ? () => {} : handleAnswerChange}
                gridAnswers={gridAnswers}
                onGridChange={submitted ? null : handleGridChange}
                readOnly={submitted}
              />
            ))}

          {error && <div className="error-msg">{error}</div>}

          {!submitted && (
            <div className="action-bar">
              <button className="btn-primary" onClick={handleSaveDraft} disabled={saving}>
                {saving ? 'Saving…' : 'Save Draft'}
              </button>
              <button className="btn-secondary" onClick={handleSubmit} disabled={saving}>
                {saving ? 'Submitting…' : 'Submit'}
              </button>
            </div>
          )}
        </main>
      </div>

    </div>
  )
}

function sectionLabel(labelKey) {
  const raw = labelKey.split('.').pop().replace(/_/g, ' ')
  return raw.charAt(0).toUpperCase() + raw.slice(1)
}

function isSectionTouched(section, answers, gridAnswers) {
  const hasField = (section.fields || []).some(
    f => answers[f.fieldKey] && String(answers[f.fieldKey]).trim() !== ''
  )
  const hasGrid = (section.grids || []).some(
    g => (gridAnswers[g.gridKey] || []).length > 0
  )
  return hasField || hasGrid
}

function formatAmount(raw) {
  const n = parseFloat(raw)
  if (isNaN(n)) return raw
  return '$' + n.toLocaleString('en-US', { maximumFractionDigits: 2 })
}
