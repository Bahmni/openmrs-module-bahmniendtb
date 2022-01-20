package org.openmrs.module.endtb.admin.controller;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bahmni.csv.CSVFile;
import org.bahmni.csv.EntityPersister;
import org.bahmni.fileimport.FileImporter;
import org.bahmni.module.common.db.JDBCConnectionProvider;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.internal.SessionImpl;
import org.openmrs.api.AdministrationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.endtb.admin.models.SaeEncounterRow;
import org.openmrs.module.endtb.admin.persister.SaeEncounterPersister;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.commons.CommonsMultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.Date;

@Controller
public class EndTBAdminImportController extends BaseRestController {
    private final String baseUrl = "/rest/" + RestConstants.VERSION_1 + "/endtb/admin/upload";
    private static Logger logger = LogManager.getLogger(EndTBAdminImportController.class);

    public final String YYYY_MM_DD_HH_MM_SS = "_yyyy-MM-dd_HH:mm:ss";

    public final String PARENT_DIRECTORY_UPLOADED_FILES_CONFIG = "uploaded.files.directory";
    public final String ENCOUNTER_FILES_DIRECTORY = "encounter/";

    @Autowired
    private SaeEncounterPersister saeEncounterPersister;

    @Autowired
    private SessionFactory sessionFactory;

    @Autowired
    @Qualifier("adminService")
    private AdministrationService administrationService;

    @RequestMapping(value = baseUrl + "/encounter", method = RequestMethod.POST)
    @ResponseBody
    public boolean upload(@CookieValue(value = "bahmni.user.location", required = true) String loginCookie,
                          @RequestParam(value = "file") MultipartFile file) throws IOException {
        try {
            JsonParser jsonParser = new JsonParser();
            JsonObject jsonObject = (JsonObject) jsonParser.parse(loginCookie);
            String loginUuid = jsonObject.get("uuid").getAsString();

            saeEncounterPersister.init(Context.getUserContext(), loginUuid);
            return importCsv(ENCOUNTER_FILES_DIRECTORY, file, saeEncounterPersister, 1, false, SaeEncounterRow.class);
        } catch (Throwable e) {
            logger.error("Could not upload file", e);
            throw e;
        }
    }

    private <T extends org.bahmni.csv.CSVEntity> boolean importCsv(String filesDirectory, MultipartFile file, EntityPersister<T> persister,
                                                                   int numberOfThreads, boolean skipValidation, Class entityClass) throws IOException {
        String uploadedOriginalFileName = ((CommonsMultipartFile) file).getFileItem().getName();
        String systemId = Context.getUserContext().getAuthenticatedUser().getSystemId();
        CSVFile persistedUploadedFile = writeToLocalFile(file, filesDirectory);
        return new FileImporter<T>().importCSV(uploadedOriginalFileName, persistedUploadedFile,
                persister, entityClass, new NewMRSConnectionProvider(), systemId, skipValidation, numberOfThreads);
    }


    private CSVFile writeToLocalFile(MultipartFile file, String filesDirectory) throws IOException {
        String uploadedOriginalFileName = ((CommonsMultipartFile) file).getFileItem().getName();
        byte[] fileBytes = file.getBytes();
        CSVFile uploadedFile = getFile(uploadedOriginalFileName, filesDirectory);
        FileOutputStream uploadedFileStream = null;
        try {
            uploadedFileStream = new FileOutputStream(new File(uploadedFile.getAbsolutePath()));
            uploadedFileStream.write(fileBytes);
            uploadedFileStream.flush();
        } catch (Throwable e) {
            logger.error(e);
            throw e;
            // TODO : handle errors for end users. Give some good message back to users.
        } finally {
            if (uploadedFileStream != null) {
                try {
                    uploadedFileStream.close();
                } catch (IOException e) {
                    logger.error(e);
                }
            }
            return uploadedFile;
        }
    }

    private CSVFile getFile(String fileName, String filesDirectory) throws IOException {
        String fileNameWithoutExtension = fileName.substring(0, fileName.lastIndexOf("."));
        String fileExtension = fileName.substring(fileName.lastIndexOf("."));

        String timestampForFile = new SimpleDateFormat(YYYY_MM_DD_HH_MM_SS).format(new Date());

        String uploadDirectory = administrationService.getGlobalProperty(PARENT_DIRECTORY_UPLOADED_FILES_CONFIG);
        String relativePath = filesDirectory + fileNameWithoutExtension + timestampForFile + fileExtension;
        FileUtils.forceMkdir(new File(uploadDirectory, filesDirectory));
        return new CSVFile(uploadDirectory, relativePath);
    }

    private class NewMRSConnectionProvider implements JDBCConnectionProvider {
        private ThreadLocal<Session> session = new ThreadLocal<>();

        @Override
        public Connection getConnection() {
            if (session.get() == null || !session.get().isOpen())
                session.set(sessionFactory.openSession());

            return ((SessionImpl)session.get()).connection();
        }

        @Override
        public void closeConnection() {
            session.get().close();
        }
    }
}