package org.bonitasoft.custompage.noonrover.resultset;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult.TYPERESULTSET;
import org.bonitasoft.custompage.noonrover.executor.NRExecutor;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public abstract class NRResultSet extends NRExecutor {

    private final static BEvent eventTypeNotFound = new BEvent(NRToolbox.class.getName(), 1, Level.APPLICATIONERROR,
            "Type unkown", "A type is given, but not found", "Action is not possible", "Report it as a bug");

    /**
     * getInstance according the type
     * 
     * @param type
     * @return
     * @throws NRException
     */
    public static NRResultSet getInstance(TYPERESULTSET type) throws NRException {
        if (type == TYPERESULTSET.TABLE)
            return new NRResultSetTable(false);
        if (type == TYPERESULTSET.EDITRECORD)
            return new NRResultSetTable(true);
              
        if (type == TYPERESULTSET.CHART)
            return new NRResultSetChart();
        if (type == TYPERESULTSET.JASPER)
            return new NRResultSetJasper();
        throw new NRException(
                new BEvent(eventTypeNotFound, "typeResult[" + (type == null ? "null" : type.toString()) + "]"));

    }

}
