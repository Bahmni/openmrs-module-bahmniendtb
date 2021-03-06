package org.openmrs.module.bahmniendtb.dataintegrity.service;

import org.openmrs.module.dataintegrity.rule.RuleResult;
import org.openmrs.PatientProgram;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PatientProgramResultsMapper {
    public PatientProgramResultsMapper() {
    }

    List<RuleResult<PatientProgram>> getPatientProgramResults(List<RuleResult> result) {
        List<RuleResult<PatientProgram>> ruleResultsPP = new ArrayList<>();
        for (RuleResult row : result) {
            PatientProgram pp = new PatientProgram();
            pp.setId((Integer) row.getEntity());
            RuleResult rowPP = new RuleResult();
            rowPP.setEntity(pp);
            rowPP.setNotes(row.getNotes());
            rowPP.setActionUrl(row.getActionUrl());
            ruleResultsPP.add(rowPP);
        }
        return ruleResultsPP;
    }
}