import { Fragment, useState, useEffect, useCallback } from 'react'

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

async function apiUploadFile(url, file) {
  const formData = new FormData()
  formData.append('file', file)
  const resp = await fetch(url, { method: 'POST', body: formData })
  if (!resp.ok) {
    const text = await resp.text().catch(() => '')
    let message = text
    try { message = JSON.parse(text).error ?? text } catch { /* not JSON */ }
    throw new Error(message || `HTTP ${resp.status}`)
  }
  return resp.json()
}

// questionnaire-service  (proxy: /api → :8082)
const QS = {
  listSections: () => apiFetch('/api/sections'),
  getSection: (sectionId, version) => apiFetch(`/api/sections/${sectionId}/versions/${version}`),
  createDraftRevision: (sectionId, template) =>
    apiFetch(`/api/sections/${sectionId}/draft-revision`, {
      method: 'POST', body: JSON.stringify({ template }),
    }),
  updateDraft: (sectionId, version, template, labelKey = null) =>
    apiFetch(`/api/sections/${sectionId}/versions/${version}`, {
      method: 'PUT', body: JSON.stringify({ template, labelKey }),
    }),
  publish: (sectionId, version) =>
    apiFetch(`/api/sections/${sectionId}/versions/${version}/publish`, { method: 'POST' }),
  bulkUploadFields: (sectionId, file) =>
    apiUploadFile(`/api/sections/${sectionId}/bulk-upload`, file),
}

// application-service  (proxy: /app-api → :8083)
const AS = {
  cloneForward: (newSnapshotCode, overrides) =>
    apiFetch('/app-api/snapshots/clone-forward', {
      method: 'POST', body: JSON.stringify({ newSnapshotCode, overrides }),
    }),
}

// lookup-stub-service  (proxy: /lookup-api → :8081)
const LS = {
  bulkUploadCodeSets: (file) => apiUploadFile('/lookup-api/code-sets/bulk-upload', file),
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

const FIELD_TYPES = ['TEXT', 'NUMBER', 'TEXTAREA', 'DROPDOWN', 'RADIO']

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
  const [grids, setGrids] = useState([]) // not editable in this UI — preserved as-is through save
  const [draftVersion, setDraftVersion] = useState(null) // null = editing from active, not yet saved
  const [loadedVersion, setLoadedVersion] = useState(null)
  const [loadedLabelKey, setLoadedLabelKey] = useState('') // last-known persisted value
  const [labelKeyInput, setLabelKeyInput] = useState('')   // editable field, may differ from loadedLabelKey
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [publishing, setPublishing] = useState(false)
  const [error, setError] = useState(null)
  const [success, setSuccess] = useState(null)
  const [published, setPublished] = useState(false)
  const [uploading, setUploading] = useState(false)

  const loadVersion = useCallback(async (version, { markAsDraft } = {}) => {
    const data = await QS.getSection(sectionMeta.sectionId, version)
    setFields(fieldsFromTemplate(data.template))
    setGrids(data.template?.grids ?? [])
    setLoadedVersion(version)
    setLoadedLabelKey(data.labelKey)
    setLabelKeyInput(data.labelKey)
    if (markAsDraft) setDraftVersion(version)
    return data
  }, [sectionMeta.sectionId])

  useEffect(() => {
    async function loadSection() {
      // Load draft if pending, otherwise load active
      const versionToLoad = sectionMeta.draftVersion ?? sectionMeta.activeVersion
      try {
        await loadVersion(versionToLoad, { markAsDraft: !!sectionMeta.draftVersion })
      } catch (e) {
        setError('Failed to load section: ' + e.message)
      } finally {
        setLoading(false)
      }
    }
    loadSection()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [sectionMeta])

  async function handleBulkUploadFields(file) {
    const confirmed = window.confirm(
      'This replaces all fields and grids in the current draft with the file\'s contents. Continue?'
    )
    if (!confirmed) return

    setUploading(true)
    setError(null)
    setSuccess(null)
    try {
      const resp = await QS.bulkUploadFields(sectionMeta.sectionId, file)
      await loadVersion(resp.draftVersion, { markAsDraft: true })
      setSuccess(`Draft v${resp.draftVersion} loaded from upload — ${resp.fieldCount} field(s), ${resp.gridCount} grid(s).`)
    } catch (e) {
      setError('Bulk upload failed: ' + e.message)
    } finally {
      setUploading(false)
    }
  }

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

  function setDropdownSourceType(index, type) {
    setFields(prev => prev.map((f, i) => {
      if (i !== index) return f
      if (!type) return { ...f, dropdownSource: null }
      if (type === 'STATIC') return { ...f, dropdownSource: { type: 'STATIC', options: f.dropdownSource?.options ?? [] } }
      if (type === 'CODE_SET') return { ...f, dropdownSource: { type: 'CODE_SET', codeSetType: f.dropdownSource?.codeSetType ?? '' } }
      return { ...f, dropdownSource: { type: 'EXTERNAL', sourceKey: f.dropdownSource?.sourceKey ?? '' } }
    }))
    setSuccess(null)
  }

  function updateDropdownSourceAttr(index, key, value) {
    setFields(prev => prev.map((f, i) =>
      i !== index ? f : { ...f, dropdownSource: { ...f.dropdownSource, [key]: value } }
    ))
    setSuccess(null)
  }

  function addStaticOption(index) {
    setFields(prev => prev.map((f, i) => {
      if (i !== index) return f
      const options = [...(f.dropdownSource?.options ?? []), { code: '', labelKey: '' }]
      return { ...f, dropdownSource: { ...f.dropdownSource, options } }
    }))
  }

  function updateStaticOption(index, optIndex, key, value) {
    setFields(prev => prev.map((f, i) => {
      if (i !== index) return f
      const options = (f.dropdownSource?.options ?? []).map((o, oi) =>
        oi !== optIndex ? o : { ...o, [key]: value })
      return { ...f, dropdownSource: { ...f.dropdownSource, options } }
    }))
    setSuccess(null)
  }

  function removeStaticOption(index, optIndex) {
    setFields(prev => prev.map((f, i) => {
      if (i !== index) return f
      const options = (f.dropdownSource?.options ?? []).filter((_, oi) => oi !== optIndex)
      return { ...f, dropdownSource: { ...f.dropdownSource, options } }
    }))
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
      grids, // grid editing UI not in scope — pass through unmodified so saves don't drop it
    }
  }

  async function handleSaveDraft() {
    const emptyKeys = fields.filter(f => !f.fieldKey.trim())
    if (emptyKeys.length > 0) {
      setError('All fields must have a Field Key before saving.')
      return
    }
    if (!labelKeyInput.trim()) {
      setError('Label key cannot be empty.')
      return
    }
    setSaving(true)
    setError(null)
    setSuccess(null)
    try {
      const template = buildTemplate()
      const renamed = labelKeyInput !== loadedLabelKey
      let saved
      if (draftVersion == null) {
        // First save — create a new draft revision from active, then apply the rename
        // (draft-revision creation doesn't carry labelKey; updateDraft does).
        saved = await QS.createDraftRevision(sectionMeta.sectionId, template)
        if (renamed) {
          saved = await QS.updateDraft(sectionMeta.sectionId, saved.version, template, labelKeyInput)
        }
        setDraftVersion(saved.version)
        setLoadedVersion(saved.version)
      } else {
        // Update existing draft
        saved = await QS.updateDraft(sectionMeta.sectionId, draftVersion, template,
          renamed ? labelKeyInput : null)
      }
      setLoadedLabelKey(saved.labelKey)
      setLabelKeyInput(saved.labelKey)
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

      <div className="card" style={{ padding: 14, marginBottom: 20, maxWidth: 420 }}>
        <label style={{ display: 'block', fontSize: 12, color: 'var(--text-secondary)', marginBottom: 6 }}>
          Label key
        </label>
        <input
          type="text"
          value={labelKeyInput}
          onChange={e => { setLabelKeyInput(e.target.value); setSuccess(null) }}
          readOnly={isReadOnly}
          style={{
            width: '100%', fontFamily: 'inherit', fontSize: 13, color: 'var(--text-primary)',
            background: 'var(--surface)', border: '1px solid var(--border)', borderRadius: 4, padding: '6px 8px',
          }}
        />
      </div>

      {!isReadOnly && (
        <div className="card" style={{ padding: 14, marginBottom: 20, display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
          <label className="btn btn-sm" style={{ margin: 0 }}>
            {uploading ? 'Uploading…' : 'Upload fields (CSV/XLSX)'}
            <input
              type="file"
              accept=".csv,.xlsx"
              disabled={uploading}
              style={{ display: 'none' }}
              onChange={e => {
                const f = e.target.files?.[0]
                e.target.value = '' // allow re-selecting the same file next time
                if (f) handleBulkUploadFields(f)
              }}
            />
          </label>
          <a href="/questionnaire-fields-template.csv" download style={{ fontSize: 12 }}>Field template</a>
          <a href="/questionnaire-fields-grid-template.csv" download style={{ fontSize: 12 }}>Grid template</a>
        </div>
      )}

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
              const showDropdownSource = f.fieldType === 'DROPDOWN' || f.fieldType === 'RADIO'
              return (
                <Fragment key={i}>
                <tr>
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
                {showDropdownSource && (
                  <tr className="dropdown-source-row">
                    <td colSpan={7}>
                      <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12, flexWrap: 'wrap', padding: '4px 0' }}>
                        <div>
                          <label style={{ display: 'block', fontSize: 11, color: 'var(--text-muted)', marginBottom: 3 }}>
                            Dropdown source
                          </label>
                          <select
                            value={f.dropdownSource?.type ?? ''}
                            onChange={e => setDropdownSourceType(i, e.target.value || null)}
                            disabled={isReadOnly}
                          >
                            <option value="">— none —</option>
                            <option value="STATIC">STATIC</option>
                            <option value="CODE_SET">CODE_SET</option>
                            <option value="EXTERNAL">EXTERNAL</option>
                          </select>
                        </div>

                        {f.dropdownSource?.type === 'CODE_SET' && (
                          <div>
                            <label style={{ display: 'block', fontSize: 11, color: 'var(--text-muted)', marginBottom: 3 }}>
                              Code set type
                            </label>
                            <input
                              type="text"
                              value={f.dropdownSource.codeSetType ?? ''}
                              onChange={e => updateDropdownSourceAttr(i, 'codeSetType', e.target.value)}
                              placeholder="e.g. PROPERTY_TYPE"
                              readOnly={isReadOnly}
                            />
                          </div>
                        )}

                        {f.dropdownSource?.type === 'EXTERNAL' && (
                          <div>
                            <label style={{ display: 'block', fontSize: 11, color: 'var(--text-muted)', marginBottom: 3 }}>
                              Source key
                            </label>
                            <input
                              type="text"
                              value={f.dropdownSource.sourceKey ?? ''}
                              onChange={e => updateDropdownSourceAttr(i, 'sourceKey', e.target.value)}
                              placeholder="e.g. postcode-lookup"
                              readOnly={isReadOnly}
                            />
                          </div>
                        )}

                        {f.dropdownSource?.type === 'STATIC' && (
                          <div style={{ flex: 1, minWidth: 260 }}>
                            <label style={{ display: 'block', fontSize: 11, color: 'var(--text-muted)', marginBottom: 3 }}>
                              Options (code / label key)
                            </label>
                            {(f.dropdownSource.options ?? []).map((opt, oi) => (
                              <div key={oi} style={{ display: 'flex', gap: 6, marginBottom: 4 }}>
                                <input
                                  type="text"
                                  value={opt.code}
                                  onChange={e => updateStaticOption(i, oi, 'code', e.target.value)}
                                  placeholder="code"
                                  readOnly={isReadOnly}
                                  style={{ width: 110 }}
                                />
                                <input
                                  type="text"
                                  value={opt.labelKey}
                                  onChange={e => updateStaticOption(i, oi, 'labelKey', e.target.value)}
                                  placeholder="label key"
                                  readOnly={isReadOnly}
                                />
                                {!isReadOnly && (
                                  <button className="btn-danger" title="Remove option"
                                    onClick={() => removeStaticOption(i, oi)}>✕</button>
                                )}
                              </div>
                            ))}
                            {!isReadOnly && (
                              <button className="btn btn-sm" onClick={() => addStaticOption(i)}>+ Add option</button>
                            )}
                          </div>
                        )}
                      </div>
                    </td>
                  </tr>
                )}
                </Fragment>
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

// ── Code Sets View ───────────────────────────────────────────────────────────

function CodeSetsPage() {
  const [file, setFile] = useState(null)
  const [uploading, setUploading] = useState(false)
  const [error, setError] = useState(null)
  const [result, setResult] = useState(null)

  async function handleUpload() {
    if (!file) {
      setError('Choose a file first.')
      return
    }
    const confirmed = window.confirm(
      'This will replace all codes for whatever types are in the file. Continue?'
    )
    if (!confirmed) return

    setUploading(true)
    setError(null)
    setResult(null)
    try {
      const resp = await LS.bulkUploadCodeSets(file)
      setResult(resp)
    } catch (e) {
      setError('Upload failed: ' + e.message)
    } finally {
      setUploading(false)
    }
  }

  return (
    <div className="page">
      <div className="page-header">
        <div>
          <h2 className="page-title">Code Sets</h2>
          <p className="page-sub">
            Bulk-replace CODE_SET reference data by type. Full replace per type present in the file —
            types not in the file are untouched.
          </p>
        </div>
      </div>

      {error && <div className="msg-error">{error}</div>}
      {result && (
        <div className="msg-success">
          Replaced {result.typesReplaced.join(', ')} — {result.rowsInserted} row
          {result.rowsInserted !== 1 ? 's' : ''} inserted.
        </div>
      )}

      <div className="card" style={{ padding: 16, display: 'flex', flexDirection: 'column', gap: 12 }}>
        <div>
          <a href="/codeset-template.csv" download>Download CSV template</a>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
          <input
            type="file"
            accept=".csv,.xlsx"
            onChange={e => { setFile(e.target.files?.[0] ?? null); setResult(null); setError(null) }}
          />
          <button className="btn-primary" onClick={handleUpload} disabled={uploading || !file}>
            {uploading ? 'Uploading…' : 'Upload'}
          </button>
        </div>
      </div>
    </div>
  )
}

// ── Root ─────────────────────────────────────────────────────────────────────

export default function App() {
  const [view, setView] = useState('sections') // 'sections' | 'codesets'
  const [editTarget, setEditTarget] = useState(null) // null = section list view

  return (
    <div className="app">
      <header className="banner">
        <div>
          <div className="banner-title">Questionnaire Admin</div>
          <div className="banner-sub">Manage section templates and publish changes</div>
        </div>
      </header>

      <nav className="top-nav" style={{ display: 'flex', gap: 16, padding: '0 24px', borderBottom: '1px solid var(--border, #ddd)' }}>
        <button
          className={view === 'sections' ? 'btn-tab btn-tab-active' : 'btn-tab'}
          onClick={() => { setView('sections'); setEditTarget(null) }}
        >
          Section Templates
        </button>
        <button
          className={view === 'codesets' ? 'btn-tab btn-tab-active' : 'btn-tab'}
          onClick={() => setView('codesets')}
        >
          Code Sets
        </button>
      </nav>

      {view === 'codesets'
        ? <CodeSetsPage />
        : (editTarget == null
            ? <SectionList onEdit={s => setEditTarget(s)} />
            : <SectionEditor sectionMeta={editTarget} onBack={() => setEditTarget(null)} />)
      }
    </div>
  )
}
