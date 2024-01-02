package org.openmrs.module.endtb.flowsheet.service;

import org.bahmni.flowsheet.config.FlowsheetConfig;
import org.bahmni.flowsheet.definition.models.FlowsheetDefinition;
import org.openmrs.Concept;
import org.openmrs.OrderType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.bahmni.flowsheet.ui.FlowsheetUI;
import org.openmrs.module.endtb.flowsheet.models.FlowsheetAttribute;

import java.util.Date;
import java.util.Set;

public interface PatientMonitoringFlowsheetService {
    FlowsheetUI getFlowsheetForPatientProgram(PatientProgram patientProgram, Date startDate, Date endDate, String configFilePath) throws Exception;

    FlowsheetAttribute getFlowsheetAttributesForPatientProgram(PatientProgram patientPrograms, PatientIdentifierType patientIdentifierType, OrderType orderType, Set<Concept> concepts);

    Date getStartDateForDrugConcepts(String patientProgramUuid, Set<String> drugConcepts, OrderType orderType);

    FlowsheetDefinition getFlowsheetDefinitionFromConfig(FlowsheetConfig flowsheetConfig, Date startDate);
}
