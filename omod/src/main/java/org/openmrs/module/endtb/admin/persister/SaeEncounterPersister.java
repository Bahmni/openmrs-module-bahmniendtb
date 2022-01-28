package org.openmrs.module.endtb.admin.persister;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.csv.EntityPersister;
import org.bahmni.csv.Messages;
import org.bahmni.module.admin.csv.persister.EncounterPersister;
import org.bahmni.module.bahmnicore.model.bahmniPatientProgram.BahmniPatientProgram;
import org.bahmni.module.bahmnicore.service.BahmniProgramWorkflowService;
import org.openmrs.Patient;
import org.openmrs.Visit;
import org.openmrs.api.VisitService;
import org.openmrs.api.context.Context;
import org.openmrs.api.context.UserContext;
import org.openmrs.module.bahmniemrapi.encountertransaction.contract.BahmniEncounterTransaction;
import org.openmrs.module.bahmniemrapi.encountertransaction.service.BahmniEncounterTransactionService;
import org.openmrs.module.endtb.admin.constants.SAETemplateConstants;
import org.openmrs.module.endtb.admin.encounter.BahmniSaeEncounterTransactionImportService;
import org.openmrs.module.endtb.admin.models.SaeEncounterRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Component
public class SaeEncounterPersister implements EntityPersister<SaeEncounterRow> {

    public static final String IMPORT_ID = "TREATMENT_ID_";
    public static final String REGISTRATION_NUMBER = "Registration Number";

    @Autowired
    private BahmniEncounterTransactionService bahmniEncounterTransactionService;
    @Autowired
    private BahmniSaeEncounterTransactionImportService bahmniSaeEncounterTransactionImportService;
    @Autowired
    private BahmniProgramWorkflowService bahmniProgramWorkflowService;
    @Autowired
    private VisitService visitService;
    private UserContext userContext;
    private String loginUuid;

    private static final Logger log = LogManager.getLogger(EncounterPersister.class);

    public void init(UserContext userContext, String loginUuid) {
        this.userContext = userContext;
        this.loginUuid = loginUuid;
    }

    @Override
    public Messages validate(SaeEncounterRow saeEncounterRow) {
        Messages errorMessages = new Messages();
        if (StringUtils.isEmpty(saeEncounterRow.registrationNumber)) {
            errorMessages.add("Registration number cannot be null.");
        }
        if (StringUtils.isEmpty(saeEncounterRow.saeTerm)) {
            errorMessages.add("SAE Term cannot be null.");
        }
        if (StringUtils.isEmpty(saeEncounterRow.dateOfSaeReport)) {
            errorMessages.add("Date of SAE report cannot be null.");
        }

        if (!isValidDate(saeEncounterRow.dateOfSaeReport)) {
            errorMessages.add("Invalid date format for Date of SAE report. Date format should be 'yyyy-mm-dd'");
        }
        if (!isValidDate(saeEncounterRow.dateOfSaeOnset)) {
            errorMessages.add("Invalid date format for Date of SAE onset. Date format should be 'yyyy-mm-dd'");
        }
        if (!isValidDate(saeEncounterRow.dateOfSaeOutcome)) {
            errorMessages.add("Invalid date format for Date od SAE Outcome. Date format should be 'yyyy-mm-dd'");
        }
        return errorMessages;
    }

    @Override
    public Messages persist(SaeEncounterRow saeEncounterRow) {
        synchronized ((IMPORT_ID + saeEncounterRow.registrationNumber).intern()) {
            try {
                Context.openSession();
                Context.setUserContext(userContext);

                importConditions(saeEncounterRow);

                List<BahmniPatientProgram> bahmniPatientPrograms = bahmniProgramWorkflowService.getPatientProgramByAttributeNameAndValue(REGISTRATION_NUMBER, saeEncounterRow.registrationNumber);

                if(CollectionUtils.isEmpty(bahmniPatientPrograms)) {
                    return noMatchingPatientProgramFound(saeEncounterRow);
                }

                BahmniPatientProgram bahmniPatientProgram = bahmniPatientPrograms.get(0);
                Patient patient = bahmniPatientProgram.getPatient();
                List<Visit> visits = visitService.getVisitsByPatient(patient, false, false);
                Visit latestVisit = visits.get(0);

                BahmniEncounterTransaction bahmniEncounterTransaction = bahmniSaeEncounterTransactionImportService.getSaeEncounterTransaction(saeEncounterRow, patient, bahmniPatientProgram.getUuid());
                if(bahmniEncounterTransaction == null) {
                    return noMatchingSaeFormFound(saeEncounterRow);
                }
                bahmniEncounterTransaction.setPatientProgramUuid(bahmniPatientProgram.getUuid());
                bahmniEncounterTransaction.setLocationUuid(loginUuid);
                bahmniEncounterTransactionService.save(bahmniEncounterTransaction, patient, latestVisit.getStartDatetime(), latestVisit.getStopDatetime());
                return new Messages();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                Context.clearSession();
                return new Messages(e);
            } finally {
                Context.flushSession();
                Context.closeSession();
            }
        }
    }

    private void importConditions(SaeEncounterRow saeEncounterRow) {
        saeEncounterRow.saeOtherCasualFactors = "";
        if(!saeEncounterRow.saeTerm.equalsIgnoreCase(SAETemplateConstants.OTHER_CONCEPT)) {
            saeEncounterRow.otherSaeTerm = "";
        }
        if(!StringUtils.isEmpty(saeEncounterRow.nonTBdrug)) {
            setSaeOtherCasualFactors(saeEncounterRow, SAETemplateConstants.NON_TB_DRUGS_CONCEPT);
        }
        if(!StringUtils.isEmpty(saeEncounterRow.coMorbidity)) {
            setSaeOtherCasualFactors(saeEncounterRow, SAETemplateConstants.COMORBIDITY_CONCEPT);
        }
        if(!StringUtils.isEmpty(saeEncounterRow.otherCausalFactor)) {
            setSaeOtherCasualFactors(saeEncounterRow, SAETemplateConstants.OTHER_CONCEPT);
        }
    }

    private void setSaeOtherCasualFactors(SaeEncounterRow saeEncounterRow, String value) {
        if(StringUtils.isNotEmpty(saeEncounterRow.saeOtherCasualFactors)) {
            saeEncounterRow.saeOtherCasualFactors = saeEncounterRow.saeOtherCasualFactors.concat("|");
        }
        saeEncounterRow.saeOtherCasualFactors = saeEncounterRow.saeOtherCasualFactors.concat(value);
    }

    private static boolean isValidDate(String dateString) {
        if(StringUtils.isEmpty(dateString)) {
            return true;
        }
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            Date date = sdf.parse(dateString);
            if(sdf.format(date).toString().equals(dateString)) {
                return true;
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private Messages noMatchingPatientProgramFound(SaeEncounterRow saeEncounterRow) {
        return new Messages("No matching patients found with ID:'" + saeEncounterRow.registrationNumber + "'");
    }

    private Messages noMatchingSaeFormFound(SaeEncounterRow saeEncounterRow) {
        if(saeEncounterRow.saeTerm.equalsIgnoreCase("Other")) {
            return new Messages("No matching sae form found with 'sae term' as 'Other' and 'other sae term' as '" + saeEncounterRow.otherSaeTerm + "' and 'sae onset date' as '" + saeEncounterRow.dateOfSaeOnset + "'");
        } else {
            return new Messages("No matching sae form found 'with sae term' as '" + saeEncounterRow.saeTerm + "' and 'sae onset date' as '" + saeEncounterRow.dateOfSaeOnset + "'");
        }
    }
}