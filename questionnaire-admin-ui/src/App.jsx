import { useState, useEffect, useCallback } from 'react'

// ── API helpers ──────────────────────────────────────────────────────────────

async function apiFetch(url, options = {}) {
  const resp = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...options,
  })
  if (!resp.ok) {
    const text = await resp.text().catch(() => '')
    throw new Error(text || `HTTP ${resp.status}`)
  }
  return resp.status === 204 ? null : resp.json()
}

// questionnaire-service  (proxy: /api → :8082)
const QS = {
  listSections: () => apiFetch('/api/sections'),
  getSection: (sectionId, version) => apiFetch(`/api/sections/${sectionId}/versions/${version}`),
  createDraftRevision: (sectionId, template) =>
    apiFetch(`/api/sections/${sectionId}/draft-revision`, {
      method: 'POST', body: JSON.stringify({ template }),
    }),
  updateDraft: (sectionId, version, template) =>
    apiFetch(`/api/sections/${sectionId}/versions/${version}`, {
      method: 'PUT', body: JSON.stringify({ template }),
    }),
  publish: (sectionId, version) =>
    apiFetch(`/api/sections/${sectionId}/versions/${version}/publish`, { method: 'POST' }),
}

// application-service  (proxy: /app-api → :8080)
const AS = {
  cloneForward: (newSnapshotCode, overrides) =>
    apiFetch('/app-api/snapshots/clone-forward', {
      method: 'POST', body: JSON.stringify({ newSnapshotCode, overrides }),
    }),
}

// ── Helpers ──────────────────────────────────────────────────────────────────

function sectionLabel(labelKey) {
  const part = labelKey.split('.').pop().replace(/_/g, ' ')
  return part.charAt(0).toUpperCase() + part.slice(1)
}

function versionTypeKey(sectionId) {
  return 'SECTION_' + sectionId.toUpperCase().replace(/-/g, '_')
}

function fieldLabel(labelKey) {
  const part = labelKey.split('.').pop().replace(/_/g, ' ')
  return part.charAt(0).toUpperCase() + part.slice(1)
}

const FIELD_TYPES = ['TEXT', 'NUMBER', 'TEXTAREA', 'DROPDOWN']

// ── Blank field template ─────────────────────────────────────────────────────

function blankField() {
  return {
    fieldKey: '',
    labelKey: 'field.',
    fieldType: 'TEXT',
    storage: { type: 'EAV' },
    required: false,
    visibilityRules: [],
    validationRules: [],
  }
}

function fieldsFromTemplate(template) {
  return (template?.fields ?? []).map(f => ({ ...f }))
}

// ── Section List View ────────────────────────────────────────────────────────

function SectionList({ onEdit }) {
  const [sections, setSections] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setSections(await QS.listSections())
      setError(null)
    } catch (e) {
      setError('Could not load sections — is questionnaire-service running on :8082?')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { load() }, [load])

  if (loading) return <div className="page"><p style={{ color: 'var(--text-muted)' }}>Loading…</p></div>

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Section Templates</h2>
          <p className="page-sub">Each section is an independently versioned block of fields.</p>
        </div>
      </div>

      {error && <div className="msg-error">{error}</div>}

      <div className="card">
        <table className="data-table">
          <thead>
            <tr>
              <th>Section ID</th>
              <th>Label</th>
              <th>Active version</th>
              <th>Draft</th>
              <th>Fields</th>
              <th>Grid</th>
              <th style={{ width: 100 }} />
            </tr>
          </thead>
          <tbody>
            {sections.length === 0 ? (
              <tr><td colSpan={7} className="empty-state">No sections found.</td></tr>
            ) : sections.map(s => (
              <tr key={s.sectionId}>
                <td className="col-section-id">{s.sectionId}</td>
                <td className="col-label">{sectionLabel(s.labelKey)}</td>
                <td>
                  {s.activeVersion > 0
                    ? <span className="badge badge-active">v{s.activeVersion} Active</span>
                    : <span style={{ color: 'var(--text-muted)' }}>—</span>}
                </td>
                <td>
                  {s.draftVersion
                    ? <span className="badge badge-draft">v{s.draftVersion} Draft</span>
                    : <span style={{ color: 'var(--text-muted)', fontSize: 12 }}>none</span>}
                </td>
                <td>{s.fieldCount}</td>
                <td>{s.hasGrid ? <span className="badge badge-pending">grid</span> : '—'}</td>
                <td>
                  <button className="btn btn-sm" onClick={() => onEdit(s)}>
                    {s.draftVersion ? 'Continue editing' : 'Edit'}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  )
}

// ── Section Editor View ──────────────────────────────────────────────────────

function SectionEditor({ sectionMeta, onBack }) {
  const [fields, setFields] = useState([])
  const [draftVersion, setDraftVersion] = useState(null) // null = editing from active, not yet saved
  const [loadedVersion, setLoadedVersion] = useState(null)
  const [loadedLabelKey, setLoadedLabelKey] = useState('')
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [published, setPublished] = useState(false)

  useEffect(() => {
    async function loadSection() {
      // Load draft if pending, otherwise load active
      const versionToLoad = sectionMeta.draftVersion ?? sectionMeta.activeVersion
      try {
        const data = await QS.getSection(sectionMeta.sectionId, versionToLoad)
        setFields(fieldsFromTemplate(data.template))
        setLoadedVersion(versionToLoad)
        setLoadedLabelKey(data.labelKey)
        if (sectionMeta.draftVersion) setDraftVersion(sectionMeta.draftVersion)
      } catch (e) {
        setError('Failed to load section: ' + e.message)
      } finally {
        setLoading(false)
      }
    }
    loadSection()
  }, [sectionMeta])

  function updateField(index, key, value) {
    setFields(prev => prev.map((f, i) => {
      if (i !== index) return f
      const updated = { ...f, [key]: value }
      // Auto-update labelKey when fieldKey changes for new EAV fields
      if (key === 'fieldKey' && f.storage?.type === 'EAV' &&
          (f.labelKey === 'field.' || f.labelKey === `field.${f.fieldKey}`)) {
        updated.labelKey = `field.${value}`
      }
      return updated
    }))
    setSuccess(null)
  }

  function addField() {
    setFields(prev => [...prev, blankField()])
    setSuccess(null)
  }

  function removeField(index) {
    setFields(prev => prev.filter((_, i) => i !== index))
    setSuccess(null)
  }

  function buildTemplate() {
    return {
      fields: fields.map(f => ({
        fieldKey: f.fieldKey,
        labelKey: f.labelKey,
        fieldType: f.fieldType,
        storage: f.storage,
        required: f.required,
        visibilityRules: f.visibilityRules ?? [],
        validationRules: f.validationRules ?? [],
        dropdownSource: f.dropdownSource ?? null,
      })),
      grids: [], // grid editing not in scope for this version
    }
  }

  async function handleSaveDraft() {
    const emptyKeys = fields.filter(f => !f.fieldKey.trim())
    if (emptyKeys.length > 0) {
      setError('All fields must have a Field Key before saving.')
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const template = buildTemplate()
      let saved
      if (draftVersion == null) {
        // First save — create a new draft revision from active
        saved = await QS.createDraftRevision(sectionMeta.sectionId, template)
        setDraftVersion(saved.version)
        setLoadedVersion(saved.version)
      } else {
        // Update existing draft
        saved = await QS.updateDraft(sectionMeta.sectionId, draftVersion, template)
      }
      setSuccess(`Draft v${saved.version} saved successfully.`)
    } catch (e) {
      setError('Save failed: ' + e.message)
    } finally {
      setSaving(false)
    }
  }

  async function handlePublish() {
    if (draftVersion == null) {
      setError('Save as draft first before publishing.')
      return
    }
    setPublishing(true)
    setError(null)
    setSuccess(null)
    try {
      // 1. Publish the draft in questionnaire-service (DRAFT → ACTIVE)
      await QS.publish(sectionMeta.sectionId, draftVersion)

      // 2. Clone-forward the snapshot in application-service to point to the new version
      const snapshotCode = `SNAPSHOT-ADMIN-${Date.now()}`
      await AS.cloneForward(snapshotCode, {
        [versionTypeKey(sectionMeta.sectionId)]: { versionValue: draftVersion },
      })

      setPublished(true)
      setSuccess(
        `v${draftVersion} published and snapshot "${snapshotCode}" activated. ` +
        `New loan applications will now use the updated section.`
      )
    } catch (e) {
      setError('Publish failed: ' + e.message)
    } finally {
      setPublishing(false)
    }
  }

  if (loading) {
    return (
      <div className="page">
        <p style={{ color: 'var(--text-muted)' }}>Loading section…</p>
      </div>
    )
  }

  const isReadOnly = published
  const editingDraft = draftVersion != null

  return (
    <div className="page">
      <nav className="breadcrumb">
        <button onClick={onBack}>Section Templates</button>
        <span className="breadcrumb-sep">›</span>
        <span>{sectionLabel(loadedLabelKey)}</span>
      </nav>

      <div className="page-header">
        <div>
          <h2 className="page-title">{sectionLabel(loadedLabelKey)}</h2>
          <p className="page-sub" style={{ fontFamily: 'monospace', fontSize: 12 }}>
            {sectionMeta.sectionId}
          </p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          {editingDraft
            ? <span className="badge badge-draft">Editing v{draftVersion} Draft</span>
            : <span className="badge badge-active">v{sectionMeta.activeVersion} Active (unsaved)</span>}
        </div>
      </div>

      {success && <div className="msg-success">{success}</div>}
      {error && <div className="msg-error">{error}</div>}

      <div className="editor-card">
        <div className="editor-card-header">
          <div>
            <div className="editor-card-title">Fields</div>
            <div className="editor-card-sub">
              {fields.length} field{fields.length !== 1 ? 's' : ''} ·{' '}
              DEDICATED fields (backed by a database column) cannot have their key changed.
            </div>
          </div>
        </div>
        <table className="field-editor">
          <thead>
            <tr>
              <th style={{ width: '22%' }}>Field key</th>
              <th style={{ width: '22%' }}>Label</th>
              <th style={{ width: '12%' }}>Type</th>
              <th style={{ width: '18%' }}>Storage</th>
              <th style={{ width: '8%', textAlign: 'center' }}>Required</th>
              <th style={{ width: '18%' }}>Label key</th>
              <th style={{ width: 36 }} />
            </tr>
          </thead>
          <tbody>
            {fields.map((f, i) => {
              const isDedicated = f.storage?.type === 'DEDICATED'
              return (
                <tr key={i}>
                  <td>
                    <input
                      type="text"
                      value={f.fieldKey}
                      onChange={e => updateField(i, 'fieldKey', e.target.value)}
                      placeholder="e.g. annual_revenue"
                      readOnly={isDedicated || isReadOnly}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      value={fieldLabel(f.labelKey)}
                      onChange={e => {
                        const raw = e.target.value.toLowerCase().replace(/\s+/g, '_')
                        updateField(i, 'labelKey', `field.${raw}`)
                      }}
                      placeholder="Human readable label"
                      readOnly={isReadOnly}
                    />
                  </td>
                  <td>
                    <select
                      value={f.fieldType}
                      onChange={e => updateField(i, 'fieldType', e.target.value)}
                      disabled={isDedicated || isReadOnly}
                    >
                      {FIELD_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </td>
                  <td>
                    {isDedicated
                      ? <span className="field-storage-tag">
                          DEDICATED · {f.storage.tableName}.{f.storage.columnName}
                        </span>
                      : <span className="field-storage-tag">EAV</span>}
                  </td>
                  <td style={{ textAlign: 'center' }}>
                    <input
                      type="checkbox"
                      checked={!!f.required}
                      onChange={e => updateField(i, 'required', e.target.checked)}
                      disabled={isReadOnly}
                    />
                  </td>
                  <td>
                    <input
                      type="text"
                      value={f.labelKey}
                      onChange={e => updateField(i, 'labelKey', e.target.value)}
                      readOnly={isReadOnly}
                      style={{ fontSize: 11, color: 'var(--text-muted)' }}
                    />
                  </td>
                  <td>
                    {!isDedicated && !isReadOnly && (
                      <button
                        className="btn-danger"
                        title="Remove field"
                        onClick={() => removeField(i)}
                      >✕</button>
                    )}
                  </td>
                </tr>
              )
            })}
            {!isReadOnly && (
              <tr className="add-field-row">
                <td colSpan={7}>
                  <button className="btn btn-sm" onClick={addField}>+ Add field</button>
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {!isReadOnly && (
        <div className="action-bar">
          <button className="btn" onClick={onBack}>← Back</button>
          <div className="action-bar-right">
            <button className="btn-primary" onClick={handleSaveDraft} disabled={saving || publishing}>
              {saving ? 'Saving…' : editingDraft ? 'Save draft' : 'Save as draft'}
            </button>
            <button
              className="btn-success"
              onClick={handlePublish}
              disabled={saving || publishing || draftVersion == null}
              title={draftVersion == null ? 'Save as draft first' : ''}
            >
              {publishing ? 'Publishing…' : 'Publish & activate'}
            </button>
          </div>
        </div>
      )}

      {isReadOnly && (
        <div className="action-bar">
          <button className="btn" onClick={onBack}>← Back to sections</button>
          <p style={{ fontSize: 13, color: 'var(--text-secondary)', margin: '0 0 0 8px' }}>
            Published. New applications will use this version.
          </p>
        </div>
      )}
    </div>
  )
}

// ── Root ─────────────────────────────────────────────────────────────────────

export default function App() {
  const [editTarget, setEditTarget] = useState(null) // null = list view

  return (
    <div className="app">
      <header className="banner">
        <div>
          <div className="banner-title">Questionnaire Admin</div>
          <div className="banner-sub">Manage section templates and publish changes</div>
        </div>
      </header>

      {editTarget == null
        ? <SectionList onEdit={s => setEditTarget(s)} />
        : <SectionEditor sectionMeta={editTarget} onBack={() => setEditTarget(null)} />
      }
    </div>
  )
}
