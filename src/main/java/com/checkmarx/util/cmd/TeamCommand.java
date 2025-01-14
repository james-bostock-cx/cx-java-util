package com.checkmarx.util.cmd;

import com.checkmarx.sdk.config.CxProperties;
import com.checkmarx.sdk.exception.CheckmarxException;
import com.checkmarx.sdk.service.CxService;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Unmatched;
import picocli.CommandLine.Parameters;
import java.util.concurrent.Callable;


/**
 * Command for Team based operations within Checkmarx
 */
@Component
@Command
public class TeamCommand implements Callable<Integer> {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(TeamCommand.class);
    private final CxService cxService;
    private final CxProperties cxProperties;

    @Option(names = {"-command","--command"}, description = "Command name")
    private String command;
    @Option(names = {"-action","--action"}, description = "Action to execute - create, delete, add-ldap, remove-ldap,")
    private String action;
    @Option(names = {"-t","--team"}, description = "Checkmarx Team")
    private String team;
    @Option(names = {"-create","--create"}, description = "Create team if it does not exist (parent team must exist)")
    private boolean create;
    @Option(names = {"-s","--ldap-server"}, description = "LDAP Server Name")
    private String ldapServer;
    @Option(names = {"-m","--add-ldap-map"}, description = "Add LDAP DN Mapping")
    private String addLdapDn;
    @Option(names = {"-r","--remove-ldap-map"}, description = "Remove LDAP DN Mapping")
    private String removeLdapDn;
    @Parameters
    private String[] remainder;
    @Unmatched
    private String[] unknown;

    /**
     * TeamCommand Constructor for team based operations against Checkmarx
     * @param cxService
     * @param cxProperties
     */
    public TeamCommand(CxService cxService, CxProperties cxProperties) {
        this.cxService = cxService;
        this.cxProperties = cxProperties;
    }

    /**
     * Entry point for Command to execute
     * @return 0 if success, or throws exception if failure
     * @throws Exception
     */
    public Integer call() throws Exception {
        log.info("Calling Team Command");
        if(!team.startsWith(this.cxProperties.getTeamPathSeparator())){
            team = this.cxProperties.getTeamPathSeparator().concat(team);
        }
        switch (action.toUpperCase()){
            case "CREATE":
                createTeam();
                break;
            case "DELETE":
                deleteTeam();
                break;
            case "ADD-LDAP":
                addLdapMapping();
                break;
            case "REMOVE-LDAP":
                removeLdapMapping();
                break;
        }

        return 0;
    }

    /**
     * Map a team to an ldap group dn
     * If the team does not exist, and the the create flag is set, it will be created first
     * @throws CheckmarxException
     */
    private void addLdapMapping() throws CheckmarxException{
        if(create){
            log.info("Creating team if it does not exits.");
            createTeam();
        }
        String teamId = cxService.getTeamId(team);
        String teamName = getTeamName();
        if(teamId.equals("-1")){
            log.error("Could not find team {}", team);
            throw new CheckmarxException("Could not find team ".concat(team));
        }
        if(StringUtils.isNotEmpty(ldapServer)) {
            Integer serverId = cxService.getLdapServerId(ldapServer);
            if(serverId > 0) {
                cxService.mapTeamLdapWS(serverId, teamId, teamName, addLdapDn);
                log.info("Ldap mapping {} has been added to team {}", addLdapDn, team);
            }
            else {
                log.error("Ldap Server {} not found ", ldapServer);
                throw new CheckmarxException("Ldap Server not found");
            }
        }
        else{
            log.error("No Ldap Server provided");
            throw new CheckmarxException("Ldap Server not provided");
        }
    }

    /**
     * Remove an Ldap groupd dn mapping for a team
     * @throws CheckmarxException
     */
    private void removeLdapMapping() throws CheckmarxException{
        String teamId = cxService.getTeamId(team);
        String teamName = getTeamName();
        if(teamId.equals("-1")){
            log.error("Could not find team {}", team);
            throw new CheckmarxException("Could not find team ".concat(team));
        }
        if(StringUtils.isNotEmpty(ldapServer)) {
            Integer serverId = cxService.getLdapServerId(ldapServer);
            if(serverId > 0) {
                cxService.removeTeamLdapWS(serverId, teamId, teamName, addLdapDn);
                log.info("Ldap mapping {} has been removed from team {}", addLdapDn, team);
            }
            else {
                log.error("Ldap Server {} not found ", ldapServer);
                throw new CheckmarxException("Ldap Server not found");
            }
        }
        else{
            log.error("No Ldap Server provided");
            throw new CheckmarxException("Ldap Server not provided");
        }
    }

    /**
     * Create a team (if it doesn't exist)
     * @throws CheckmarxException
     */
    private void createTeam() throws CheckmarxException {
        //check if the team exists
        if(!cxService.getTeamId(team).equals("-1")){
            log.warn("Team already exists...");
            return;
        }
        //get the parent and create the team
        int idx = team.lastIndexOf(this.cxProperties.getTeamPathSeparator());
        String parentPath = team.substring(0, idx);
        String teamName = getTeamName();
        log.info("Parent path: {}", parentPath);
        String parentId = cxService.getTeamId(parentPath);
        log.info(parentId);
        cxService.createTeam(parentId, teamName);
    }

    /**
     * Delete a given team
     *
     * @throws CheckmarxException
     */
    private void deleteTeam() throws CheckmarxException{
        String teamId = cxService.getTeamId(team);
        if(teamId.equals("-1")){
            log.warn("Could not find team {}", team);
        }
        else {
            log.info("Deleting team {} with Id {}", team, teamId);
            cxService.deleteTeam(teamId);
        }
    }

    /**
     * Get the teamname from the full path
     * @return
     */
    private String getTeamName(){
        int idx = team.lastIndexOf(this.cxProperties.getTeamPathSeparator());
        return team.substring(idx+1);
    }
}
