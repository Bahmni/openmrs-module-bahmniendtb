package org.openmrs.module.bahmniendtb;

import org.bahmni.module.bahmnicore.service.BahmniProgramWorkflowService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.openmrs.PatientProgram;
import org.openmrs.PatientProgramAttribute;
import org.openmrs.ProgramAttributeType;
import org.openmrs.api.APIException;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.when;

public class EndTbProgramServiceValidatorTest {

    @Mock
    private BahmniProgramWorkflowService bahmniProgramWorkflowService;

    private EndTbProgramServiceValidator endTbProgramServiceValidator;

    @Before
    public void setUp() {
        initMocks(this);
        endTbProgramServiceValidator = new EndTbProgramServiceValidator(bahmniProgramWorkflowService);
    }

    @Test(expected=APIException.class)
    public void shouldThrowExceptionIfRegistrationNumberAlreadyPresentForAProgram() {
        when(bahmniProgramWorkflowService.getPatientProgramByAttributeNameAndValue(any(String.class), any(String.class))).thenReturn(Arrays.asList(getPatientProgram()));
        endTbProgramServiceValidator.validate(getPatientProgram());
    }

    private PatientProgram getPatientProgram() {
        ProgramAttributeType programAttributeType = new ProgramAttributeType();
        programAttributeType.setName("Registration Number");

        PatientProgramAttribute patientProgramAttribute = new PatientProgramAttribute();
        patientProgramAttribute.setValue("123");
        patientProgramAttribute.setAttributeType(programAttributeType);

        PatientProgram patientPrograms = new PatientProgram();
        patientPrograms.setAttribute(patientProgramAttribute);

        return patientPrograms;
    }
}