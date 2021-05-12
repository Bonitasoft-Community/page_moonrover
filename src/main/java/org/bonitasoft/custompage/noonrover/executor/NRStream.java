package org.bonitasoft.custompage.noonrover.executor;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

/**
 * This class carry all the Data along the request. it's start by the different source parameters, and is completed by all the different request
 * 

 */
public class NRStream {

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
    private NRBusResult result;
    


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

    
    private OutputStream out = null;

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
    public NRStream(NoonRoverAccessAPI.ParameterSource parameterSource) {
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

    /**
     * Provide / Get the outputstream where result is sent
     * @param out
     */
    public void setOutputstream( OutputStream out ) {
        this.out = out;
    }
    public OutputStream getOutputstream( ) {
        return this.out;
    }

    
    
    public void setContentType(String contentType, String fileName) throws IOException {
        isJsonResult = false;
    }

    public void writeDirectOutput(String content) throws IOException {
        out.write(content.getBytes());
    }

    public void endDirectOutput() throws IOException {
        out.flush();
    }
    
    
    
    /* getter / setter */
    public NRBusResult getResult() {
        return result;
    }
    
    public void setResult(NRBusResult result) {
        this.result = result;
    }
   

}