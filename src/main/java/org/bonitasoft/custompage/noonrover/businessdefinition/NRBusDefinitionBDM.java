package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;



public class NRBusDefinitionBDM extends NRBusDefinition {

    public static Logger logger = Logger.getLogger(NRBusDefinitionBDM.class.getName());

    private static String logHeader = "NoonRover.cmd";

    
    public static BEvent eventAPIError = new BEvent(NRBusDefinitionBDM.class.getName(), 6, Level.ERROR,
            "API Error", "Check the error", "Connection is lost", "Reconnect then check the error");
    
      
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
        List<BEvent> listEvents= new ArrayList<>();
        try
        {
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(parameterSource.apiSession);
            PlatformAPI platformAPI = null;
            NRBusCommandAPI nrCommandAPI = new NRBusCommandAPI(pageDirectory, commandAPI, platformAPI, tenantId);
            
            BusinessObject businessObject = (BusinessObject) parameterSource.businessDefinition.getObjectTransported();

            listEvents.addAll( nrCommandAPI.callUpdate(businessObject, parameterSource.listRecords));
        }
        catch( Exception e)
        {
            listEvents.add( new BEvent(eventAPIError, e,""));
        }
        Map<String, Object> result = new HashMap<>();
        result.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml( listEvents));
        return result;
        
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

   

   
}
