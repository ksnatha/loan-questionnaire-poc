export default function FieldRenderer({ field, value, onChange, fullWidth }) {
  const id = `field-${field.fieldKey}`
  const label = field.labelKey.split('.').pop().replace(/_/g, ' ')
  const displayLabel = label.charAt(0).toUpperCase() + label.slice(1)

  let control
  if (field.fieldType === 'DROPDOWN') {
    const options = field.resolvedOptions || []
    control = (
      <select
        id={id}
        data-testid={id}
        value={value || ''}
        onChange={e => onChange(field.fieldKey, e.target.value)}
      >
        <option value="">— Select —</option>
        {options.map(opt => (
          <option key={opt.code} value={opt.code}>{opt.labelKey}</option>
        ))}
      </select>
    )
  } else if (field.fieldType === 'TEXTAREA') {
    control = (
      <textarea
        id={id}
        data-testid={id}
        value={value || ''}
        onChange={e => onChange(field.fieldKey, e.target.value)}
        rows={3}
      />
    )
  } else {
    control = (
      <input
        id={id}
        data-testid={id}
        type={field.fieldType === 'NUMBER' ? 'number' : 'text'}
        value={value || ''}
        onChange={e => onChange(field.fieldKey, e.target.value)}
      />
    )
  }

  return (
    <div className={`field${fullWidth ? ' full' : ''}`}>
      <label htmlFor={id}>
        {displayLabel}
        {field.required && <span className="req">*</span>}
      </label>
      {control}
    </div>
  )
}
