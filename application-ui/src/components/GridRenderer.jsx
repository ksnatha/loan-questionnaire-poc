export default function GridRenderer({ grid, rows, onRowsChange, readOnly }) {
  const label = grid.labelKey.split('.').pop().replace(/_/g, ' ')
  const displayLabel = label.charAt(0).toUpperCase() + label.slice(1)

  function addRow() {
    onRowsChange([...rows, {}])
  }

  function removeRow(index) {
    onRowsChange(rows.filter((_, i) => i !== index))
  }

  function handleCellChange(rowIndex, fieldKey, value) {
    onRowsChange(rows.map((row, i) =>
      i === rowIndex ? { ...row, [fieldKey]: value } : row
    ))
  }

  return (
    <div>
      <div className="grid-section-header">
        <span className="grid-section-title">{displayLabel}</span>
        {!readOnly && (
          <button className="btn" onClick={addRow}>+ Add row</button>
        )}
      </div>

      {rows.length === 0 ? (
        <p className="grid-empty">No rows yet.</p>
      ) : (
        <div className="grid-table-wrap">
          <table className="grid-table">
            <thead>
              <tr>
                {grid.columns.map(col => {
                  const h = col.labelKey.split('.').pop().replace(/_/g, ' ')
                  return <th key={col.fieldKey}>{h.charAt(0).toUpperCase() + h.slice(1)}</th>
                })}
                {!readOnly && <th style={{ width: 32 }} />}
              </tr>
            </thead>
            <tbody>
              {rows.map((row, rowIndex) => (
                <tr key={rowIndex}>
                  {grid.columns.map(col => (
                    <td key={col.fieldKey}>
                      {col.fieldType === 'DROPDOWN' ? (
                        <select
                          value={row[col.fieldKey] || ''}
                          onChange={e => handleCellChange(rowIndex, col.fieldKey, e.target.value)}
                          disabled={readOnly}
                        >
                          <option value="">—</option>
                          {(col.resolvedOptions || []).map(opt => (
                            <option key={opt.code} value={opt.code}>{opt.labelKey}</option>
                          ))}
                        </select>
                      ) : (
                        <input
                          type={col.fieldType === 'NUMBER' ? 'number' : 'text'}
                          value={row[col.fieldKey] || ''}
                          onChange={e => handleCellChange(rowIndex, col.fieldKey, e.target.value)}
                          readOnly={readOnly}
                        />
                      )}
                    </td>
                  ))}
                  {!readOnly && (
                    <td>
                      <button className="btn-remove" onClick={() => removeRow(rowIndex)} title="Remove row">✕</button>
                    </td>
                  )}
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
