package com.example.common.template;

import java.util.ArrayList;
import java.util.List;

public class TabTemplateJson {

    public String tabId;
    public String labelKey;
    public List<TabSectionRef> sections = new ArrayList<>();

    public TabTemplateJson() {}
}
