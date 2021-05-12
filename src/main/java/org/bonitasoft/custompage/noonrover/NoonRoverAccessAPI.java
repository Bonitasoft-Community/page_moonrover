package org.bonitasoft.custompage.noonrover;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusCommandAPI;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionBDM;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.custompage.noonrover.executor.NRStream;
import org.bonitasoft.custompage.noonrover.executor.NRStreamServletResponse;
import org.bonitasoft.custompage.noonrover.librairy.RequestFactory;
import org.bonitasoft.custompage.noonrover.librairy.RequestFactory.LibraryResult;
import org.bonitasoft.custompage.noonrover.source.NRSource;
import org.bonitasoft.custompage.noonrover.source.NRSourceFactory;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.engine.api.CommandAPI;
import org.bonitasoft.engine.api.PlatformAPI;
import org.bonitasoft.engine.api.ProfileAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.profile.Profile;
import org.bonitasoft.engine.profile.ProfileCriterion;
import org.bonitasoft.engine.service.TenantServiceAccessor;
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

    public static String cstJsonListEvents = "listevents";

    public static class ParameterSource {

        public String jsonSt;
        public NRBusDefinition businessDefinition;
        public List<BEvent> listEvents = new ArrayList<>();
        public Long startIndex;
        public Long maxResults;
        public Long nbRecords;
        public APISession apiSession;

        public String name;

        public long maxExportRecordsPerEntity;
        public List<String> entitiesToExport;

        // source can borrow a record
        public List<Map<String, Object>> listRecords;

        public enum TYPEOUTPUT {
            NORMAL, CSV
        };

        public TYPEOUTPUT typeOutput = TYPEOUTPUT.NORMAL;

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

            try {
                parameterSource.maxExportRecordsPerEntity = NRToolbox.getJsonLong(false, "maxRecordsPerEntity", jsonHash, "/");
                parameterSource.entitiesToExport = (List<String>) jsonHash.get("entities");;
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
            NRSource.SourceStatus sourceStatus = source.loadBusinessDefinition(apiSession,
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
        NRStream executorStream = new NRStreamServletResponse(parameterSource, response);

        if (BEventFactory.isError(parameterSource.listEvents)) {
            executorStream.listEvents = parameterSource.listEvents;
            return executorStream.getJson();
        }

        executorStream = parameterSource.businessDefinition.execute(parameterSource, executorStream);
        if (executorStream.isJsonResult())
            return executorStream.getJson();
        else
            return new HashMap<>();
    }

    /**
     * @param parameterSource
     * @param pageDirectory
     * @param tenantId
     * @param tenantServiceAccessor
     * @return
     */
    public Map<String, Object> updateRecordBdm(final ParameterSource parameterSource, File pageDirectory,
            long tenantId, TenantServiceAccessor tenantServiceAccessor) {

        NRBusDefinition businessDefinition = parameterSource.businessDefinition;
        Map<String, Object> result = null;
        if (businessDefinition instanceof NRBusDefinitionBDM) {
            result = ((NRBusDefinitionBDM) businessDefinition).updateRecordBDM(parameterSource, pageDirectory,
                    tenantId, tenantServiceAccessor);
        } else
            result = new HashMap<>();
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

    /**
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
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

    /**
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
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

    /**
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
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

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Export */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public Map<String, Object> launchExportData(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider,
            File pageDirectory,
            long tenantId,
            TenantServiceAccessor tenantServiceAccessor) {
        Map<String, Object> result = new HashMap<>();

        List<BEvent> listEvents = new ArrayList<>();
        try {
            CommandAPI commandAPI = TenantAPIAccessor.getCommandAPI(parametersSource.apiSession);
            PlatformAPI platformAPI = null;
            NRBusCommandAPI nrCommandAPI = new NRBusCommandAPI(pageDirectory, commandAPI, platformAPI, tenantId);

            listEvents.addAll(nrCommandAPI.launchExport(parametersSource.name,
                    parametersSource.entitiesToExport,
                    parametersSource.maxExportRecordsPerEntity,
                    parametersSource.apiSession,
                    getDirectoryExport()));
            Map<String, Object> listExport = refreshExportData(parametersSource, pageResourceProvider, pageDirectory, tenantId, tenantServiceAccessor);
            result.putAll(listExport);

        } catch (Exception e) {
            listEvents.add(new BEvent(NRBusDefinitionBDM.eventAPIError, e, ""));
        }
        result.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
        return result;
    }

    /**
     * @param parametersSource
     * @param pageResourceProvider
     * @param pageDirectory
     * @param tenantId
     * @param tenantServiceAccessor
     * @return
     */
    public Map<String, Object> refreshExportData(final ParameterSource parametersSource,
            PageResourceProvider pageResourceProvider,
            File pageDirectory,
            long tenantId,
            TenantServiceAccessor tenantServiceAccessor) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        List<BEvent> listEvents = new ArrayList<>();
        Map<String, Object> result = new HashMap<>();
        try {
            List<Map<String, Object>> listExportFiles = new ArrayList();
            result.put("exportfiles", listExportFiles);
            Long oneDayBefore = System.currentTimeMillis() - 1000 * 60 * 60 * 24;

            File folderExport = new File(getDirectoryExport());
            for (final File f : folderExport.listFiles()) {
                Map<String, Object> oneFile = new HashMap<>();

                oneFile.put("filename", f.getName());
                BasicFileAttributes attr = Files.readAttributes(f.toPath(), BasicFileAttributes.class);
                oneFile.put("datefile", sdf.format(new Date(attr.creationTime().toMillis())));

                // more than one day? Purge it
                if (attr.creationTime().toMillis() < oneDayBefore) {
                    Files.delete(Paths.get(f.getName()));
                    continue;
                }

                if (f.isFile() && f.getName().endsWith(".zip")) {
                    oneFile.put("status", "ready");
                    listExportFiles.add(oneFile);
                }
                if (f.isFile() && f.getName().endsWith(".zipinprogress")) {
                    oneFile.put("status", "inprogress");
                    listExportFiles.add(oneFile);
                }
            }

        } catch (Exception e) {
            listEvents.add(new BEvent(NRBusDefinitionBDM.eventAPIError, e, ""));
        }
        result.put(NoonRoverAccessAPI.cstJsonListEvents, BEventFactory.getHtml(listEvents));
        return result;
    }

    /**
     * @param output
     * @param name
     * @param pageDirectory
     *        * exportDataFile( output, fileName, pageDirectory );
     */
    public void exportDataFile(OutputStream output, String name, File pageDirectory) {
        File fileToExport = new File(getDirectoryExport() + "/" + name);
        try {
            FileInputStream inputStream = new FileInputStream(fileToExport);

            byte[] bytes = new byte[2048];
            int read;
            while ((read = inputStream.read(bytes)) != -1) {
                output.write(bytes, 0, read);
            }
        } catch (Exception e) {
            logger.severe("moonRover.exportDataFile :  exception " + e.getMessage());
        }
    }

    /**
     * @return
     */
    private String getDirectoryExport() {
        String tempDirectory = System.getProperty("java.io.tmpdir");

        Path path = Paths.get(tempDirectory + "/noonrover/export");

        try {
            Files.createDirectories(path);
            return path.toString();

        } catch (IOException e) {
            return tempDirectory;
        }

    }
}
