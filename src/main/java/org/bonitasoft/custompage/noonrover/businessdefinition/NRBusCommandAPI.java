package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.io.File;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommandDeployment;
import org.bonitasoft.command.BonitaCommandDeployment.DeployStatus;
import org.bonitasoft.command.BonitaCommandDescription;
import org.bonitasoft.command.BonitaCommandDescription.CommandJarDependency;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public class NRBusCommandAPI {

    public static Logger logger = Logger.getLogger(NRBusCommandAPI.class.getName());
    private static String logHeader = "NoonRover.cmd";

    private final static BEvent eventAlreadyDeployed = new BEvent(NRBusCommandAPI.class.getName(), 1, Level.INFO,
            "Command already deployed", "The command at the same version is already deployed");
    private final static BEvent eventDeployedWithSuccess = new BEvent(NRBusCommandAPI.class.getName(), 2, Level.INFO,
            "Command deployed with success", "The command are correctly deployed");
    private final static BEvent eventErrorAtDeployment = new BEvent(NRBusCommandAPI.class.getName(), 3,
            Level.APPLICATIONERROR, "Error during deployment of the command", "The command are not deployed",
            "The pâge can not work", "Check the exception");
    private final static BEvent eventNotDeployed = new BEvent(NRBusCommandAPI.class.getName(), 4, Level.ERROR,
            "Command not deployed", "The command is not deployed");
    private final static BEvent eventStartError = new BEvent(NRBusCommandAPI.class.getName(), 5, Level.ERROR,
            "Error during starting the simulation", "Check the error", "No test are started", "See the error");
    private final static BEvent eventUpdateSuccess = new BEvent(NRBusCommandAPI.class.getName(), 6, Level.SUCCESS,
            "Record updated", "Record updated with success");
    private final static BEvent eventInsertSuccess = new BEvent(NRBusCommandAPI.class.getName(), 7, Level.SUCCESS,
            "Record inserted", "Record inserted with success");
    private final static BEvent eventOperationFailed = new BEvent(NRBusCommandAPI.class.getName(), 8, Level.APPLICATIONERROR,
            "Operation failed", "The operation failed", "No update / insert was perform", "Check the status");

    
    private final static BEvent eventExportStarted = new BEvent(NRBusCommandAPI.class.getName(), 9, Level.INFO,
            "Export started", "Export operation started");
    
    public final static String CST_COMMAND_NAME = "NoonRoverCmd";
    public final static String CST_COMMAND_DESCRIPTION = "MoonRover command to add BDM object";
    public final static String CST_COMMAND_JARFILE = "bonita-moonrover-2.5.0.jar";

    private File pageDirectory;
    private CommandAPI commandAPI;
    private PlatformAPI platformAPI;
    private long tenantId;

    public NRBusCommandAPI(File pageDirectory, CommandAPI commandAPI, PlatformAPI platformAPI,
            long tenantId) {
        this.pageDirectory = pageDirectory;
        this.commandAPI = commandAPI;
        this.platformAPI = platformAPI;
        this.tenantId = tenantId;

    }

    /**
     * Call Update
     * 
     * @return
     */
    public List<BEvent> callUpdate(BusinessObject businessObject, List<Map<String, Object>> listRecords) {
        List<BEvent> listEvents = new ArrayList();
        listEvents.addAll(checkAndDeployCommand(pageDirectory, commandAPI, platformAPI, tenantId).listEvents);
        if (BEventFactory.isError(listEvents))
            return listEvents;

        HashMap<String, Serializable> parameters = new HashMap<>();
        parameters.put(NRBusCmdControl.CST_TENANTID, tenantId);
        // BusinessObject businessObject = (BusinessObject) parameterSource.businessDefinition.getObjectTransported();
        // something like com.airtahitinui.bpm.TNWaiverCode
        parameters.put(NRBusCmdControl.CST_BUSINESSNAME, businessObject.getQualifiedName());
        HashMap<String, Object> record = new HashMap<>();
        if (listRecords != null)
            for (Map<String, Object> oneRecordParameter : listRecords) {
                record.put((String) oneRecordParameter.get("name"), oneRecordParameter.get("value"));
            }
        parameters.put(NRBusCmdControl.CST_RECORD, record);
        logger.info(logHeader + " Update BDM with record[" + record + "]");
        
        Map<String, Object> resultUpdate = callCommand(NRBusCmdControl.CST_VERB_UPDATEBDM, parameters, tenantId, commandAPI);

        if (NRBusCmdControl.CST_STATUS_V_OKUPDATE.equals(resultUpdate.get(NRBusCmdControl.CST_STATUS)))
            listEvents.add(eventUpdateSuccess);
        else if (NRBusCmdControl.CST_STATUS_V_OKINSERT.equals(resultUpdate.get(NRBusCmdControl.CST_STATUS)))
            listEvents.add(eventInsertSuccess);
        else
            listEvents.add(new BEvent(eventOperationFailed, (String) resultUpdate.get(NRBusCmdControl.CST_STATUS)));
        return listEvents;
    }

    /**
     * 
     * @param exportName
     * @param entitiesToExport
     * @param maxExportRecordsPerEntity
     * @param directoryExport
     * @return
     */
    public List<BEvent> launchExport(String exportName, List<String> entitiesToExport, long maxExportRecordsPerEntity, APISession apiSession, String directoryExport) {
        List<BEvent> listEvents = new ArrayList();
        listEvents.addAll(checkAndDeployCommand(pageDirectory, commandAPI, platformAPI, tenantId).listEvents);
        if (BEventFactory.isError(listEvents))
            return listEvents;

        HashMap<String, Serializable> parameters = new HashMap<>();
        parameters.put(NRBusCmdControl.CST_TENANTID, tenantId);
        // BusinessObject businessObject = (BusinessObject) parameterSource.businessDefinition.getObjectTransported();
        // something like com.airtahitinui.bpm.TNWaiverCode
        parameters.put(NRBusCmdControl.CST_EXPORTNAME, exportName);
        parameters.put(NRBusCmdControl.CST_EXPORTLISTENTITIES, (ArrayList) entitiesToExport);
        parameters.put(NRBusCmdControl.CST_EXPORTMAXRECORDPERENTITY, maxExportRecordsPerEntity);
        parameters.put(NRBusCmdControl.CST_EXPORTDIRECTORYEXPORT, directoryExport);
        parameters.put(NRBusCmdControl.CST_APISESSION, apiSession);
        
        logger.info(logHeader + " LaunchExport");

        Map<String, Object> resultUpdate = callCommand(NRBusCmdControl.CST_VERB_LAUNCHEXPORT, parameters, tenantId, commandAPI);

        if (NRBusCmdControl.CST_STATUS_V_STARTED.equals(resultUpdate.get(NRBusCmdControl.CST_STATUS)))
            listEvents.add(eventExportStarted);
        else
            listEvents.add(new BEvent(eventOperationFailed, (String) resultUpdate.get(NRBusCmdControl.CST_STATUS)));
        return listEvents;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Command operation */
    /*                                                                                  */
    /* ******************************************************************************** */
    private DeployStatus checkAndDeployCommand(File pageDirectory, CommandAPI commandAPI, PlatformAPI platFormAPI,
            long tenantId) {
        BonitaCommandDescription commandDescription = getMoonRoverCommandDescription(pageDirectory);
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(commandDescription);

        return bonitaCommand.checkAndDeployCommand(commandDescription, true, tenantId, commandAPI, platFormAPI);
    }

    private Map<String, Object> callCommand(String verb, HashMap<String, Serializable> parameters, long tenantId, CommandAPI commandAPI) {
        List<BEvent> listEvents = new ArrayList<>();
        Map<String, Object> resultCommand = new HashMap<>();

        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(NRBusCmdControl.cstCommandName);

        if (bonitaCommand == null) {
            logger.info(logHeader + "~~~~~~~~~~ MoonRover.start() No Command[" + NRBusCmdControl.cstCommandName
                    + "] deployed, stop");
            listEvents.add(eventNotDeployed);
            resultCommand.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
            return resultCommand;
        }

        try {
            
            logger.info(logHeader + "~~~~~~~~~~ Call Command[" + bonitaCommand.getName() + "] Verb[" + verb + "]");
            resultCommand = bonitaCommand.callCommand(verb, parameters, tenantId, commandAPI);

        } catch (final Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe(logHeader + "~~~~~~~~~~  : ERROR Command[" + bonitaCommand.getName() + "] Verb["
                    + verb + "] " + e + " at " + exceptionDetails);
            listEvents.add(new BEvent(eventStartError, e, ""));
        }
        if (listEvents.size() != 0)
            resultCommand.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
        logger.info(logHeader + "~~~~~~~~~~ : END Command[" + bonitaCommand.getName() + "] Verb["
                + verb + "]" + resultCommand);
        return resultCommand;
    }

    /**
     * Get definition
     * 
     * @param pageDirectory
     * @return
     */
    private BonitaCommandDescription getMoonRoverCommandDescription(File pageDirectory) {

        BonitaCommandDescription commandDescription = new BonitaCommandDescription(NRBusCommandAPI.CST_COMMAND_NAME, pageDirectory);
        commandDescription.forceDeploy = false;
        commandDescription.mainCommandClassName = NRBusCmdControl.class.getName();
        commandDescription.mainJarFile = CST_COMMAND_JARFILE;
        commandDescription.commandDescription = CST_COMMAND_DESCRIPTION;

        CommandJarDependency cmdDependency;

        cmdDependency = commandDescription.addJarDependencyLastVersion(BonitaCommandDeployment.NAME, BonitaCommandDeployment.VERSION, BonitaCommandDeployment.JAR_NAME);
        cmdDependency.setForceDeploy(true);

        commandDescription.addJarDependencyLastVersion("bonita-event", "1.10.0", "bonita-event-1.10.0.jar");

        commandDescription.addJarDependencyLastVersion("bonita-properties", "2.8.2", "bonita-properties-2.8.2.jar");

        // don't add the NoonRover Dependency : with Bonita, all dependencies are GLOBAL. 
        return commandDescription;
    }

}
