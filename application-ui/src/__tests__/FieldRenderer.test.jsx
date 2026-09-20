import { describe, it, expect } from 'vitest'
import { render, screen } from '@testing-library/react'
import FieldRenderer from '../components/FieldRenderer'

describe('FieldRenderer', () => {
  it('renders DROPDOWN as a select with resolved options', () => {
    const field = {
      fieldKey: 'loan_purpose',
      fieldType: 'DROPDOWN',
      labelKey: 'field.loan_purpose',
      required: false,
      resolvedOptions: [
        { code: 'PURCHASE', labelKey: 'Purchase' },
        { code: 'REFI', labelKey: 'Refinance' },
      ],
    }
    render(<FieldRenderer field={field} value="" onChange={() => {}} />)

    const select = screen.getByTestId('field-loan_purpose')
    expect(select.tagName).toBe('SELECT')
    expect(screen.getByText('Purchase')).toBeTruthy()
    expect(screen.getByText('Refinance')).toBeTruthy()
  })

  it('pre-selects the stored code value', () => {
    const field = {
      fieldKey: 'loan_purpose',
      fieldType: 'DROPDOWN',
      labelKey: 'field.loan_purpose',
      required: false,
      resolvedOptions: [
        { code: 'PURCHASE', labelKey: 'Purchase' },
        { code: 'REFI', labelKey: 'Refinance' },
      ],
    }
    render(<FieldRenderer field={field} value="REFI" onChange={() => {}} />)

    const select = screen.getByTestId('field-loan_purpose')
    expect(select.value).toBe('REFI')
  })

  it('renders empty select when resolvedOptions is absent', () => {
    const field = {
      fieldKey: 'some_dropdown',
      fieldType: 'DROPDOWN',
      labelKey: 'field.some_dropdown',
      required: false,
      resolvedOptions: [],
    }
    render(<FieldRenderer field={field} value="" onChange={() => {}} />)

    const select = screen.getByTestId('field-some_dropdown')
    expect(select.tagName).toBe('SELECT')
    // Only the placeholder option
    expect(select.options.length).toBe(1)
  })
})
