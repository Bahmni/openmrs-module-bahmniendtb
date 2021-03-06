package org.openmrs.module.bahmniendtb;

import org.apache.commons.collections.CollectionUtils;
import org.bahmni.module.bahmnicore.model.bahmniPatientProgram.BahmniPatientProgram;
import org.bahmni.module.bahmnicore.model.bahmniPatientProgram.PatientProgramAttribute;
import org.bahmni.module.bahmnicore.service.BahmniProgramServiceValidator;
import org.bahmni.module.bahmnicore.service.BahmniProgramWorkflowService;
import org.openmrs.PatientProgram;
import org.openmrs.api.APIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.openmrs.module.bahmniendtb.EndTBConstants.PROGRAM_ATTRIBUTE_REG_NO;

@Component
public class EndTbProgramServiceValidator implements BahmniProgramServiceValidator {
    private BahmniProgramWorkflowService bahmniProgramWorkflowService;

    @Autowired
    public EndTbProgramServiceValidator(BahmniProgramWorkflowService bahmniProgramWorkflowService) {
        this.bahmniProgramWorkflowService = bahmniProgramWorkflowService;
    }

    @Override
    public void validate(PatientProgram patientProgram) throws APIException {
        for (PatientProgramAttribute patientProgramAttribute : ((BahmniPatientProgram) patientProgram).getAttributes()) {
            if (PROGRAM_ATTRIBUTE_REG_NO.equals(patientProgramAttribute.getAttributeType().getName())) {
                List<BahmniPatientProgram> bahmniPatientPrograms = bahmniProgramWorkflowService.getPatientProgramByAttributeNameAndValue(PROGRAM_ATTRIBUTE_REG_NO, (String) patientProgramAttribute.getValue());
                if (isPatientProgramPresent(patientProgram, bahmniPatientPrograms)) {
                    throw new APIException("Registration number is already used for another Treatment");
                }
            }
        }
    }

    private boolean isPatientProgramPresent(PatientProgram patientProgram, List<BahmniPatientProgram> bahmniPatientPrograms) {
        if(CollectionUtils.isNotEmpty(bahmniPatientPrograms)) {
            for(BahmniPatientProgram bahmniPatientProgram : bahmniPatientPrograms) {
                if(!bahmniPatientProgram.getUuid().equals(patientProgram.getUuid())) {
                    return true;
                }
            }
        }
        return false;
    }
}
