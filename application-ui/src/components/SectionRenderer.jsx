import FieldRenderer from './FieldRenderer'
import GridRenderer from './GridRenderer'
import { isVisible } from '../ruleEvaluator'

export default function SectionRenderer({ section, answers, onAnswerChange, gridAnswers, onGridChange, readOnly }) {
  const label = section.labelKey.split('.').pop().replace(/_/g, ' ')
  const displayLabel = label.charAt(0).toUpperCase() + label.slice(1)
  const visibleFields = (section.fields || []).filter(field => isVisible(field, answers))

  return (
    <div>
      <h1 className="section-title">{displayLabel}</h1>

      {visibleFields.length > 0 && (
        <div className="field-grid">
          {visibleFields.map(field => (
            <FieldRenderer
              key={field.fieldKey}
              field={field}
              value={answers[field.fieldKey]}
              onChange={onAnswerChange}
              fullWidth={field.fieldType === 'TEXTAREA'}
            />
          ))}
        </div>
      )}

      {(section.grids || []).map(grid => (
        <GridRenderer
          key={grid.gridKey}
          grid={grid}
          rows={(gridAnswers || {})[grid.gridKey] || []}
          onRowsChange={rows => onGridChange && onGridChange(grid.gridKey, rows)}
          readOnly={readOnly}
        />
      ))}
    </div>
  )
}
