package org.bonitasoft.custompage.noonrover.executor;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult.ResultsetColumn;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.bdm.model.Query;
import org.bonitasoft.engine.bdm.model.field.FieldType;
import org.bonitasoft.engine.bdm.model.field.SimpleField;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRExecutorBdm extends NRExecutor {

    private Logger logger = Logger.getLogger(NRExecutorBdm.class.getName());

    private final static BEvent eventErrorExecution = new BEvent(NRExecutorBdm.class.getName(), 1,
            Level.APPLICATIONERROR,
            "BDM Execution", "The request has an error", "No result to display",
            "Check exception");

    public NRExecutor.ExecutorStream execute(NRExecutor.ExecutorStream executorStream) throws NRException {
        /**
         * query bdm does not allow after any ordering or filtering
         */
        executorStream.isOrderPossible = false;
        executorStream.isFilterPossible = false;

        executorStream.listData = new ArrayList<Map<String, Object>>();
        if (executorStream.selection.typeFind == TYPESELECTION.FIND) {
            // execute the query
            BusinessObject businessObject = (BusinessObject) executorStream.selection.busDefinition
                    .getObjectTransported();

            try {
                Class<?> businessObjectClassDAO = Class.forName(businessObject.getQualifiedName() + "DAOImpl");

                // Search the constructor of the implementation
                Class[] constructorTypes = new Class[1];
                constructorTypes[0] = org.bonitasoft.engine.session.APISession.class;

                Object businessObjectDAO = businessObjectClassDAO.getDeclaredConstructor(constructorTypes)
                        .newInstance(executorStream.apiSession);

                // Ok, we have now th DAO object, search the query Or the simpleField query
                Method method = null;
                Object[] parameterValues = null;
                if (executorStream.selection.getObjectTransported() instanceof Query) {
                    Query queryObject = (Query) executorStream.selection.getObjectTransported();
                    Class[] parameterTypes = new Class[queryObject.getQueryParameters().size()];
                    parameterValues = new Object[queryObject.getQueryParameters().size() + 2];
                    for (int i = 0; i < queryObject.getQueryParameters().size(); i++) {
                        parameterTypes[i] = queryObject.getQueryParameters().get(i).getClass();
                        parameterValues[i] = executorStream.selection.listParameters.get(i).value;
                    }
                    parameterTypes[queryObject.getQueryParameters().size()] = Integer.TYPE;
                    parameterTypes[queryObject.getQueryParameters().size() + 1] = Integer.TYPE;

                    parameterValues[queryObject.getQueryParameters().size()] = Integer
                            .valueOf((int) executorStream.startIndex);
                    parameterValues[queryObject.getQueryParameters().size() + 1] = Integer
                            .valueOf((int) executorStream.maxResults);

                    method = businessObjectClassDAO.getDeclaredMethod(queryObject.getName(), parameterTypes);
                }
                if (executorStream.selection.getObjectTransported() instanceof SimpleField) {
                    parameterValues = new Class[1 + 2];
                    SimpleField simpleField = (SimpleField) executorStream.selection.getObjectTransported();
                    // name of the method is 
                    // public List<Site> findByCaseId(Long caseId, int startIndex, int maxResults);
                    String name = "findBy" + getFirstCharUpperCase(simpleField.getName());
                    Class[] parameterTypes = new Class[3];
                    FieldType fieldType = simpleField.getType();
                    parameterTypes[0] = fieldType.getClazz();
                    parameterTypes[1] = Integer.TYPE;
                    parameterTypes[2] = Integer.TYPE;

                    parameterValues = new Object[3];

                    parameterValues[0] = executorStream.selection.listParameters.get(0).value;

                    parameterValues[1] = (int) executorStream.startIndex;
                    parameterValues[2] = (int) executorStream.maxResults;
                    method = businessObjectClassDAO.getDeclaredMethod(name, parameterTypes);
                }

                if (method != null) {

                    Object resultBdm = method.invoke(businessObjectDAO, parameterValues);
                    logger.info("Result " + resultBdm == null ? "null" : resultBdm.getClass().getName());
                    if (resultBdm instanceof List) {
                        for (Object oneBdm : ((List) resultBdm)) {
                            Map<String, Object> record = manageOneBdm(oneBdm, executorStream.result);
                            executorStream.listData.add(record);
                        }
                    } else {
                        Map<String, Object> record = manageOneBdm(resultBdm, executorStream.result);
                        executorStream.listData.add(record);
                    }

                } // end method is not null

            } // end catch

            catch (Exception e) {
                executorStream.listEvents.add(new BEvent(eventErrorExecution, e, ""));
            }
        }
        return executorStream;
    }

    private Map<String, Object> manageOneBdm(Object bdm, NRBusResult result) throws Exception {
        Map<String, Object> record = new HashMap<String, Object>();

        for (ResultsetColumn column : result.listColumnset) {

            if (column.isVisible || column.isSum) {
                String methodName;
                // method can be get
                // public String getNomCapteur() {
                // or 
                // public Boolean isEnService() {
                if (column.attributeDefinition.type == TYPECOLUMN.BOOLEAN) {
                    methodName = "is" + getFirstCharUpperCase(column.attributeDefinition.name);
                } else
                    methodName = "get" + getFirstCharUpperCase(column.attributeDefinition.name);
                Class[] parameterTypes = new Class[0];

                Method method = bdm.getClass().getDeclaredMethod(methodName, parameterTypes);
                Object oneData = method.invoke(bdm);
                record.put(column.attributeDefinition.name, oneData);
            }
        }
        return record;
    }

    private String getFirstCharUpperCase(String value) {
        if (value == null)
            return "";
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }
}
