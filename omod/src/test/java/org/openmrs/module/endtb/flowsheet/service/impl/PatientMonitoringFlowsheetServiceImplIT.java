package org.openmrs.module.endtb.flowsheet.service.impl;


import org.bahmni.flowsheet.api.models.Milestone;
import org.bahmni.flowsheet.ui.FlowsheetUI;
import org.bahmni.module.bahmnicore.model.bahmniPatientProgram.BahmniPatientProgram;
import org.bahmni.module.bahmnicore.service.BahmniProgramWorkflowService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.Concept;
import org.openmrs.OrderType;
import org.openmrs.PatientIdentifierType;
import org.openmrs.PatientProgram;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.ConceptService;
import org.openmrs.api.PatientService;
import org.openmrs.api.context.Context;
import org.openmrs.module.bahmniendtb.EndTBConstants;
import org.openmrs.module.endtb.flowsheet.models.FlowsheetAttribute;
import org.openmrs.module.endtb.flowsheet.service.PatientMonitoringFlowsheetService;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.bahmni.module.bahmnicore.mapper.PatientIdentifierMapper.BAHMNI_PRIMARY_IDENTIFIER_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class PatientMonitoringFlowsheetServiceImplIT extends BaseModuleWebContextSensitiveTest {


    private BahmniProgramWorkflowService bahmniProgramWorkflowService;

    private PatientService patientService;

    private AdministrationService administrationService;

    private org.openmrs.api.OrderService orderService;

    private ConceptService conceptService;

    @Autowired
    PatientMonitoringFlowsheetService patientMonitoringFlowsheetService;

    @Before
    public void setUp() throws Exception {
        executeDataSet("patientProgramTestData.xml");
        executeDataSet("flowsheetTestData.xml");
        bahmniProgramWorkflowService = Context.getService(BahmniProgramWorkflowService.class);
        patientService = Context.getPatientService();
        administrationService = Context.getAdministrationService();
        orderService = Context.getOrderService();
        conceptService = Context.getConceptService();
    }

    @Test
    public void shouldSetFlowsheetAttributes() {
        BahmniPatientProgram bahmniPatientProgram = (BahmniPatientProgram) bahmniProgramWorkflowService.getPatientProgramByUuid("dfdfoifo-dkcd-475d-b939-6d82327f36a3");
        PatientIdentifierType patientIdentifierType = patientService.getPatientIdentifierTypeByUuid(administrationService.getGlobalProperty(BAHMNI_PRIMARY_IDENTIFIER_TYPE));
        OrderType orderType = orderService.getOrderTypeByUuid(OrderType.DRUG_ORDER_TYPE_UUID);
        Set<Concept> conceptsForDrugs = new HashSet<>();
        conceptsForDrugs.add(conceptService.getConceptByName(EndTBConstants.DRUG_BDQ));
        conceptsForDrugs.add(conceptService.getConceptByName(EndTBConstants.DRUG_DELAMANID));

        FlowsheetAttribute flowsheetAttribute = patientMonitoringFlowsheetService.getFlowsheetAttributesForPatientProgram(bahmniPatientProgram, patientIdentifierType, orderType, conceptsForDrugs);

        assertEquals("ARM10021", flowsheetAttribute.getPatientEMRID());
        assertEquals("2016-09-16 00:00:00.0", flowsheetAttribute.getMdrtbTreatmentStartDate().toString());
        assertEquals("2016-01-27 00:30:00.0", flowsheetAttribute.getNewDrugTreatmentStartDate().toString());
        assertEquals("REG12345", flowsheetAttribute.getTreatmentRegistrationNumber());
        assertEquals("Negative", flowsheetAttribute.getHivStatus());
        assertEquals("Yes", flowsheetAttribute.getBaselineXRayStatus());
    }


    @Test
    public void shouldSetFlowsheetWhenEndDateIsGiven() throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date flowsheetStartDate = simpleDateFormat.parse("2016-1-10");
        Date flowsheetEndDate = simpleDateFormat.parse("2016-03-30");

        String configFilePath = "src/test/resources/patientMonitoringConf.json";
        PatientProgram patientProgram = bahmniProgramWorkflowService.getPatientProgramByUuid("kkkkkkfo-okok-475d-b939-6d82327f36a3");
        FlowsheetUI flowsheetUI = patientMonitoringFlowsheetService.getFlowsheetForPatientProgram(patientProgram, flowsheetStartDate, flowsheetEndDate, configFilePath);

        assertNotNull(flowsheetUI);
        Map<String, List<String>> flowsheetData = flowsheetUI.getFlowsheetData();
        Set<String> keys = flowsheetData.keySet();
        List<String> questions = new ArrayList<>();
        for (String key : keys) {
            questions.add(key);
        }
        List<Milestone> flowsheetHeaders = flowsheetUI.getMilestones();

        List<String> coloursForNewDrugs = flowsheetData.get("New Drugs");
        List<String> coloursForBloodPressure = flowsheetData.get("Blood Pressure");


        assertEquals("New Drugs", questions.get(0));
        assertEquals("Blood Pressure", questions.get(1));
        assertEquals("M1", flowsheetHeaders.get(0).getName());
        assertEquals("M2", flowsheetHeaders.get(1).getName());
        assertEquals("M3", flowsheetHeaders.get(2).getName());
        assertEquals("M4", flowsheetHeaders.get(3).getName());
        assertEquals("MTx", flowsheetHeaders.get(4).getName());
        assertEquals("M6M", flowsheetHeaders.get(5).getName());
        assertEquals("M3", flowsheetUI.getHighlightedMilestone());
        assertEquals(6, flowsheetData.get("New Drugs").size());
        assertEquals(6, flowsheetData.get("Blood Pressure").size());
        assertEquals("grey", coloursForNewDrugs.get(0));
        assertEquals("green", coloursForNewDrugs.get(1));
        assertEquals("green", coloursForNewDrugs.get(2));
        assertEquals("green", coloursForNewDrugs.get(3));
        assertEquals("green", coloursForNewDrugs.get(4));
        assertEquals("green", coloursForNewDrugs.get(5));
        assertEquals("purple", coloursForBloodPressure.get(0));
        assertEquals("purple", coloursForBloodPressure.get(1));
        assertEquals("purple", coloursForBloodPressure.get(2));
        assertEquals("grey", coloursForBloodPressure.get(3));
        assertEquals("grey", coloursForBloodPressure.get(4));
        assertEquals("purple", coloursForBloodPressure.get(5));
        assertEquals(0, flowsheetHeaders.get(0).getQuestions().size());
        assertEquals(0, flowsheetHeaders.get(1).getQuestions().size());
    }

    @Test
    public void shouldSetFlowsheetWhenEndDateIsNotGiven() throws Exception {

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date flowsheetStartDate = simpleDateFormat.parse("2016-1-10");


        String configFilePath = "src/test/resources/patientMonitoringConf.json";
        PatientProgram patientProgram = bahmniProgramWorkflowService.getPatientProgramByUuid("kkkkkkfo-okok-475d-b939-6d82327f36a3");
        FlowsheetUI flowsheetUI = patientMonitoringFlowsheetService.getFlowsheetForPatientProgram(patientProgram, flowsheetStartDate, null, configFilePath);

        assertNotNull(flowsheetUI);
        Map<String, List<String>> flowsheetData = flowsheetUI.getFlowsheetData();
        Set<String> keys = flowsheetData.keySet();
        List<String> questions = new ArrayList<>();
        for (String key : keys) {
            questions.add(key);
        }

        List<Milestone> flowsheetHeaders = flowsheetUI.getMilestones();


        List<String> coloursForNewDrugs = flowsheetData.get("New Drugs");
        List<String> coloursForBloodPressure = flowsheetData.get("Blood Pressure");


        assertEquals("New Drugs", questions.get(0));
        assertEquals("Blood Pressure", questions.get(1));
        assertEquals("M1", flowsheetHeaders.get(0).getName());
        assertEquals("M2", flowsheetHeaders.get(1).getName());
        assertEquals("M3", flowsheetHeaders.get(2).getName());
        assertEquals("M4", flowsheetHeaders.get(3).getName());
        assertEquals("MTx", flowsheetHeaders.get(4).getName());
        assertEquals("M6M", flowsheetHeaders.get(5).getName());
        assertEquals("", flowsheetUI.getHighlightedMilestone());
        assertEquals(6, flowsheetData.get("New Drugs").size());
        assertEquals(6, flowsheetData.get("Blood Pressure").size());
        assertEquals("grey", coloursForNewDrugs.get(0));
        assertEquals("green", coloursForNewDrugs.get(1));
        assertEquals("green", coloursForNewDrugs.get(2));
        assertEquals("green", coloursForNewDrugs.get(3));
        assertEquals("green", coloursForNewDrugs.get(4));
        assertEquals("green", coloursForNewDrugs.get(5));
        assertEquals("purple", coloursForBloodPressure.get(0));
        assertEquals("purple", coloursForBloodPressure.get(1));
        assertEquals("purple", coloursForBloodPressure.get(2));
        assertEquals("purple", coloursForBloodPressure.get(3));
        assertEquals("grey", coloursForBloodPressure.get(4));
        assertEquals("purple", coloursForBloodPressure.get(5));
    }


}
