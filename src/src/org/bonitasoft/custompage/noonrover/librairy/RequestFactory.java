package org.bonitasoft.custompage.noonrover.librairy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;
import org.bonitasoft.ext.properties.BonitaProperties;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;
import org.bonitasoft.web.extension.page.PageResourceProvider;
import org.json.simple.JSONValue;

/**
 * This class is used to manupulate the library request : save, load, list of request, delete
 * In fact, the request are kept in a JSON format , because we don't really need to understand it, just to save/ load
 * 
 * @author Firstname Lastname
 */
public class RequestFactory {

    private final static BEvent eventRequestParseError = new BEvent(RequestFactory.class.getName(), 1,
            Level.APPLICATIONERROR, "Error parsing request", "A request definition is corrupted",
            "This request is not usable", "Delete this request");

    private final static BEvent eventSourceNameNotGiven = new BEvent(RequestFactory.class.getName(), 2,
            Level.APPLICATIONERROR, "Source undefined", "The source name is not given",
            "The source name must be give to load it", "Specify a source name");

    public static RequestFactory getInstance() {
        return new RequestFactory();
    }

    /**
     * please use the getInstance
     * Default Constructor.
     */
    private RequestFactory() {
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Result */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public final static String cstJsonRequestName = "name";
    public final static String cstJsonRequestDescription = "description";
    public final static String cstJsonRequestAuthorization = "auth";

    public static class LibraryResult {

        public List<BEvent> listEvents = new ArrayList<BEvent>();
        List<Map<String, Object>> listRequests = new ArrayList<Map<String, Object>>();
        public String contentJsonSt;
        public Map<String, Object> content = null;

        public Map<String, Object> getJson() {
            Map<String, Object> result = new HashMap<String, Object>();
            result.put("listevents", BEventFactory.getHtml(listEvents));
            result.put("listrequests", listRequests);
            if (content != null)
                result.put("content", content);
            return result;
        }
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Operations */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    private final static String cstDomainLibrary = "Library";

    /**
     * Save
     */
    public LibraryResult save(ParameterSource parametersSource, PageResourceProvider pageResourceProvider) {
        LibraryResult libraryResult = new LibraryResult();

        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        libraryResult.listEvents.addAll(bonitaProperties.loaddomainName(cstDomainLibrary));

        bonitaProperties.setProperty(parametersSource.name, parametersSource.jsonSt);

        // then detect all the properties
        libraryResult = getListRequests(libraryResult, bonitaProperties);

        libraryResult.listEvents.addAll(bonitaProperties.store());
        return libraryResult;
    }

    public LibraryResult list(ParameterSource parametersSource, PageResourceProvider pageResourceProvider) {
        LibraryResult libraryResult = new LibraryResult();

        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        libraryResult.listEvents.addAll(bonitaProperties.loaddomainName(cstDomainLibrary));

        // detect all the properties
        libraryResult = getListRequests(libraryResult, bonitaProperties);

        return libraryResult;
    }

    /**
     * load a request
     * 
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
    public LibraryResult load(ParameterSource parametersSource, PageResourceProvider pageResourceProvider) {
        LibraryResult libraryResult = new LibraryResult();

        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        libraryResult.listEvents.addAll(bonitaProperties.loaddomainName(cstDomainLibrary));

        if (parametersSource.name == null) {
            libraryResult.listEvents.add(eventSourceNameNotGiven);
        } else {
            libraryResult.contentJsonSt = bonitaProperties.getProperty(parametersSource.name);
        }
        // So, now parse the saved information to send back correcly
        try {
            libraryResult.content = (Map<String, Object>) JSONValue.parse(libraryResult.contentJsonSt);
        } catch (Exception e) {
            libraryResult.listEvents
                    .add(new BEvent(eventRequestParseError, "Request name[" + parametersSource.name + "]"));
        }

        return libraryResult;
    }

    /**
     * Delete a saved request
     * 
     * @param parametersSource
     * @param pageResourceProvider
     * @return
     */
    public LibraryResult delete(ParameterSource parametersSource, PageResourceProvider pageResourceProvider) {
        LibraryResult libraryResult = new LibraryResult();

        BonitaProperties bonitaProperties = new BonitaProperties(pageResourceProvider);
        libraryResult.listEvents.addAll(bonitaProperties.loaddomainName(cstDomainLibrary));

        bonitaProperties.remove(parametersSource.name);

        libraryResult.listEvents.addAll(bonitaProperties.store());

        // detect all the properties
        libraryResult = getListRequests(libraryResult, bonitaProperties);

        return libraryResult;
    }

    private LibraryResult getListRequests(LibraryResult libraryResult, BonitaProperties bonitaProperties) {
        Enumeration<?> enumKey = bonitaProperties.propertyNames();
        while (enumKey.hasMoreElements()) {
            String requestSt = (String) bonitaProperties.get(enumKey.nextElement());
            Map<String, Object> oneRequest = new HashMap<String, Object>();
            try {
                final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(requestSt);
                if (jsonHash.get(cstJsonRequestName) != null) {
                    oneRequest.put(cstJsonRequestName, jsonHash.get(cstJsonRequestName));
                    oneRequest.put(cstJsonRequestDescription, jsonHash.get(cstJsonRequestDescription));
                    oneRequest.put(cstJsonRequestAuthorization, jsonHash.get(cstJsonRequestAuthorization));

                    libraryResult.listRequests.add(oneRequest);
                }
            } catch (Exception e) {
                // add the request name, then the user can delete it
                if (requestSt != null) {
                    oneRequest.put(cstJsonRequestName, requestSt);
                    libraryResult.listRequests.add(oneRequest);
                }
                libraryResult.listEvents.add(new BEvent(eventRequestParseError, "Request name[" + requestSt + "]"));
            }
        }

        // sort the result
        Collections.sort(libraryResult.listRequests, new Comparator<Map<String, Object>>() {

            public int compare(Map<String, Object> s1,
                    Map<String, Object> s2) {
                return ((String) s1.get(cstJsonRequestName)).compareTo((String) s2.get(cstJsonRequestName));
            }
        });

        return libraryResult;
    }
}
