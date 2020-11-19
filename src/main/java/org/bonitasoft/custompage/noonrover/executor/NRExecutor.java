package org.bonitasoft.custompage.noonrover.executor;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.custompage.noonrover.source.NRSourceDatabase;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.log.event.BEventFactory;

public abstract class NRExecutor {

    /**
     * This class carry all the Data along the request. it's start by the different source parameters, and is completed by all the different request
     * 
     * @author Firstname Lastname
     */
    public static class ExecutorStream {

        // give a name to this execution
        public String name;
        public List<BEvent> listEvents = new ArrayList<>();
        public List<Map<String, Object>> listData = new ArrayList<>();
        public List<Map<String, Object>> listHeader = new ArrayList<>();
        public List<Map<String, Object>> listFooterData = new ArrayList<>();
        /**
         * if the executor run a Sql request, it may keep the columnSet visible, and should match with the listData information
         */
        public List<String> listColumnName = new ArrayList<>();
        /**
         * some executor can build from scratch the result
         */
        public NRBusResult result;
        /**
         * some executor need this information, or can build one from scratch
         */
        public NRBusSelection selection;

        public long startIndex;
        public long maxResults;
        public long nbRecords;

        // Executor can return a JSON value, or work directly with the httpResponse value
        // this is by default the value
        public boolean isJsonResult = true;

        public HttpServletResponse response;
        public OutputStream out = null;

        /**
         * source describe if the order or the filter will be possible after when result are visible
         */
        public boolean isOrderPossible = false;
        public boolean isFilterPossible = false;

        public APISession apiSession;

        /**
         * prepare the RequestData with information form the parameter source
         * Default Constructor.
         * 
         * @param parameterSource
         */
        public ExecutorStream(NoonRoverAccessAPI.ParameterSource parameterSource) {
            this.startIndex = parameterSource.startIndex == null ? 0 : parameterSource.startIndex;
            this.maxResults = parameterSource.maxResults == null ? 100 : parameterSource.maxResults;
            this.apiSession = parameterSource.apiSession;
        }

        public Map<String, Object> getJson() {
            Map<String, Object> resultJson = new HashMap<>();
            resultJson.put("listevents", BEventFactory.getHtml(listEvents));
            resultJson.put("listdata", listData);
            resultJson.put("listheader", listHeader);
            resultJson.put("listfooterdata", listFooterData);
            resultJson.put(NoonRoverAccessAPI.cstJsonStartIndex, Long.valueOf(startIndex));
            resultJson.put(NoonRoverAccessAPI.cstJsonMaxResults, Long.valueOf(maxResults));
            resultJson.put(NoonRoverAccessAPI.cstJsonNbRecords, Long.valueOf(nbRecords));

            return resultJson;
        }

        /**
         * manage the result
         */
        public boolean isJsonResult() {
            return isJsonResult;
        }

        public void openDirectOutput(String contentType, String fileName) throws IOException {
            response.addHeader("content-type", contentType);
            response.addHeader("content-disposition", "attachment; filename=" + fileName);
            isJsonResult = false;

            out = response.getOutputStream();
        }

        public void writeDirectOutput(String content) throws IOException {
            out.write(content.getBytes());
        }

        public void closeDirectOutput() throws IOException {
            out.flush();
            out.close();
        }
    }

    private final static BEvent eventTypeNotFound = new BEvent(NRExecutor.class.getName(), 1, Level.APPLICATIONERROR,
            "Type result unkown", "A type is given, but not found", "Action is not possible", "Report it as a bug");

    /**
     * getInstance according the type
     * 
     * @param type
     * @return
     * @throws NRException
     */
    public static NRExecutor getInstance(TYPESELECTION type) throws NRException {

        if (type == TYPESELECTION.SQL)
            return new NRExecutorSql();
        if (type == TYPESELECTION.STD)
            return new NRExecutorStandard();
        if (type == TYPESELECTION.FIND)
            return new NRExecutorBdm();
        if (type == TYPESELECTION.PROCESS)
            return new NRExecutorProcess();
        throw new NRException(new BEvent(eventTypeNotFound, "typeSelection[" + type.toString() + "]"));
    }

    public static Connection getConnection() throws SQLException {
        return NRSourceDatabase.getConnection();
    }

    /**
     * @param selection
     * @return
     * @throws NRException
     */
    public abstract NRExecutor.ExecutorStream execute(NRExecutor.ExecutorStream requestData) throws NRException;

}
