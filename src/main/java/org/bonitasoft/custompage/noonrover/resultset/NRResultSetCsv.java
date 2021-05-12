package org.bonitasoft.custompage.noonrover.resultset;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.executor.NRStream;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRResultSetCsv extends NRResultSet {

    private final static BEvent eventOutputInError = new BEvent(NRResultSetCsv.class.getName(), 1,
            Level.APPLICATIONERROR,
            "Output in error", "The result is send to the output when an error arrived", "Action is not possible",
            "Is the browser connection closed by the user?");

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss Z");

    public NRStream execute(NRStream executorStream) throws NRException {
        try {
            executorStream.setContentType("application/vnd.ms-excel", executorStream.name + ".csv"); //

            StringBuilder lineHeader = new StringBuilder();
            for (Map<String, Object> column : executorStream.listHeader) {
                if (lineHeader.length() > 0)
                    lineHeader.append(";");
                Object title = column.get(NRBusResult.cstJsonColumnTitle);
                if (title == null)
                    title = "";
                // csv separator is based on ;, so don't let the ; broke the list
                title = title.toString().replaceAll(";", "\\;");
                lineHeader.append(title);
            }
            executorStream.writeDirectOutput(lineHeader.toString() + "\n");

            // now all lines
            for (Map<String, Object> record : executorStream.listData) {
                lineHeader = new StringBuilder();
                for (Map<String, Object> column : executorStream.listHeader) {
                    if (lineHeader.length() > 0)
                        lineHeader.append(";");
                    String colId = (String) column.get(NRBusResult.cstJsonColumnId);

                    Object colValue = record.get(colId);
                    if (colValue == null)
                        colValue = "";
                    // csv separator is based on ;, so don't let the ; broke the list
                    colValue = colValue.toString().replaceAll(";", "\\;");
                    lineHeader.append(colValue);
                }
                executorStream.writeDirectOutput(lineHeader.toString() + "\n");
            }
            executorStream.endDirectOutput();

        } catch (IOException e) {
            executorStream.listEvents.add(new BEvent(eventOutputInError, e, ""));
        }
        return executorStream;
    }

}
