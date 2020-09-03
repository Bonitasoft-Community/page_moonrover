import java.lang.management.RuntimeMXBean;
import java.lang.management.ManagementFactory;

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.logging.Logger;
import java.io.File
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.Runtime;

import org.json.simple.JSONObject;
import org.codehaus.groovy.tools.shell.CommandAlias;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;



import javax.naming.Context;
import javax.naming.InitialContext;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import javax.sql.DataSource;
import java.sql.DatabaseMetaData;
import java.sql.Clob;
import java.util.Date;

import org.apache.commons.lang3.StringEscapeUtils


import org.bonitasoft.engine.identity.User;
import org.bonitasoft.engine.search.SearchOptionsBuilder;
import org.bonitasoft.engine.search.SearchResult;
import org.bonitasoft.engine.service.TenantServiceSingleton

import org.bonitasoft.web.extension.page.PageContext;
import org.bonitasoft.web.extension.page.PageController;
import org.bonitasoft.web.extension.page.PageResourceProvider;

import org.bonitasoft.engine.exception.AlreadyExistsException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.CreationException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.bpm.process.ProcessDefinitionNotFoundException;

import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstance;
import org.bonitasoft.engine.bpm.flownode.HumanTaskInstanceSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstance;
import org.bonitasoft.engine.bpm.process.ArchivedProcessInstancesSearchDescriptor;
import org.bonitasoft.engine.bpm.process.ProcessInstance;
import org.bonitasoft.engine.bpm.process.ProcessInstanceSearchDescriptor;
import org.bonitasoft.engine.business.data.BusinessDataRepository
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.api.ProcessAPI;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.log.event.BEvent.Level;


import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource.TYPEOUTPUT
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;


public class Actions {

    private static Logger logger= Logger.getLogger("org.bonitasoft.custompage.longboard.groovy");
    
    private static BEvent eventGetSteEvents = new BEvent("com.edf.cockpitste", 1, Level.ERROR, 
            "Error during loading STE event", "Check Exception to see the cause",
            "The properties will not work (no read, no save)", "Check Exception");
    private final static BEvent eventCancelCaseError = new BEvent("com.edf.cockpitste", 2, Level.ERROR, "Erreur d'archive du case", "Une erreur est arrivée durant l'archivage du case", "Le case est toujours actif", "Vérifier l'erreur");
    private final static BEvent eventCancelCaseWithSuccess = new BEvent("com.edf.cockpitste", 3, Level.SUCCESS, "Le case est archivé", "Le cas a été archivé avec succés");
    private final static BEvent eventProcessNotFound = new BEvent("com.edf.cockpitste", 4, Level.ERROR, "Process non trouvé", "Un processus particulier est recherché, et il n'est pas déployé sur votre plateforme", "Aucun cas ne peut être crée", "Deployez le processus");
    private final static BEvent eventTransfertRexSubmited = new BEvent("com.edf.cockpitste", 5, Level.SUCCESS, "Requête REX envoyée", "Les requêtes ont été soumises au SI REX");
    private final static BEvent eventErreurRequeteREX = new BEvent("com.edf.cockpitste", 6, Level.ERROR, "Erreur lors de la soumission des requêtes", "Erreur", "Le cas n'est pas crée", "Vérifier l'erreur");
    
        
    
      // 2018-03-08T00:19:15.04Z
    public final static SimpleDateFormat sdfJson = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    public final static SimpleDateFormat sdfHuman = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* doAction */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public static Index.ActionAnswer doAction(HttpServletRequest request, String paramJsonSt, HttpServletResponse response, PageResourceProvider pageResourceProvider, PageContext pageContext) {
                
        // logger.info("#### cockpit:Actions start");
        Index.ActionAnswer actionAnswer = new Index.ActionAnswer(); 
        List<BEvent> listEvents=new ArrayList<BEvent>();
        
        try {
            String action=request.getParameter("action");
            logger.info("#### log:Actions  action is["+action+"] !");
            if (action==null || action.length()==0 )
            {
                actionAnswer.isManaged=false;
                logger.info("#### log:Actions END No Actions");
                return actionAnswer;
            }
            actionAnswer.isManaged=true;
            
            APISession apiSession = pageContext.getApiSession();
            HttpSession httpSession = request.getSession();            
            ProcessAPI processAPI = TenantAPIAccessor.getProcessAPI(apiSession);
            File pageDirectory = pageResourceProvider.getPageDirectory();

            long tenantId = apiSession.getTenantId();          
            TenantServiceAccessor tenantServiceAccessor = TenantServiceSingleton.getInstance(tenantId);             

                
            String finalAction=request.getParameter("finalaction");
            if ("init".equals(action))
            {
                // chargement des evenements
                logger.info("#### log:Actions getInit");
                
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.getInit( apiSession, ParameterSource.getInstanceFromJson(apiSession, paramJsonSt),pageResourceProvider );
            }
            // ------------------------------ getEventSte
            else if ("loadsources".equals(action))
            {
                // chargement des evenements
                logger.info("#### log:Actions load Source");
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.getSourcesJson( apiSession, ParameterSource.getInstanceFromJson(apiSession, paramJsonSt) );
            }
            // ------------------------------ bigPost
            else if ("collect_reset".equals(action))
            {
                logger.info("collect_reset json=["+paramJsonSt+"]");
                httpSession.setAttribute("accumulate", paramJsonSt );
                actionAnswer.responseMap.put("status", "ok");
            }
            else if ("collect_add".equals(action))
            {
                        
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("collect_add json=["+paramJsonSt+"] previous["+accumulateJson+"]");
                accumulateJson+=paramJsonSt;
                httpSession.setAttribute("accumulate", accumulateJson );
                actionAnswer.responseMap.put("status", "ok");

            } 
            
            // ------------------------------ executeRequest
            // Not a else : it may be a action == collect + finalAction==executeRequest
            if ("executeRequest".equals(finalAction) ) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log:Actions executeRequest on json["+accumulateJson+"]");
                
                httpSession.setAttribute("request", accumulateJson);

                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();                
                actionAnswer.responseMap = NoonRoverAccessAPI.executeRequest( ParameterSource.getInstanceFromJson(apiSession, accumulateJson), response );
                
            }
            if ("updaterecordbdm".equals(finalAction)) {
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log:Actions updaterecord_22 on json["+accumulateJson+"]");
                
                httpSession.setAttribute("request", accumulateJson);

                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                
                actionAnswer.responseMap = NoonRoverAccessAPI.updateRecordBdm( ParameterSource.getInstanceFromJson(apiSession, accumulateJson), 
                        pageDirectory,
                        tenantId,
                        tenantServiceAccessor );
                
            }
            if ("excelRequest".equals(action) ) {
                String accumulateJson = (String) httpSession.getAttribute("request" );
                logger.info("#### log:Actions excel on json["+accumulateJson+"]");
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                ParameterSource parameterSource=ParameterSource.getInstanceFromJson(apiSession, accumulateJson);
                parameterSource.typeOutput = TYPEOUTPUT.CSV;
                parameterSource.startIndex=0;
                parameterSource.maxResults= (long) Index.getIntegerParameter( request, "maxCsvValue", 10000);
                actionAnswer.responseMap = NoonRoverAccessAPI.executeRequest( parameterSource, response );
                
            }
            // use the bigpost for saveRequest
            if ("saveRequest".equals(finalAction) ) { 
                String accumulateJson = (String) httpSession.getAttribute("accumulate" );
                logger.info("#### log:Actions saveRequest on json["+accumulateJson+"]");
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.saveRequest( ParameterSource.getInstanceFromJson(apiSession, accumulateJson), pageResourceProvider );             
            }
            if ("loadRequest".equals(action))
            {
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.loadRequest( ParameterSource.getInstanceFromJson(apiSession, paramJsonSt), pageResourceProvider );                
            }
            if ("deleteRequest".equals(action))
            {
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.deleteRequest( ParameterSource.getInstanceFromJson(apiSession, paramJsonSt), pageResourceProvider );              
            }
                    
            if ("listRequests".equals(action))
            {
                NoonRoverAccessAPI NoonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
                actionAnswer.responseMap = NoonRoverAccessAPI.listRequests( ParameterSource.getInstanceFromJson(apiSession, paramJsonSt), pageResourceProvider );
            
            }
             
            // actionAnswer.responseMap.put("listevents",BEventFactory.getHtml( listEvents));
                
            
            logger.info("#### log:Actions END responseMap ="+actionAnswer.responseMap.size());
            return actionAnswer;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();
            logger.severe("#### log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            actionAnswer.isResponseMap=true;
            actionAnswer.responseMap.put("Error", "log:Groovy Exception ["+e.toString()+"] at "+exceptionDetails);
            

            
            return actionAnswer;
        }
    }

    
    
    
    
}
