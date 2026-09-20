package com.example.loanapp.dto;

import java.util.List;
import java.util.Map;

public class SaveDraftRequest {
    public Map<String, String> answers;
    // gridKey -> ordered list of row answer maps
    public Map<String, List<Map<String, String>>> gridAnswers;
}
