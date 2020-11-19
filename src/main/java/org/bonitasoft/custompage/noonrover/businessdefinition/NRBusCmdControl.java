package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.bonitasoft.command.BonitaCommandApiAccessor;
import org.bonitasoft.command.BonitaCommand.ExecuteAnswer;
import org.bonitasoft.command.BonitaCommand.ExecuteParameters;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.business.data.BusinessDataRepository;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.transaction.UserTransactionService;

/* ******************************************************************************** */
/*                                                                                  */
/* Command Control */
/*
 * Only a command can update the BDM :-(
 * So the goal of this command is just to update it
 */

public class NRBusCmdControl extends BonitaCommandApiAccessor {

    static Logger logger = Logger.getLogger(NRBusCmdControl.class.getName());

    static String logHeader = "NRBusCmdControl ~~~";

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

    public final static String CST_TENANTID = "tenantid";
    public final static String CST_BUSINESSNAME = "businessName";
    public final static String CST_RECORD = "record";
    public final static String CST_STATUS = "status";
    public final static String CST_STATUS_V_OKUPDATE = "OKUPDATE";
    public final static String CST_STATUS_V_OKINSERT = "OKINSERT";

    @Override
    public String getName() {
        return cstCommandName;
    }

    @Override
    public ExecuteAnswer afterDeployment(ExecuteParameters executeParameters, APIAccessor apiAccessor, TenantServiceAccessor tenantServiceAccessor) {
        return returnOkAnswer();
    }

    /**
     * each call, the command create a new object !
     * The singleton is then use, and decision is take that the method is responsible to save all change
     */
    @SuppressWarnings("unchecked")
    @Override
    public ExecuteAnswer executeCommandApiAccessor(ExecuteParameters executeParameters, APIAccessor apiAccessor, TenantServiceAccessor tenantServiceAccessor) {
        final String businessName = (String) executeParameters.parametersCommand.get(CST_BUSINESSNAME);
        Map<String, Object> record = (Map<String, Object>) executeParameters.parametersCommand.get("record");
        BusinessDataRepository businessDataRepository = tenantServiceAccessor.getBusinessDataRepository();
        UserTransactionService userTransactionService = tenantServiceAccessor.getUserTransactionService();
        Long persistenceID;
        TransactionBDM transactionBdm = new TransactionBDM();

        
        // a persistenceId ? Read it
        try {
            persistenceID = (Long) record.get("PersistenceId");
        } catch (Exception e) {
            persistenceID = null;
        }
        transactionBdm.businessName = businessName;
        transactionBdm.persistenceID = persistenceID;
        transactionBdm.businessDataRepository = businessDataRepository;
        transactionBdm.record = record;
        try {
            userTransactionService.executeInTransaction(transactionBdm);
        } catch (Exception e) {
            logger.severe("NRBusCmdControl: " + e.getMessage());
            transactionBdm.executeAnswer.result.put(CST_STATUS, "Can't update[" + businessName + "]: " + e.getMessage());
        }
        logger.info("End command ");

        return transactionBdm.executeAnswer;
    }

    /**
     * @author Firstname Lastname
     */
    public class TransactionBDM implements Callable<String> {

        public String businessName;
        public Long persistenceID;
        public BusinessDataRepository businessDataRepository;
        public Map<String, Object> record;

        public ExecuteAnswer executeAnswer = new ExecuteAnswer();

        /**
         * call function
         */
        public String call() {
            StringBuffer statusTransaction = new StringBuffer();
            try {

                SimpleDateFormat sdf = new SimpleDateFormat(cstDateFormat);

                Class<?> classBdm = Class.forName(businessName);
                Entity entity = null;

                if (persistenceID != null) {
                    Object entityObj = businessDataRepository.findById((Class) classBdm, persistenceID);
                    entity = (Entity) entityObj;
                    executeAnswer.result.put(CST_STATUS, CST_STATUS_V_OKUPDATE);

                } else {
                    entity = (Entity) classBdm.newInstance();
                    executeAnswer.result.put(CST_STATUS, CST_STATUS_V_OKINSERT);
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
                                statusTransaction.append(methodTrace + " : " + e.getMessage() + ";");

                            }

                        } // end method
                    } // end attribut
                } // end record !=null

                // now we can persist it
                if (success)
                    businessDataRepository.persist(entity);
            } catch (Exception e) {
                logger.severe("NRBusCmdControl: " + e.getMessage());
                statusTransaction.append(e.getMessage());
                executeAnswer.result.put(CST_STATUS, "Can't update[" + businessName + "] [" + statusTransaction.toString() + "]: " + e.getMessage());
            }
            if (statusTransaction.length() > 0)
                executeAnswer.result.put(CST_STATUS, "Can't update[" + businessName + "] [" + statusTransaction.toString() + "]");
            return statusTransaction.toString();
        }
    }

}
