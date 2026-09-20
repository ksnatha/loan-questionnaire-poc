export function isVisible(field, answers) {
  const rules = field.visibilityRules || []
  if (rules.length === 0) return true
  const showRules = rules.filter(r => r.show)
  if (showRules.length > 0) {
    return showRules.some(r => evalCondition(r.condition, answers))
  }
  return rules.filter(r => !r.show).every(r => !evalCondition(r.condition, answers))
}

export function isRequired(field, answers) {
  if (field.required) return true
  return (field.validationRules || [])
    .filter(r => r.validationType === 'REQUIRED')
    .some(r => !r.condition || evalCondition(r.condition, answers))
}

function evalCondition(condition, answers) {
  if (!condition) return true
  const actual = answers[condition.fieldKey] || ''
  switch (condition.operator) {
    case 'EQUALS':       return condition.value === actual
    case 'NOT_EQUALS':   return condition.value !== actual
    case 'IN':           return (condition.values || []).includes(actual)
    case 'NOT_IN':       return !(condition.values || []).includes(actual)
    case 'IS_EMPTY':     return !actual
    case 'IS_NOT_EMPTY': return !!actual
    default:             return false
  }
}
