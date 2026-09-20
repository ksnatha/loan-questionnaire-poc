import { render, screen } from '@testing-library/react'
import SectionRenderer from '../components/SectionRenderer'

const KEY_INFO_SECTION = {
  sectionId: 'key-information',
  labelKey: 'section.key_information',
  fields: [
    {
      fieldKey: 'proposal_name',
      fieldType: 'TEXT',
      labelKey: 'field.proposal_name',
      required: true,
      visibilityRules: [],
      validationRules: [],
    },
    {
      fieldKey: 'loan_amount',
      fieldType: 'NUMBER',
      labelKey: 'field.loan_amount',
      required: true,
      visibilityRules: [],
      validationRules: [],
    },
    {
      fieldKey: 'proposal_description',
      fieldType: 'TEXTAREA',
      labelKey: 'field.proposal_description',
      required: false,
      visibilityRules: [],
      validationRules: [],
    },
  ],
  grids: [],
}

test('renders all 3 DEDICATED fields from template JSON', () => {
  render(
    <SectionRenderer
      section={KEY_INFO_SECTION}
      answers={{}}
      onAnswerChange={() => {}}
    />
  )
  expect(screen.getByTestId('field-proposal_name')).toBeInTheDocument()
  expect(screen.getByTestId('field-loan_amount')).toBeInTheDocument()
  expect(screen.getByTestId('field-proposal_description')).toBeInTheDocument()
})

test('TEXT field renders as text input', () => {
  render(<SectionRenderer section={KEY_INFO_SECTION} answers={{}} onAnswerChange={() => {}} />)
  expect(screen.getByTestId('field-proposal_name').tagName).toBe('INPUT')
  expect(screen.getByTestId('field-proposal_name').type).toBe('text')
})

test('NUMBER field renders as number input', () => {
  render(<SectionRenderer section={KEY_INFO_SECTION} answers={{}} onAnswerChange={() => {}} />)
  expect(screen.getByTestId('field-loan_amount').type).toBe('number')
})

test('TEXTAREA field renders as textarea', () => {
  render(<SectionRenderer section={KEY_INFO_SECTION} answers={{}} onAnswerChange={() => {}} />)
  expect(screen.getByTestId('field-proposal_description').tagName).toBe('TEXTAREA')
})

const SHOW_WHEN_YES = {
  ruleId: 'show-rel-id',
  show: true,
  condition: { fieldKey: 'has_rel', operator: 'EQUALS', value: 'YES' },
}

const VISIBILITY_SECTION = {
  sectionId: 'test',
  labelKey: 'section.test',
  fields: [
    {
      fieldKey: 'has_rel',
      fieldType: 'DROPDOWN',
      labelKey: 'field.has_rel',
      required: false,
      visibilityRules: [],
      validationRules: [],
      resolvedOptions: [{ code: 'YES', labelKey: 'Yes' }, { code: 'NO', labelKey: 'No' }],
    },
    {
      fieldKey: 'rel_id',
      fieldType: 'TEXT',
      labelKey: 'field.rel_id',
      required: false,
      visibilityRules: [SHOW_WHEN_YES],
      validationRules: [],
    },
  ],
  grids: [],
}

test('hidden field is not rendered when condition not met', () => {
  render(
    <SectionRenderer
      section={VISIBILITY_SECTION}
      answers={{ has_rel: 'NO' }}
      onAnswerChange={() => {}}
    />
  )
  expect(screen.getByTestId('field-has_rel')).toBeInTheDocument()
  expect(screen.queryByTestId('field-rel_id')).not.toBeInTheDocument()
})

test('hidden field appears when condition is met', () => {
  render(
    <SectionRenderer
      section={VISIBILITY_SECTION}
      answers={{ has_rel: 'YES' }}
      onAnswerChange={() => {}}
    />
  )
  expect(screen.getByTestId('field-has_rel')).toBeInTheDocument()
  expect(screen.getByTestId('field-rel_id')).toBeInTheDocument()
})

test('pre-fills inputs from answers map', () => {
  render(
    <SectionRenderer
      section={KEY_INFO_SECTION}
      answers={{ proposal_name: 'Acme Facility', loan_amount: '500000' }}
      onAnswerChange={() => {}}
    />
  )
  expect(screen.getByTestId('field-proposal_name').value).toBe('Acme Facility')
  expect(screen.getByTestId('field-loan_amount').value).toBe('500000')
})
