package org.bonitasoft.custompage.noonrover.executor;

import java.sql.Connection;
import java.sql.SQLException;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.custompage.noonrover.source.NRSourceDatabase;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public abstract class NRExecutor {

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
    public abstract NRStream execute(NRStream requestData) throws NRException;

    
  
}
