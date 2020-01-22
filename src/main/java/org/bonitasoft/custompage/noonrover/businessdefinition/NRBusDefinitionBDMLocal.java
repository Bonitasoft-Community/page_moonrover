package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;

import org.bonitasoft.engine.bdm.Entity;
import org.bonitasoft.engine.business.data.BusinessDataRepository;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.transaction.UserTransactionService;

public class NRBusDefinitionBDMLocal {
    
    Logger logger = Logger.getLogger("org.bonitasoft.custompage.noonrover.businessdefinition");

    private static String logHeader = "NoonRover.cmd";

    
    @SuppressWarnings("unchecked")
   
        private class PersistBdm implements Callable {

            BusinessDataRepository businessDataRepository;
            Entity entity;
            Exception exception = null;

            public PersistBdm(BusinessDataRepository businessDataRepository, Entity entity) {
                this.businessDataRepository = businessDataRepository;
                this.entity = entity;

            }

            @Override
            public Object call() throws Exception {
                try {
                    businessDataRepository.persist(entity);
                } catch (Exception e) {
                    this.exception = e;
                }
                return null;

            }

        }

    public Map<String, Object> updateRecordBDMLocal(ParameterSource parameterSource,
            TenantServiceAccessor tenantServiceAccessor) {
        BusinessDataRepository businessDataRepository = tenantServiceAccessor.getBusinessDataRepository();
        UserTransactionService userTransactionService = tenantServiceAccessor.getUserTransactionService();

        Entity entity = null;
        try {
            // a persistenceId ? Read it
            Long persistenceID = 1L;
            Class<?> classBdmDAO = Class.forName(parameterSource.businessDefinition.getName() + "DAOImpl");
            Constructor<?> constructorBdmDao = classBdmDAO.getConstructor(APISession.class);
            Object bdmDAO = constructorBdmDao.newInstance(parameterSource.apiSession);
            if (persistenceID != null) {
                Method method = classBdmDAO.getMethod("findByPersistenceId", Long.class);
                Object entityObj = method.invoke(bdmDAO, persistenceID);
                entity = (Entity) entityObj;
            } else {
                Method method = classBdmDAO.getMethod("newInstance");
                Object entityObj = method.invoke(bdmDAO);
                entity = (Entity) entityObj;

            }

            // update the object now

            // you know what ? The BDM item is not an object, but just a list of private field. No way to getAttributes() or setAttributes(), so...
            // the only way if to find the method for each value, and to call it.. what a nice conception, isn't ? 
            Map<String, Object> record = new HashMap<>();

            record.put("label", "Change Penalty_2");

            Method[] methodsEntity = entity.getClass().getMethods();
            for (String attribut : record.keySet()) {
                boolean success = false;
                // look for a method "set<Attribut>"
                for (Method method : methodsEntity) {
                    if (method.getName().equalsIgnoreCase("set" + attribut)) {
                        // we get it !
                        method.invoke(entity, record.get(attribut));
                        success = true;
                    }
                }
            }

            // now we can persist it

            PersistBdm persistBdm = new PersistBdm(businessDataRepository, entity);
            userTransactionService.executeInTransaction(persistBdm);
            if (persistBdm.exception != null)
                logger.severe("erreur " + persistBdm.exception.getMessage());
        } catch (Exception e) {
            logger.severe("erreur " + e.getMessage());
        }

        return new HashMap<>();
    }
}
