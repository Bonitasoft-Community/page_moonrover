package org.bonitasoft.custompage.noonrover.executor;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;

/**
 * This class Extends the Stream, and send the result to a ServletResponse
 * The main difference is that the ContentType has to be send to the Servlet Response BEFORE the data
 * 
 * @author Firstname Lastname
 */
public class NRStreamServletResponse extends NRStream {

    public HttpServletResponse response;

    public NRStreamServletResponse(ParameterSource parameterSource, HttpServletResponse response) {
        super(parameterSource);
    }
    public void setContentType(String contentType, String fileName) throws IOException {
        response.addHeader("content-type", contentType);
        response.addHeader("content-disposition", "attachment; filename=" + fileName);
        isJsonResult = false;
        setOutputstream( response.getOutputStream() );
        super.setContentType( contentType, fileName);
    }
    
    public void endDirectOutput() throws IOException {
        response.getOutputStream().flush();
        response.getOutputStream().close();
        // don't close the out
    }
}
