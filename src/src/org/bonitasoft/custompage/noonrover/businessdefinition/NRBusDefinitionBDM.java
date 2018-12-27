package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.concurrent.Callable;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProcessAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.business.data.BusinessDataRepository;
import org.bonitasoft.engine.command.CommandCriterion;
import org.bonitasoft.engine.command.CommandDescriptor;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.platform.StartNodeException;
import org.bonitasoft.engine.platform.StopNodeException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.transaction.UserTransactionService;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;


public class NRBusDefinitionBDM extends NRBusDefinition {

    public static Logger logger = Logger.getLogger("org.bonitasoft.custompage.noonrover.businessdefinition");

    private static String logHeader = "NoonRover.cmd";

    
    private static BEvent EventAlreadyDeployed = new BEvent(NRBusDefinitionBDM.class.getName(), 1, Level.INFO,
            "Command already deployed", "The command at the same version is already deployed");
    private static BEvent EventDeployedWithSuccess = new BEvent(NRBusDefinitionBDM.class.getName(), 2, Level.INFO,
            "Command deployed with success", "The command are correctly deployed");
    private static BEvent EventErrorAtDeployment = new BEvent(NRBusDefinitionBDM.class.getName(), 3,
            Level.APPLICATIONERROR, "Error during deployment of the command", "The command are not deployed",
            "The pâge can not work", "Check the exception");
    private static BEvent EventNotDeployed = new BEvent(NRBusDefinitionBDM.class.getName(), 4, Level.ERROR,
            "Command not deployed", "The command is not deployed");
    private static BEvent EventStartError = new BEvent(NRBusDefinitionBDM.class.getName(), 5, Level.ERROR,
            "Error during starting the simulation", "Check the error", "No test are started", "See the error");

    private static BEvent eventAPIError = new BEvent(NRBusDefinitionBDM.class.getName(), 6, Level.ERROR,
            "API Error", "Check the error", "Connection is lost", "Reconnect then check the error");
    
    private static BEvent eventUpdateSuccess = new BEvent(NRBusDefinitionBDM.class.getName(), 7, Level.SUCCESS,
            "Record updated", "Record updated with success");
    private static BEvent eventInsertSuccess = new BEvent(NRBusDefinitionBDM.class.getName(), 8, Level.SUCCESS,
            "Record inserted", "Record inserted with success");
    private static BEvent eventOperationFailed = new BEvent(NRBusDefinitionBDM.class.getName(), 9, Level.APPLICATIONERROR,
            "Operation failed", "The operation failed", "No update / insert was perform", "Check the status");

    
/**
Insert
org.bonitasoft.engine.commons.exceptions.SRetryableException: javax.persistence.PersistenceException: org.hibernate.PropertyAccessException: could not get a field value by reflection getter of com.airtahitinui.bpm.TNWaiverCode.persistenceId

Update
java.lang.IllegalArgumentException: Unknown entity: com.airtahitinui.bpm.TNWaiverCode_$$_jvstc9c_0
   */ 
    public NRBusDefinitionBDM(BusinessObject businessObject) {
        super(businessObject.getQualifiedName());
        setObjectTransported(businessObject);
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* API */
    /*                                                                                  */
    /* ******************************************************************************** */

    public Map<String, Object> updateRecordBDM(ParameterSource parameterSource,
            File pageDirectory,
            long tenantId,
            TenantServiceAccessor tenantServiceAccessor) {
        List<BEvent> listEvents= new ArrayList<BEvent>();
        try
        {
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(parameterSource.apiSession);
            
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(parameterSource.apiSession);
            PlatformAPI platformAPI = null;
            
            listEvents.addAll( checkAndDeployCommand(pageDirectory, commandAPI, platformAPI, tenantId));
            if (! BEventFactory.isError( listEvents))
            {
                Map<String, Serializable> parameters = new HashMap<String, Serializable>();
                parameters.put(NRBusCmdControl.cstTenantId, tenantId);
                BusinessObject businessObject = (BusinessObject) parameterSource.businessDefinition.getObjectTransported();
                // something like com.airtahitinui.bpm.TNWaiverCode
                parameters.put(NRBusCmdControl.cstBusinessName, businessObject.getQualifiedName());
                HashMap<String,Object> record = new HashMap<String,Object>();
                if (parameterSource.listRecords!=null)
                    for (Map<String,Object> oneRecordParameter : parameterSource.listRecords)
                    {
                        record.put((String) oneRecordParameter.get("name"), oneRecordParameter.get( "value"));
                    }
                parameters.put(NRBusCmdControl.cstRecord, record);
                
                Map<String, Object> resultUpdate= callCommand(parameters, commandAPI);
                if (NRBusCmdControl.cstStatus_V_OKUPDATE.equals( resultUpdate.get(NRBusCmdControl.cstStatus)))
                    listEvents.add( eventUpdateSuccess);
                else if (NRBusCmdControl.cstStatus_V_OKINSERT.equals( resultUpdate.get(NRBusCmdControl.cstStatus)))
                    listEvents.add( eventInsertSuccess);
                else 
                    listEvents.add( new BEvent( eventOperationFailed, (String) resultUpdate.get(NRBusCmdControl.cstStatus)));
                
            }
        }
        catch( Exception e)
        {
            listEvents.add( new BEvent(eventAPIError, e,""));
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml( listEvents));
        return result;
        
    }
    
    
    
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Command  */
    /*                                                                                  */
    /* ******************************************************************************** */
    private Map<String, Object> callCommand(Map<String, Serializable> parameters, CommandAPI commandAPI) {
        List<BEvent> listEvents = new ArrayList<BEvent>();
        Map<String, Object> resultCommandHashmap = new HashMap<String, Object>();

        final CommandDescriptor command = getCommandByName(NRBusCmdControl.cstCommandName, commandAPI);
        if (command == null) {
            logger.info(logHeader + "~~~~~~~~~~ TruckMilk.start() No Command[" + NRBusCmdControl.cstCommandName
                    + "] deployed, stop");
            listEvents.add(EventNotDeployed);
            resultCommandHashmap.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
            return resultCommandHashmap;
        }

        try {

            // see the command in CmdMeteor
            logger.info(logHeader + "~~~~~~~~~~ Call Command[" + command.getId() + "] Verb["
                    + parameters.get(NRBusCmdControl.cstVerb) + "]");
            final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);

            resultCommandHashmap = (Map<String, Object>) resultCommand;

        } catch (final Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe(logHeader + "~~~~~~~~~~  : ERROR Command[" + command.getId() + "] Verb["
                    + parameters.get(NRBusCmdControl.cstVerb) + "] " + e + " at " + exceptionDetails);
            listEvents.add(new BEvent(EventStartError, e, ""));
        }
        if (listEvents.size() != 0)
            resultCommandHashmap.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
        logger.info(logHeader + "~~~~~~~~~~ : END Command[" + command.getId() + "] Verb["
                + parameters.get(NRBusCmdControl.cstVerb) + "]" + resultCommandHashmap);
        return resultCommandHashmap;
    }
    
    public List<BEvent> checkAndDeployCommand(File pageDirectory, CommandAPI commandAPI, PlatformAPI platFormAPI,
            long tenantId) {
        String message = "";
        boolean forceDeploy = false;
        File fileJar = new File(pageDirectory.getAbsolutePath() + "/lib/CustomPageMoonRover-1.0.jar");;
        String signature = getSignature(fileJar);

        message += "CommandFile[" + fileJar.getAbsolutePath() + "],Signature[" + signature + "]";

        // so no need to have a force deploy here.
        DeployStatus deployStatus = deployCommand(forceDeploy, signature, fileJar, pageDirectory,
                commandAPI, platFormAPI);

        message += "Deployed ?[" + deployStatus.newDeployment + "], Success?["
                + BEventFactory.isError(deployStatus.listEvents) + "]";

        logger.info(logHeader + message);
        return deployStatus.listEvents;
    }

    /**
     * 
     *
     */
    public static class JarDependencyCommand {

        public String jarName;
        public File pageDirectory;

        public JarDependencyCommand(final String name, File pageDirectory) {
            this.jarName = name;
            this.pageDirectory = pageDirectory;
        }

        public String getCompleteFileName() {
            return pageDirectory.getAbsolutePath() + "/lib/" + jarName;
        }
    }

    /**
     * @param name
     * @param pageDirectory
     * @return
     */
    private static JarDependencyCommand getInstanceJarDependencyCommand(final String name, File pageDirectory) {
        return new JarDependencyCommand(name, pageDirectory);
    }

    /**
     * Deploy the command
     * 
     * @param forceDeploy
     * @param version
     * @param fileJar
     * @param pageDirectory
     * @param commandAPI
     * @param platFormAPI
     * @return
     */
    private static class DeployStatus {

        List<BEvent> listEvents = new ArrayList<BEvent>();;
        boolean newDeployment = false;
    }

    private static DeployStatus deployCommand(final boolean forceDeploy, final String version, File fileJar,
            File pageDirectory, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
        // String commandName, String commandDescription, String className,
        // InputStream inputStreamJarFile, String jarName, ) throws IOException,
        // AlreadyExistsException, CreationException, CommandNotFoundException,
        // DeletionException {
        DeployStatus deployStatus = new DeployStatus();

        List<JarDependencyCommand> jarDependencies = new ArrayList<JarDependencyCommand>();
        // execute the groovy scenario
        /*
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "bdm-jpql-query-executor-command-1.0.jar", pageDirectory));
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "process-starter-command-1.0.jar", pageDirectory));
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "scenario-utils-2.0.jar", pageDirectory));
         */
        // execute the meteor command
        jarDependencies.add(getInstanceJarDependencyCommand(fileJar.getName(), pageDirectory));
        jarDependencies.add(getInstanceJarDependencyCommand("bonita-event-1.1.0.jar", pageDirectory));
        jarDependencies.add(getInstanceJarDependencyCommand("bonita-properties-1.6.jar", pageDirectory));

        String message = "";

        try {
            // pause the engine to deploy a command
            if (platFormAPI != null) {
                platFormAPI.stopNode();
            }

            final List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
            for (final CommandDescriptor command : listCommands) {
                if (NRBusCmdControl.cstCommandName.equals(command.getName())) {
                    final String description = command.getDescription();
                    if (!forceDeploy && description.startsWith("V " + version)) {
                        logger.info("NRBusDefinition.cmd >>>>>>>>>>>>>>>>>>>>>>>>> No deployment Command ["
                                + NRBusCmdControl.cstCommandName
                                + "] Version[V " + version + "]");

                        // deployStatus.listEvents.add(new BEvent(EventAlreadyDeployed, "V " + version));
                        deployStatus.newDeployment = false;
                        return deployStatus;
                    }

                    commandAPI.unregister(command.getId());
                }
            }
            logger.info(logHeader + " >>>>>>>>> DEPLOIEMENT Command [" + NRBusCmdControl.cstCommandName
                    + "] Version[V "
                    + version + "]");

            // register globaly
            // MilkScheduleQuartz.deployDependency(fileJar );

            /*
             * File commandFile = new File(jarFileServer); FileInputStream fis =
             * new FileInputStream(commandFile); byte[] fileContent = new
             * byte[(int) commandFile.length()]; fis.read(fileContent);
             * fis.close();
             */
            for (final JarDependencyCommand onejar : jarDependencies) {
                final ByteArrayOutputStream fileContent = new ByteArrayOutputStream();
                final byte[] buffer = new byte[10000];
                int nbRead = 0;
                InputStream inputFileJar = null;
                try {
                    inputFileJar = new FileInputStream(onejar.getCompleteFileName());

                    while ((nbRead = inputFileJar.read(buffer)) > 0) {
                        fileContent.write(buffer, 0, nbRead);
                    }

                    commandAPI.removeDependency(onejar.jarName);
                } catch (final Exception e) {
                    logger.info(logHeader + " Remove dependency[" + e.toString() + "]");
                    message += "Exception remove[" + onejar.jarName + "]:" + e.toString();
                } finally {
                    if (inputFileJar != null)
                        inputFileJar.close();
                }
                //                message += "Adding jarName [" + onejar.jarName + "] size[" + fileContent.size() + "]...";
                commandAPI.addDependency(onejar.jarName, fileContent.toByteArray());
                message += "+";
            }

            message += "Registering...";
            final CommandDescriptor commandDescriptor = commandAPI.register(NRBusCmdControl.cstCommandName,
                    "V " + version + " " + NRBusCmdControl.cstCommandDescription, NRBusCmdControl.class.getName());

            if (platFormAPI != null) {
                platFormAPI.startNode();
            }

            deployStatus.listEvents.add(new BEvent(EventDeployedWithSuccess, message));
            deployStatus.newDeployment = true;
            return deployStatus;

        } catch (final StopNodeException e) {
            logger.severe("Can't stop  [" + e.toString() + "]");
            message += e.toString();
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
            return null;
        } catch (final StartNodeException e) {
            logger.severe("Can't  start [" + e.toString() + "]");
            message += e.toString();
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
            return null;
        } catch (final CommandNotFoundException e) {
            logger.severe("Error during deploy command " + e);
        } catch (final DeletionException e) {
            logger.severe("Error during deploy command " + e);
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
        } catch (final IOException e) {
            logger.severe("Error during deploy command " + e);
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
        } catch (final AlreadyExistsException e) {
            logger.severe("Error during deploy command " + e);
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
        } catch (final CreationException e) {
            logger.severe("Error during deploy command " + e);
            deployStatus.listEvents.add(new BEvent(EventErrorAtDeployment, e,
                    "Command[" + NRBusCmdControl.cstCommandName + "V " + version + " "
                            + NRBusCmdControl.cstCommandDescription + "]"));
        }
        return deployStatus;
    }
    protected static CommandDescriptor getCommandByName(String commandName, CommandAPI commandAPI) {
        List<CommandDescriptor> listCommands = commandAPI.getAllCommands(0, 1000, CommandCriterion.NAME_ASC);
        for (CommandDescriptor command : listCommands) {
            if (commandName.equals(command.getName()))
                return command;
        }
        return null;
    }
    /**
     * in order to know if the file change on the disk, we need to get a signature.
     * the date of the file is not enough in case of a cluster: the file is read in the database then save on the local disk. On a cluster, on each node, the
     * date
     * will be different then. So, a signature is the reliable information.
     * 
     * @param fileToGetSignature
     * @return
     */
    private String getSignature(File fileToGetSignature) {
        long timeStart = System.currentTimeMillis();
        String checksum = "";
        try {
            //Use MD5 algorithm
            MessageDigest md5Digest = MessageDigest.getInstance("MD5");

            //Get the checksum
            checksum = getFileChecksum(md5Digest, fileToGetSignature);

        } catch (Exception e) {
            checksum = "Date_" + String.valueOf(fileToGetSignature.lastModified());
        } finally {
            logger.info(logHeader + " CheckSum [" + fileToGetSignature.getName() + "] is [" + checksum + "] is "
                    + (timeStart - System.currentTimeMillis()) + " ms");
        }
        //see checksum
        return checksum;

    }
    /**
     * calulate the checksum
     * 
     * @param digest
     * @param file
     * @return
     * @throws IOException
     */
    private static String getFileChecksum(MessageDigest digest, File file) throws IOException {
        //Get file input stream for reading the file content
        FileInputStream fis = new FileInputStream(file);

        //Create byte array to read data in chunks
        byte[] byteArray = new byte[1024];
        int bytesCount = 0;

        //Read file data and update in message digest
        while ((bytesCount = fis.read(byteArray)) != -1) {
            digest.update(byteArray, 0, bytesCount);
        } ;

        //close the stream; We don't need it now.
        fis.close();

        //Get the hash's bytes
        byte[] bytes = digest.digest();

        //This bytes[] has bytes in decimal format;
        //Convert it to hexadecimal format
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
        }

        //return complete hash
        return sb.toString();
    }
}
