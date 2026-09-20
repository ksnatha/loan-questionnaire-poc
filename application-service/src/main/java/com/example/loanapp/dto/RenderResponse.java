package com.example.loanapp.dto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class RenderResponse {
    public ApplicationResponse application;
    public RenderedTab tab;
    public Map<String, String> answers;
    // gridKey -> ordered list of row answer maps
    public Map<String, List<Map<String, String>>> gridAnswers = new LinkedHashMap<>();
}
