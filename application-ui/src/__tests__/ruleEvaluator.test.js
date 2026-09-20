import { describe, it, expect } from 'vitest'
import { isVisible, isRequired } from '../ruleEvaluator'

const showWhenYes = {
  ruleId: 'show-when-yes',
  show: true,
  condition: { fieldKey: 'has_rel', operator: 'EQUALS', value: 'YES' },
}

const requiredWhenVariable = {
  ruleId: 'required-when-variable',
  validationType: 'REQUIRED',
  condition: { fieldKey: 'rate_type', operator: 'EQUALS', value: 'VARIABLE' },
}

describe('isVisible', () => {
  it('returns true when field has no visibility rules', () => {
    expect(isVisible({ visibilityRules: [] }, {})).toBe(true)
  })

  it('hides field when show-rule condition is not met', () => {
    const field = { visibilityRules: [showWhenYes] }
    expect(isVisible(field, { has_rel: 'NO' })).toBe(false)
  })

  it('shows field when show-rule condition is met', () => {
    const field = { visibilityRules: [showWhenYes] }
    expect(isVisible(field, { has_rel: 'YES' })).toBe(true)
  })

  it('treats missing trigger as empty (condition not met)', () => {
    const field = { visibilityRules: [showWhenYes] }
    expect(isVisible(field, {})).toBe(false)
  })
})

describe('isRequired', () => {
  it('returns true when field.required is true regardless of answers', () => {
    expect(isRequired({ required: true, validationRules: [] }, {})).toBe(true)
  })

  it('returns true when conditional required rule fires', () => {
    const field = { required: false, validationRules: [requiredWhenVariable] }
    expect(isRequired(field, { rate_type: 'VARIABLE' })).toBe(true)
  })

  it('returns false when conditional required rule does not fire', () => {
    const field = { required: false, validationRules: [requiredWhenVariable] }
    expect(isRequired(field, { rate_type: 'FIXED' })).toBe(false)
  })

  it('returns false when trigger field is absent', () => {
    const field = { required: false, validationRules: [requiredWhenVariable] }
    expect(isRequired(field, {})).toBe(false)
  })
})
