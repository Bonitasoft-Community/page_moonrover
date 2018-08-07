package org.bonitasoft.custompage.noonrover;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionBDM;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.custompage.noonrover.executor.NRExecutor;
import org.bonitasoft.custompage.noonrover.librairy.RequestFactory;
import org.bonitasoft.custompage.noonrover.librairy.RequestFactory.LibraryResult;
import org.bonitasoft.custompage.noonrover.source.NRSource;
import org.bonitasoft.custompage.noonrover.source.NRSourceFactory;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.business.data.BusinessDataRepository;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileCriterion;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.TenantServiceSingleton;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.web.extension.page.PageResourceProvider;
import org.json.simple.JSONValue;

public class NoonRoverAccessAPI {

    public static Logger logger = Logger.getLogger(NoonRoverAccessAPI.class.getName());

    private final static BEvent eventErrorJsonParsing = new BEvent(NoonRoverAccessAPI.class.getName(), 1, Level.ERROR,
            "Json Parsing error", "Parameters are in error", "No result to display",
            "Check exception");

    private static NRBusDefinitionFactory businessDefinitionFactory = NRBusDefinitionFactory.getInstance();

    public static String cstJsonStartIndex = "startindex";
    public static String cstJsonMaxResults = "maxresults";
    public static String cstJsonNbRecords = "nbrecords";

    public static String cstJsonListEvents="listevents";
    
    public static class ParameterSource {

        public String jsonSt;
        public NRBusDefinition businessDefinition;
        public List<BEvent> listEvents = new ArrayList<BEvent>();
        public Long startIndex;
        public Long maxResults;
        public Long nbRecords;
        public APISession apiSession;

        public String name;

        
        // source can borrow a record
        public List<Map<String,Object>> listRecords;
        
        public enum TYPEOUTPUT {
            NORMAL, CSV
        };

        public TYPEOUTPUT typeOutput = TYPEOUTPUT.NORMAL;;

        public static ParameterSource getInstanceFromJson(APISession apiSession, String jsonSt) {
            ParameterSource parameterSource = new ParameterSource();
            parameterSource.apiSession = apiSession;
            parameterSource.jsonSt = jsonSt;
            if (parameterSource.jsonSt == null)
                return parameterSource;

            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(parameterSource.jsonSt);
            if (jsonHash == null) {
                // error during parsing
                parameterSource.listEvents
                        .add(new BEvent(eventErrorJsonParsing, "Json[" + parameterSource.jsonSt + "]"));
                return parameterSource;
            }
            Map<String, Object> request = (Map<String, Object>) jsonHash.get("request");
            if (request != null) {
                // decode the request
                try {
                    parameterSource.businessDefinition = businessDefinitionFactory.fromJson(apiSession, request);
                    parameterSource.startIndex = NRToolbox.getJsonLong(false, cstJsonStartIndex, request, "request");
                    parameterSource.maxResults = NRToolbox.getJsonLong(false, cstJsonMaxResults, request, "request");
                    parameterSource.nbRecords = NRToolbox.getJsonLong(false, cstJsonNbRecords, request, "request");
                } catch (NRException e) {
                    parameterSource.listEvents.addAll(e.listEvents);
                }
            }

            try {
                parameterSource.name = NRToolbox.getJsonSt(false, "name", jsonHash, "/");
            } catch (Exception e) {
                // name may not be present
            }
            try {
                parameterSource.typeOutput = TYPEOUTPUT.NORMAL;
                parameterSource.typeOutput = TYPEOUTPUT.valueOf(NRToolbox.getJsonSt(false, "output", jsonHash, "/"));
            } catch (Exception e) {
                // name may not be present
            }

            try {
                parameterSource.listRecords = (List) jsonHash.get("listrecords");
            } catch (Exception e) {
                // name may not be present
            }
            return parameterSource;

        }
    }

    public static NoonRoverAccessAPI getInstance() {
        return new NoonRoverAccessAPI();
    }

    public Map<String, Object> getInit(APISession apiSession, final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {
        NRSource.SourceStatus sourceStatus = getSources(apiSession, parametersSource);
        LibraryResult libraryResult = getListRequests(parametersSource, pageResourceProvider);
        // merge the two record

        sourceStatus.listEvents.addAll(libraryResult.listEvents);
        Map<String, Object> record = sourceStatus.getJson();
        record.putAll(libraryResult.getJson());
        record.put(cstJsonListEvents, sourceStatus.listEvents);

        record.put("isAdmin", false);
        try {
            ProfileAPI profileAPI = TenantAPIAccessor.getProfileAPI(apiSession);
            for (Profile profile : profileAPI.getProfilesForUser(apiSession.getUserId(), 0, 1000,
                    ProfileCriterion.NAME_ASC)) {
                if (profile.getName().equals("Administrator"))
                    record.put("isadmin", true);
            }
        } catch (Exception e) {
        }
        return record;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getSource definition */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public Map<String, Object> getSourcesJson(APISession apiSession, final ParameterSource parametersSource) {
        return getSources(apiSession, parametersSource).getJson();
    }

    public NRSource.SourceStatus getSources(APISession apiSession, final ParameterSource parametersSource) {

        logger.info("NoonRooverAPI:getSource");

        NRSourceFactory sourceFactory = NRSourceFactory.getInstance();

        businessDefinitionFactory.referenceSources(apiSession, sourceFactory);

        NRSource.SourceStatus globalSourceStatus = new NRSource.SourceStatus();
        for (NRSource source : sourceFactory.getSources()) {
            NRSource.SourceStatus sourceStatus = source.getListBusinessDefinition(apiSession,
                    businessDefinitionFactory);
            globalSourceStatus.add(sourceStatus);

            /*
             * NRSource dataModel = new NRSourceDatabase();
             * SourceStatus sourceStatus = dataModel.readDatabase(businessDefinitionFactory);
             * logger.info("List sources size= " + sourceStatus.listBusinessDefinition.size());
             */
        }
        return globalSourceStatus;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* executeRequest definition */
    /*
     * If the execute send back the result directly in the HttpResponse, then then the return value must be EMPTY
     */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> executeRequest(final ParameterSource parameterSource, HttpServletResponse response) {

        logger.info("NoonRooverAPI:ExecuteRequest");
        NRExecutor.ExecutorStream executorStream = new NRExecutor.ExecutorStream(parameterSource);
        executorStream.response = response;
        if (BEventFactory.isError(parameterSource.listEvents)) {
            executorStream.listEvents = parameterSource.listEvents;
            return executorStream.getJson();
        }

        executorStream = parameterSource.businessDefinition.execute(parameterSource, executorStream);
        if (executorStream.isJsonResult())
            return executorStream.getJson();
        else
            return new HashMap<String, Object>();
    }

    
    public Map<String, Object> updateRecordBdm(final ParameterSource parameterSource,  File pageDirectory,
            long tenantId,TenantServiceAccessor tenantServiceAccessor) {
        
        
        NRBusDefinition businessDefinition=  parameterSource.businessDefinition;
        Map<String,Object> result=null;
        if (businessDefinition instanceof NRBusDefinitionBDM)
        {
           result= ((NRBusDefinitionBDM)businessDefinition).updateRecordBDM(parameterSource,  pageDirectory,
                    tenantId,tenantServiceAccessor );
        }
        else
            result= new HashMap<String,Object>();
        return result;
            
    }

        
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Request operation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    /**
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
    public Map<String, Object> saveRequest(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {
        logger.info("NoonRooverAPI:SaveRequest");
        RequestFactory requestFactory = RequestFactory.getInstance();
        if (BEventFactory.isError(parametersSource.listEvents)) {
            // get the list
            LibraryResult libraryResult = requestFactory.list(parametersSource, pageResourceProvider);
            libraryResult.listEvents.addAll(parametersSource.listEvents);

            return libraryResult.getJson();
        }
        LibraryResult libraryResult = requestFactory.save(parametersSource, pageResourceProvider);

        return libraryResult.getJson();
    }

    /**
     * return the list of Request
     * 
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
    public Map<String, Object> getListRequestsJson(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {

        return getListRequests(parametersSource, pageResourceProvider).getJson();
    }

    public LibraryResult getListRequests(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {
        logger.info("NoonRooverAPI:listRequests");
        if (BEventFactory.isError(parametersSource.listEvents)) {
            LibraryResult libraryResult = new LibraryResult();
            libraryResult.listEvents = parametersSource.listEvents;
            return libraryResult;
        }
        RequestFactory requestFactory = RequestFactory.getInstance();
        LibraryResult libraryResult = requestFactory.list(parametersSource, pageResourceProvider);

        return libraryResult;
    }

    public Map<String, Object> deleteRequest(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {
        logger.info("NoonRooverAPI:DeleteRequest");
        RequestFactory requestFactory = RequestFactory.getInstance();
        if (BEventFactory.isError(parametersSource.listEvents)) {
            // get the list
            LibraryResult libraryResult = requestFactory.list(parametersSource, pageResourceProvider);
            libraryResult.listEvents.addAll(parametersSource.listEvents);

            return libraryResult.getJson();
        }
        LibraryResult libraryResult = requestFactory.delete(parametersSource, pageResourceProvider);

        return libraryResult.getJson();
    }

    public Map<String, Object> loadRequest(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider) {
        logger.info("NoonRooverAPI:LoadRequest");
        RequestFactory requestFactory = RequestFactory.getInstance();
        if (BEventFactory.isError(parametersSource.listEvents)) {
            // get the list
            LibraryResult libraryResult = requestFactory.list(parametersSource, pageResourceProvider);
            libraryResult.listEvents.addAll(parametersSource.listEvents);

            return libraryResult.getJson();
        }
        LibraryResult libraryResult = requestFactory.load(parametersSource, pageResourceProvider);

        return libraryResult.getJson();
    }

}
