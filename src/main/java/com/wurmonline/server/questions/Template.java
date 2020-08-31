package com.wurmonline.server.questions;

import com.wurmonline.server.items.ItemTemplate;

class Template {
    private final EligibleTemplates eligibleTemplates;
    final ItemTemplate itemTemplate;
    final int templateIndex;
    final String filter;

    Template(int templateIndex, String filter) {
        this.eligibleTemplates = new EligibleTemplates(filter);
        this.templateIndex = templateIndex;
        ItemTemplate template;
        try {
             template = eligibleTemplates.getTemplate(templateIndex);
        } catch (ArrayIndexOutOfBoundsException ignored) {
            template = null;
        }
        itemTemplate = template;
        this.filter = filter;
    }

    Template(Template template, int templateIndex) {
        this.eligibleTemplates = template.eligibleTemplates;
        this.templateIndex = templateIndex;
        itemTemplate = eligibleTemplates.getTemplate(templateIndex);
        this.filter = template.filter;
    }

    String getOptions() {
        return eligibleTemplates.getOptions();
    }

    static Template _default() {
        return new Template(0, "");
    }

    static Template getForTemplateId(int templateId) {
        EligibleTemplates eligible = new EligibleTemplates("");
        return new Template(eligible.getIndexOf(templateId), "");
    }
}
