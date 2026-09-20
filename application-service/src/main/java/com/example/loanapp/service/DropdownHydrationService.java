package com.example.loanapp.service;

import com.example.common.template.DropdownSource;
import com.example.common.template.FieldDefinition;
import com.example.common.template.FieldOption;
import com.example.loanapp.client.LookupClient;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.List;

@Service
public class DropdownHydrationService {

    private final LookupClient lookupClient;

    public DropdownHydrationService(LookupClient lookupClient) {
        this.lookupClient = lookupClient;
    }

    public List<FieldOption> resolve(FieldDefinition field, LocalDate asOf) {
        DropdownSource src = field.dropdownSource;
        if (src == null) return List.of();
        return switch (src.type) {
            case STATIC   -> src.options != null ? src.options : List.of();
            case CODE_SET -> lookupClient.getCodeSet(src.codeSetType, asOf);
            case EXTERNAL -> lookupClient.getLookup(src.sourceKey);
        };
    }
}
