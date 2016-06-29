package org.openmrs.module.endtb.bahmniextn.exports;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openmrs.api.APIAuthenticationException;
import org.openmrs.api.context.Context;
import org.openmrs.module.webservices.rest.web.RestConstants;
import org.openmrs.module.webservices.rest.web.v1_0.controller.BaseRestController;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.FileInputStream;

@Controller
public class EndTBExportsController extends BaseRestController implements ResourceLoaderAware {

	private static Logger logger = Logger.getLogger(EndTBExportsController.class);
	private static final String ENDTB_EXPORTS_LOCATION = "endtb.exports.location";

	private final String baseUrl = "/rest/" + RestConstants.VERSION_1 + "/endtb";
	private ResourceLoader resourceLoader;

	@RequestMapping(value = baseUrl + "/export", method = RequestMethod.GET)
	@ResponseBody
	public void downloadFile(HttpServletResponse response, @RequestParam("filename") String filename) {
		try {
			boolean authenticated = Context.isAuthenticated();
			if(!authenticated){
				throw new APIAuthenticationException("Not Logged In");
			}

			Resource resource = resourceLoader.getResource("file:"+Context.getAdministrationService().getGlobalProperty(ENDTB_EXPORTS_LOCATION)+filename);
			IOUtils.copy(new FileInputStream(resource.getFile()),response.getOutputStream());
			response.setContentType("application/zip");
			response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");
			response.flushBuffer();
		} catch (Exception e) {
			logger.error("Unable to send the file ["+filename+"]",e);
		}
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
