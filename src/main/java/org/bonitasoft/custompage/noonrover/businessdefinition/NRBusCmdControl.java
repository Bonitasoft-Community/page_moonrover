package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.business.data.BusinessDataRepository;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.transaction.UserTransactionService;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/* ******************************************************************************** */
/*                                                                                  */
/* Command Control */
/*
 * Only a command can update the BDM :-(
 * So the goal of this command is just to update it
 */

public class NRBusCmdControl extends TenantCommand {

    static Logger logger = Logger.getLogger(NRBusCmdControl.class.getName());

    static String logHeader = "NRBusCmdControl ~~~";

    private static BEvent eventInternalError = new BEvent(NRBusCmdControl.class.getName(), 1, Level.ERROR,
            "Internal error", "Internal error, check the log");
    private static BEvent eventTourRemoved = new BEvent(NRBusCmdControl.class.getName(), 2, Level.SUCCESS,
            "Tour removed", "Tour is removed with success");

    private static BEvent eventTourStarted = new BEvent(NRBusCmdControl.class.getName(), 4, Level.SUCCESS,
            "Tour started", "The Tour is now started");
    private static BEvent eventTourStopped = new BEvent(NRBusCmdControl.class.getName(), 5, Level.SUCCESS,
            "Tour stopped", "The Tour is now stopped");

    private static BEvent eventTourUpdated = new BEvent(NRBusCmdControl.class.getName(), 6, Level.SUCCESS,
            "Tour updated", "The Tour is now updated");

    private static BEvent eventTourRegister = new BEvent(NRBusCmdControl.class.getName(), 8, Level.SUCCESS,
            "Tour registered", "The Tour is now registered");

    private static BEvent eventPlugInViolation = new BEvent(NRBusCmdControl.class.getName(), 9, Level.ERROR,
            "Plug in violation",
            "A plug in must return a status on each execution. The plug in does not respect the contract",
            "No report is saved", "Contact the plug in creator");

    private static BEvent eventPlugInError = new BEvent(NRBusCmdControl.class.getName(), 10, Level.ERROR,
            "Plug in error", "A plug in throw an error", "No report is saved", "Contact the plug in creator");

    private static BEvent eventSchedulerResetSuccess = new BEvent(NRBusCmdControl.class.getName(), 13, Level.SUCCESS,
            "Schedule reset", "The schedule is reset with success");

    private String cstDateFormat = "yyyy-MM-dd'T'HH:mm:SS";
    /* ******************************************************************************** */
    /*                                                                                  */
    /* the companion MilkCmdControlAPI call this API */
    /*                                                                                  */
    /* ******************************************************************************** */

    /**
     * this constant is defined too in MilkQuartzJob to have an independent JAR
     */
    public static String cstCommandName = "NoonRoverCmd";
    public static String cstCommandDescription = "Execute BDM Update";

    /**
     * this constant is defined too in MilkQuartzJob to have an independent JAR
     */
    public static String cstVerb = "verb";
    /**
     * this constant is defined too in MilkQuartzJob to have an independent JAR
     */

    public static String cstTenantId = "tenantid";
    public static String cstBusinessName = "businessName";
    public static String cstApiSession = "apiSession";
    public static String cstRecord = "record";
    public static String cstStatus = "status";
    public static String cstStatus_V_OKUPDATE = "OKUPDATE";
    public static String cstStatus_V_OKINSERT = "OKINSERT";

    /**
     * each call, the command create a new object !
     * The singleton is then use, and decision is take that the method is responsible to save all change
     */
    public Serializable execute(Map<String, Serializable> parameters, TenantServiceAccessor serviceAccessor)
            throws SCommandParameterizationException, SCommandExecutionException {

        String businessName = (String) parameters.get(cstBusinessName);
        APISession apiSession = (APISession) parameters.get(cstApiSession);
        Map<String, Object> record = (Map<String, Object>) parameters.get(cstRecord);
        SimpleDateFormat sdf = new SimpleDateFormat(cstDateFormat);
        BusinessDataRepository businessDataRepository = serviceAccessor.getBusinessDataRepository();
        UserTransactionService userTransactionService = serviceAccessor.getUserTransactionService();

        HashMap<String, Object> result = new HashMap<String, Object>();
        Entity entity = null;
        String finalStatus = "";
        try {
            // a persistenceId ? Read it
            Long persistenceID = null;
            try {
                persistenceID = (Long) record.get("PersistenceId");
            } catch (Exception e) {
            } ;

            Class<?> classBdm = Class.forName(businessName);

            if (persistenceID != null) {
                Object entityObj = businessDataRepository.findById((Class) classBdm, persistenceID);
                entity = (Entity) entityObj;
                result.put(cstStatus, cstStatus_V_OKUPDATE);

            } else {
                entity = (Entity) classBdm.newInstance();
                result.put(cstStatus, cstStatus_V_OKINSERT);
            }

            // update the object now

            // you know what ? The BDM item is not an object, but just a list of private field. No way to getAttributes() or setAttributes(), so...
            // the only way if to find the method for each value, and to call it.. what a nice conception, isn't ? 

            // record.put("label", "Change Penalty_2");
            // http://embed.plnkr.co/jwNjrXCjMRLk5Jg79nTw/preview
            boolean success = true;
            if (record != null) {
                Method[] methodsEntity = entity.getClass().getMethods();
                for (String attribut : record.keySet()) {
                    // look for a method "set<Attribut>"
                    for (Method method : methodsEntity) {
                        String methodTrace = null;

                        try {
                            if (method.getName().equalsIgnoreCase("set" + attribut)) {
                                methodTrace = "Attribut[" + attribut + "] value[" + record.get(attribut) + "]";

                                if (record.get(attribut) == null) {
                                    method.invoke(entity, record.get(attribut));
                                    continue;
                                }
                                // we get it !
                                Parameter parameterMethod = (method.getParameters().length > 0
                                        ? method.getParameters()[0] : null);
                                methodTrace += "(" + record.get(attribut).getClass().getName() + ") - parameter("
                                        + (parameterMethod == null ? "null" : parameterMethod.toString()) + ")";
                                if (parameterMethod.getType() == OffsetDateTime.class) {
                                    // change the type
                                    OffsetDateTime valueOffset = OffsetDateTime.parse(record.get(attribut).toString(),
                                            DateTimeFormatter.ISO_ZONED_DATE_TIME);
                                    method.invoke(entity, valueOffset);
                                } else if (parameterMethod.getType() == Date.class) {
                                    Date valueDate = sdf.parse(record.get(attribut).toString());
                                    method.invoke(entity, valueDate);
                                } else if (parameterMethod.getType() == LocalDateTime.class) {
                                    LocalDateTime valueOffset = LocalDateTime.parse(record.get(attribut).toString(),
                                            DateTimeFormatter.ISO_ZONED_DATE_TIME);
                                    method.invoke(entity, valueOffset);
                                } else if (parameterMethod.getType() == LocalDate.class) {
                                    LocalDate valueOffset = LocalDate.parse(record.get(attribut).toString(), DateTimeFormatter.ISO_LOCAL_DATE);
                                    method.invoke(entity, valueOffset);
                                
                                } else if (parameterMethod.getType() == Boolean.class) {
                                    method.invoke(entity, Boolean.valueOf(record.get(attribut).toString()));

                                } else if (parameterMethod.getType() == Integer.class) {
                                    method.invoke(entity, Integer.valueOf(record.get(attribut).toString()));

                                } else if (parameterMethod.getType() == Float.class) {
                                    method.invoke(entity, Float.valueOf(record.get(attribut).toString()));
                                } else if (parameterMethod.getType() == Double.class) {
                                    method.invoke(entity, Double.valueOf(record.get(attribut).toString()));

                                } else
                                    method.invoke(entity, record.get(attribut));

                            }
                        } catch (Exception e) {
                            logger.severe("NRBusCmdControl: " + e.getMessage());
                            success = false;
                            finalStatus += methodTrace + " : " + e.getMessage() + ";";

                        }

                    }
                }
            }

            // now we can persist it
            if (success)
                businessDataRepository.persist(entity);
            if (finalStatus.length() > 0)
                result.put(cstStatus, "Can't update[" + businessName + "] [" + finalStatus + "]");

        } catch (Exception e) {
            logger.severe("NRBusCmdControl: " + e.getMessage());
            result.put(cstStatus, "Can't update[" + businessName + "] [" + finalStatus + "]: " + e.getMessage());
        }

        logger.info("End command ");

        return result;
    }
}
