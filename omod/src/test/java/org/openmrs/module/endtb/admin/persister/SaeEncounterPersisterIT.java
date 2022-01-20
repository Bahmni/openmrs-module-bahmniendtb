package org.openmrs.module.endtb.admin.persister;

import org.bahmni.csv.Messages;
import org.bahmni.module.bahmnicore.service.BahmniObsService;
import org.hibernate.FlushMode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openmrs.api.context.Context;
import org.openmrs.api.db.hibernate.DbSessionFactory;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniObservation;
import org.openmrs.module.endtb.admin.constants.SAETemplateConstants;
import org.openmrs.module.endtb.admin.models.SaeEncounterRow;
import org.openmrs.module.endtb.admin.models.SaeTBDrugTreatmentRow;
import org.openmrs.web.test.BaseModuleWebContextSensitiveTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;

import java.util.*;
import java.util.stream.Collectors;

@RunWith(SpringJUnit4ClassRunner.class)
@org.springframework.test.context.ContextConfiguration(locations = {"classpath:TestingApplicationContext.xml"}, inheritLocations = true)
public class SaeEncounterPersisterIT  extends BaseModuleWebContextSensitiveTest {

    @Autowired
    private SaeEncounterPersister saeEncounterPersister;

    @Autowired
    DbSessionFactory sessionFactory;

    private FlushMode flushMode;

    @Autowired
    private BahmniObsService bahmniObsService;

    @Before
    public void setUp() throws Exception {
        flushMode = sessionFactory.getCurrentSession().getFlushMode();
        sessionFactory.getCurrentSession().setFlushMode(FlushMode.MANUAL);

        executeDataSet("diagnosisMetadata.xml");
        executeDataSet("dispositionMetadata.xml");
        executeDataSet("saeMetaData.xml");
        executeDataSet("patientProgramTestData.xml");
        executeDataSet("saeTestData.xml");
        saeEncounterPersister.init(Context.getUserContext(), null);
    }

    @After
    public void destroy() {
        sessionFactory.getCurrentSession().setFlushMode(flushMode);
    }

    @Test
    public void itShouldNotImportWhenPatientDoesntExist() throws Exception{
        SaeEncounterRow encounterRow = createSaeEncounterRow("DUMMY", "Cardiac Rhythm", "2017-02-01", "TRUE");

        Messages messages = saeEncounterPersister.persist(encounterRow);
        assertEquals("No matching patients found with ID:'" + encounterRow.registrationNumber + "'", messages.get(0).toString());
    }

    @Test
    public void itShouldNotImportWhenSAEDoesntMatch() throws Exception{
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Cardiac Rhythm", "2017-02-01", "TRUE");

        Messages messages = saeEncounterPersister.persist(encounterRow);
        assertEquals("No matching sae form found 'with sae term' as '" + encounterRow.saeTerm + "' and 'sae onset date' as '" + encounterRow.dateOfSaeOnset + "'", messages.get(0).toString());
    }

    @Test
    public void itShouldImportSAEEncounterWithNoTBDrugSection() throws Exception{
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(0, SAETbTreatments.size());
    }

    @Test
    public void itShouldImportSAEEncounterWithEmptyTBDrugSection() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("", "", "");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(0, SAETbTreatments.size());
    }

    @Test
    public void itShouldImportSAEEncounterWithTBDrugSection() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());

        BahmniObservation SAETbTreatmentPV = SAETbTreatments.get(0);
        BahmniObservation SAETbDrug = filterByConceptName( SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
        BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

        assertEquals(tbDrugTreatmentRow.tbDrug, SAETbDrug.getValueAsString());
        assertEquals(tbDrugTreatmentRow.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
    }

    @Test
    public void itShouldImportSAEFormWithMultipleTBDrugSections() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Isoniazid", "FALSE", "Dose maintained");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(2, SAETbTreatments.size());

        int check = 0;
        for (BahmniObservation SAETbDrugTreatmentPV : SAETbTreatments) {
            BahmniObservation SAETbDrug = filterByConceptName( SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
            BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

            if(tbDrugTreatmentRow.tbDrug.equals(SAETbDrug.getValueAsString())) {
                assertEquals(tbDrugTreatmentRow.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
                check = check + 1;
            }
            else {
                assertEquals(tbDrugTreatmentRow2.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
                check = check + 10;
            }
        }
        assertEquals(11, check);
    }

    @Test
    public void itShouldImportOnlyOneSectionWhenMultipleIdenticalTBDrugSectionsImported() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());
    }

    @Test
    public void itShouldNotDuplicateIdenticalExistingTBDrugSection() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);

        saeEncounterPersister.persist(encounterRow);
        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());
    }

    @Test
    public void itShouldOverwriteExistingTBDrugSectionWithMatchingDrug() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);

        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose maintained");
        SaeEncounterRow encounterRow2 = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow2.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);

        saeEncounterPersister.persist(encounterRow);
        saeEncounterPersister.persist(encounterRow2);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());

        BahmniObservation SAETbTreatmentPV = SAETbTreatments.get(0);
        BahmniObservation SAETbDrug = filterByConceptName( SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
        BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

        assertEquals(tbDrugTreatmentRow2.tbDrug, SAETbDrug.getValueAsString());
        assertEquals(tbDrugTreatmentRow2.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
    }

    @Test
    public void itShouldOverwriteExistingTBDrugSectionsWhenReimported() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Isoniazid", "FALSE", "Dose maintained");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);
        saeEncounterPersister.persist(encounterRow);

        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(2, SAETbTreatments.size());

        int check = 0;
        for (BahmniObservation SAETbDrugTreatmentPV : SAETbTreatments) {
            BahmniObservation SAETbDrug = filterByConceptName( SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
            BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

            if(tbDrugTreatmentRow.tbDrug.equals(SAETbDrug.getValueAsString())) {
                assertEquals(tbDrugTreatmentRow.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
                check = check + 1;
            }
            else {
                assertEquals(tbDrugTreatmentRow2.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
                check = check + 10;
            }
        }
        assertEquals(11, check);
    }

    @Test
    public void itShouldOverwriteTBDrugSectionsList() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Isoniazid", "FALSE", "Dose maintained");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);
        saeEncounterPersister.persist(encounterRow);

        encounterRow.saeTBDrugTreatmentRows.remove(1);
        encounterRow.saeTBDrugTreatmentRows.get(0).tbDrugRelated = "";
        encounterRow.saeTBDrugTreatmentRows.get(0).tbDrugFinalAction = "";
        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());

        BahmniObservation SAETbTreatmentPV = SAETbTreatments.get(0);
        BahmniObservation SAETbDrug = filterByConceptName( SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
        BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

        assertEquals(tbDrugTreatmentRow.tbDrug, SAETbDrug.getValueAsString());
        assertEquals(null, tbDrugFinalAction);
    }

    @Test
    public void itShouldReplaceMultipleTBDrugSections() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Isoniazid", "FALSE", "Dose maintained");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        saeEncounterPersister.persist(encounterRow);

        tbDrugTreatmentRow.tbDrugFinalAction = "";
        tbDrugTreatmentRow.tbDrugRelated = "FALSE";
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);
        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(2, SAETbTreatments.size());

        int check = 0;
        for (BahmniObservation SAETbDrugTreatmentPV : SAETbTreatments) {
            BahmniObservation SAETbDrug = filterByConceptName( SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_NAME);
            BahmniObservation tbDrugFinalAction = filterByConceptName(SAETbDrugTreatmentPV, SAETemplateConstants.SAE_TB_DRUG_FINAL_ACTION);

            if(tbDrugTreatmentRow.tbDrug.equals(SAETbDrug.getValueAsString())) {
                assertEquals(null, tbDrugFinalAction);
                check = check + 1;
            }
            else {
                assertEquals(tbDrugTreatmentRow2.tbDrugFinalAction, tbDrugFinalAction.getValueAsString());
                check = check + 10;
            }
        }
        assertEquals(11, check);
    }

    @Test
    public void itShouldOverwriteTBDrugSectionWithEmpty() throws Exception{
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = createSaeTBDrugTreatmentRow("Bedaquiline", "TRUE", "Dose reduced");
        SaeTBDrugTreatmentRow tbDrugTreatmentRow2 = createSaeTBDrugTreatmentRow("Isoniazid", "FALSE", "Dose maintained");
        SaeEncounterRow encounterRow = createSaeEncounterRow("REG123456", "Hypertension", "2017-02-01", "TRUE");
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow);
        encounterRow.saeTBDrugTreatmentRows.add(tbDrugTreatmentRow2);

        saeEncounterPersister.persist(encounterRow);
        tbDrugTreatmentRow.tbDrugRelated = "FALSE";
        tbDrugTreatmentRow2.tbDrug = "";
        tbDrugTreatmentRow2.tbDrugFinalAction = "";
        tbDrugTreatmentRow2.tbDrugRelated = "";
        saeEncounterPersister.persist(encounterRow);

        Context.openSession();
        Context.authenticate("admin", "test");
        Collection<BahmniObservation> bahmniObservations = bahmniObsService.getObservationsForPatientProgram("ppuuid2", Arrays.asList(SAETemplateConstants.SAE_TEMPLATE), null);
        Context.closeSession();

        BahmniObservation SAEObservation = bahmniObservations.stream().findFirst().get();
        BahmniObservation SAEOutcome = filterByConceptName(SAEObservation, SAETemplateConstants.SAE_OUTCOME_PV);

        List<BahmniObservation> SAETbTreatments = SAEOutcome.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(SAETemplateConstants.SAE_TB_DRUG_TREATMENT))
                .collect(Collectors.toList());
        assertEquals(1, SAETbTreatments.size());
    }

    private BahmniObservation filterByConceptName(BahmniObservation parentObservation, String conceptName) {
        return parentObservation.getGroupMembers()
                .stream()
                .filter(observation -> observation.getConcept().getName().equalsIgnoreCase(conceptName))
                .findFirst().orElse(null);
    }

    private SaeEncounterRow createSaeEncounterRow(String regNum, String saeTerm, String dateOfSaeOnset, String saeRelatedTbDrug) {
        SaeEncounterRow encounterRow = new SaeEncounterRow();
        encounterRow.registrationNumber = regNum;
        encounterRow.dateOfSaeOnset = dateOfSaeOnset;
        encounterRow.dateOfSaeReport = "";
        encounterRow.saeCaseNumber = "";
        encounterRow.saeTerm = saeTerm;
        encounterRow.maxSeverityOfSae = "";
        encounterRow.dateOfSaeOutcome = "";
        encounterRow.saeOutcome = "";
        encounterRow.saeRelatedTbDrug = saeRelatedTbDrug;
        encounterRow.saeTBDrugTreatmentRows = new ArrayList<>();

        encounterRow.otherCausalFactor = "";
        encounterRow.nonTBdrug = "";
        encounterRow.otherSaeTerm = "";
        encounterRow.coMorbidity = "";
        encounterRow.saeOtherCasualFactors = "";

        return encounterRow;
    }

    private SaeTBDrugTreatmentRow createSaeTBDrugTreatmentRow(String drugName, String isRelated, String finalAction) {
        SaeTBDrugTreatmentRow tbDrugTreatmentRow = new SaeTBDrugTreatmentRow();
        tbDrugTreatmentRow.tbDrug = drugName;
        tbDrugTreatmentRow.tbDrugRelated= isRelated;
        tbDrugTreatmentRow.tbDrugFinalAction = finalAction;
        return tbDrugTreatmentRow;
    }

}
