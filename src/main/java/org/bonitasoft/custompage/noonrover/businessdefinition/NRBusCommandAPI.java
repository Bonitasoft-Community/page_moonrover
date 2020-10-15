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
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public class NRBusCommandAPI {

    public static Logger logger = Logger.getLogger(NRBusCommandAPI.class.getName());
    private static String logHeader = "NoonRover.cmd";

    private static BEvent EventAlreadyDeployed = new BEvent(NRBusCommandAPI.class.getName(), 1, Level.INFO,
            "Command already deployed", "The command at the same version is already deployed");
    private static BEvent EventDeployedWithSuccess = new BEvent(NRBusCommandAPI.class.getName(), 2, Level.INFO,
            "Command deployed with success", "The command are correctly deployed");
    private static BEvent EventErrorAtDeployment = new BEvent(NRBusCommandAPI.class.getName(), 3,
            Level.APPLICATIONERROR, "Error during deployment of the command", "The command are not deployed",
            "The pâge can not work", "Check the exception");
    private static BEvent EventNotDeployed = new BEvent(NRBusCommandAPI.class.getName(), 4, Level.ERROR,
            "Command not deployed", "The command is not deployed");
    private static BEvent EventStartError = new BEvent(NRBusCommandAPI.class.getName(), 5, Level.ERROR,
            "Error during starting the simulation", "Check the error", "No test are started", "See the error");

    public final static String cstCommandName = "NoonRoverCmd";
    public final static String cstCommandDescription = "MoonRover command to add BDM object";
    public final static String cstCommandJarFile = "bonita-moonrover-2.3.1.jar";
    
    public DeployStatus checkAndDeployCommand(File pageDirectory, CommandAPI commandAPI, PlatformAPI platFormAPI,
            long tenantId) {
        BonitaCommandDescription commandDescription = getMoonRoverCommandDescription(pageDirectory);
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(commandDescription);

        return bonitaCommand.checkAndDeployCommand(commandDescription, true, tenantId, commandAPI, platFormAPI);
    }
    
    /* ******************************************************************************** */
    /*                                                                                  */
    /* Command  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public Map<String, Object> callCommand(HashMap<String, Serializable> parameters, long tenantId, CommandAPI commandAPI) {
        List<BEvent> listEvents = new ArrayList<>();
        Map<String, Object> resultCommand = new HashMap<>();
        
        BonitaCommandDeployment bonitaCommand = BonitaCommandDeployment.getInstance(NRBusCmdControl.cstCommandName);
   
        if (bonitaCommand == null) {
            logger.info(logHeader + "~~~~~~~~~~ MoonRover.start() No Command[" + NRBusCmdControl.cstCommandName
                    + "] deployed, stop");
            listEvents.add(EventNotDeployed);
            resultCommand.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
            return resultCommand;
        }

        try {

            // 
            String verb=(String) parameters.get(NRBusCmdControl.cstVerb);
            logger.info(logHeader + "~~~~~~~~~~ Call Command[" + bonitaCommand.getName() + "] Verb[" + verb + "]");
            // final Serializable resultCommand = commandAPI.execute(command.getId(), parameters);
            resultCommand = bonitaCommand.callCommand(verb, parameters, tenantId, commandAPI);
            

        } catch (final Exception e) {

            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe(logHeader + "~~~~~~~~~~  : ERROR Command[" + bonitaCommand.getName() + "] Verb["
                    + parameters.get(NRBusCmdControl.cstVerb) + "] " + e + " at " + exceptionDetails);
            listEvents.add(new BEvent(EventStartError, e, ""));
        }
        if (listEvents.size() != 0)
            resultCommand.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
        logger.info(logHeader + "~~~~~~~~~~ : END Command[" + bonitaCommand.getName()+ "] Verb["
                + parameters.get(NRBusCmdControl.cstVerb) + "]" + resultCommand);
        return resultCommand;
    }
    
    private BonitaCommandDescription getMoonRoverCommandDescription(File pageDirectory) {

        BonitaCommandDescription commandDescription = new BonitaCommandDescription(NRBusCommandAPI.cstCommandName, pageDirectory);
        commandDescription.forceDeploy = false;
        commandDescription.mainCommandClassName = NRBusCmdControl.class.getName();
        commandDescription.mainJarFile = cstCommandJarFile;
        commandDescription.commandDescription =cstCommandDescription ;



        CommandJarDependency cmdDependency;
        
        cmdDependency=commandDescription.addJarDependencyLastVersion(BonitaCommandDeployment.NAME, BonitaCommandDeployment.VERSION, BonitaCommandDeployment.JAR_NAME);
        cmdDependency.setForceDeploy( true );
        
        
        cmdDependency=commandDescription.addJarDependencyLastVersion("bonita-event", "1.9.0", "bonita-event-1.9.0.jar");

        cmdDependency=commandDescription.addJarDependencyLastVersion("bonita-properties", "2.7.0", "bonita-properties-2.7.0.jar");

        
        // don't add the Meteor Dependency : with Bonita, all dependencies are GLOBAL. If we reference the MeteorAPI, we will have the same API for all pages
        // and that's impact the meteor page.
        return commandDescription;
    }

    
    /*
     *  
     * Deploy the command
     * 
     * @param forceDeploy
     * @param version
     * @param fileJar
     * @param pageDirectory
     * @param commandAPI
     * @param platFormAPI
     * @return
     *
  
    private static DeployStatus deployCommand(final boolean forceDeploy, final String version, File fileJar,
            File pageDirectory, final CommandAPI commandAPI, final PlatformAPI platFormAPI) {
        // String commandName, String commandDescription, String className,
        // InputStream inputStreamJarFile, String jarName, ) throws IOException,
        // AlreadyExistsException, CreationException, CommandNotFoundException,
        // DeletionException {
        DeployStatus deployStatus = new DeployStatus();

        List<JarDependencyCommand> jarDependencies = new ArrayList<>();
        // execute the groovy scenario
        /*
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "bdm-jpql-query-executor-command-1.0.jar", pageDirectory));
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "process-starter-command-1.0.jar", pageDirectory));
         * jarDependencies.add( CmdMeteor.getInstanceJarDependencyCommand(
         * "scenario-utils-2.0.jar", pageDirectory));
         *
        // execute the meteor command
        jarDependencies.add(getInstanceJarDependencyCommand(fileJar.getName(), pageDirectory));
        jarDependencies.add(getInstanceJarDependencyCommand("bonita-event-1.9.0.jar", pageDirectory));
        jarDependencies.add(getInstanceJarDependencyCommand("bonita-properties-2.7.0.jar", pageDirectory));

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
             *
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
     *
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
     *
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
    */
     
}
