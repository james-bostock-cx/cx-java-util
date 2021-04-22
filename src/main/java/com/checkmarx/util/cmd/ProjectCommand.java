package com.checkmarx.util.cmd;

import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.dto.cx.CxCustomField;
import com.checkmarx.sdk.dto.cx.CxProject;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxService;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import static com.checkmarx.util.cmd.CmdUtil.addTeamPathSeparatorPrefix;

/**
 * Command for project based operations within Checkmarx
 */
@Component
@Command(name = "project")
public class ProjectCommand implements Callable<Integer> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ProjectCommand.class);
    private final CxService cxService;
    @SuppressWarnings("unused")
    private final CxProperties cxProperties;

    @Spec
    private CommandSpec spec;

    /**
     * TeamCommand Constructor for team based operations against Checkmarx
     * @param cxService
     * @param cxProperties
     */
    public ProjectCommand(CxService cxService, CxProperties cxProperties) {
        this.cxService = cxService;
        this.cxProperties = cxProperties;
    }

    /**
     * Dummy implementation of the call method to implement the Callable
     * interface.
     *
     * @return CommandLine.ExitCode.USAGE
     */
    public Integer call() throws Exception {
        log.info("Calling role command");

	CommandLine.usage(spec, System.err);
        return CommandLine.ExitCode.USAGE;
    }

    /**
     * Set a project's custom fields
     * @throws CheckmarxException
     */
    @Command(name = "set-custom-fields")
    private void setCustomFields(
	    @Option(names = {"-team", "--team"}) String team,
	    @Parameters(paramLabel = "Project") String project,
	    @Parameters(paramLabel = "Custom fields", arity = "1..*") String[] customFields
	    ) throws CheckmarxException{
        log.info("Calling project set-custom-fields command");

        CxProject cxProject = null;

        if (team != null) {
            team = addTeamPathSeparatorPrefix(cxProperties, team);
            String teamId = cxService.getTeamId(team);
            Integer projectId = cxService.getProjectId(teamId, project);
            cxProject = cxService.getProject(projectId);
        } else {
            List<CxProject> projects = cxService.getProjects();
            if (projects.isEmpty()) {
        	throw new CheckmarxException("setCustomFields: no projects found");
            }
            for (CxProject p : projects) {
        	if (p.name.equalsIgnoreCase(project)) {
        	    if (cxProject == null) {
        		cxProject = p;
        	    } else {
        		throw new CheckmarxException(String.format("%s: project name is not unique", project));
        	    }
        	}
            }
        }

        if (cxProject == null) {
            throw new CheckmarxException(String.format("%s: canot find project", project));
        }

	List<CxCustomField> cxCustomFields = cxService.getCustomFields();
	log.debug("setCustomFields: cxCustomFields: {}", cxCustomFields);
	List<CxProject.CustomField> customFieldList = new ArrayList<>();
	for (String customField : customFields) {
	    String[] parts = customField.split("=", 2);
	    String customFieldName = parts[0];
	    String customFieldValue = parts[1];
	    CxProject.CustomField cf = new CxProject.CustomField();
	    for (CxCustomField ccf : cxCustomFields) {
		if (ccf.name.equalsIgnoreCase(customFieldName)) {
		    cf.id = ccf.id;
		    break;
		}
	    }
	    if (cf.id == null) {
		throw new CheckmarxException(String.format("%s: unrecognised custom field", customFieldName));
	    }
	    cf.value = customFieldValue;
	    customFieldList.add(cf);
	}
	cxProject.customFields = customFieldList;
	cxService.updateProjectCustomFields(cxProject);
    }
}