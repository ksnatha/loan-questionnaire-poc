package com.example.loanapp.service;

import com.example.common.template.*;
import com.example.loanapp.client.LookupClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DropdownHydrationServiceTest {

    @Mock LookupClient lookupClient;
    @InjectMocks DropdownHydrationService service;

    private static final LocalDate AS_OF = LocalDate.of(2026, 9, 20);

    @Test
    void staticReturnsTemplateOptionsWithoutCallingLookupClient() {
        FieldDefinition field = new FieldDefinition();
        field.fieldKey = "has_rel";
        field.dropdownSource = DropdownSource.staticSource(List.of(
            new FieldOption("YES", "Yes"), new FieldOption("NO", "No")));

        List<FieldOption> result = service.resolve(field, AS_OF);

        assertEquals(2, result.size());
        assertEquals("YES", result.get(0).code);
        verifyNoInteractions(lookupClient);
    }

    @Test
    void codeSetDelegatesToLookupClient() {
        FieldDefinition field = new FieldDefinition();
        field.fieldKey = "loan_purpose";
        field.dropdownSource = DropdownSource.codeSet("LOAN_PURPOSE");

        List<FieldOption> expected = List.of(new FieldOption("PURCHASE", "Purchase"));
        when(lookupClient.getCodeSet("LOAN_PURPOSE", AS_OF)).thenReturn(expected);

        List<FieldOption> result = service.resolve(field, AS_OF);

        assertSame(expected, result);
        verify(lookupClient).getCodeSet("LOAN_PURPOSE", AS_OF);
    }

    @Test
    void externalDelegatesToLookupClientGetLookup() {
        FieldDefinition field = new FieldDefinition();
        field.fieldKey = "ext_field";
        field.dropdownSource = DropdownSource.external("SOME_SOURCE");

        List<FieldOption> expected = List.of(new FieldOption("A", "Option A"));
        when(lookupClient.getLookup("SOME_SOURCE")).thenReturn(expected);

        List<FieldOption> result = service.resolve(field, AS_OF);

        assertSame(expected, result);
        verify(lookupClient).getLookup("SOME_SOURCE");
    }

    @Test
    void nullDropdownSourceReturnsEmptyList() {
        FieldDefinition field = new FieldDefinition();
        field.fieldKey = "plain_text";
        field.dropdownSource = null;

        List<FieldOption> result = service.resolve(field, AS_OF);

        assertTrue(result.isEmpty());
        verifyNoInteractions(lookupClient);
    }
}
