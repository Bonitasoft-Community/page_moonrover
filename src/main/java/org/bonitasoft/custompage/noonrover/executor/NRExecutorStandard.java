package org.bonitasoft.custompage.noonrover.executor;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.OPERATOR;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult.ResultsetColumn;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRExecutorStandard extends NRExecutor {

    private final static BEvent eventNoColum = new BEvent(NRExecutorStandard.class.getName(), 1, Level.APPLICATIONERROR,
            "No column", "The request does not contains column", "No result to display",
            "Select a column as minimum");

    private final static BEvent eventSqlError = new BEvent(NRExecutorStandard.class.getName(), 2, Level.ERROR,
            "Sql Error", "Error during sql request", "No result to display",
            "Check error");

    private final static BEvent eventSqlParameterError = new BEvent(NRExecutorStandard.class.getName(), 3,
            Level.APPLICATIONERROR,
            "Sql parameter Error",
            "Error during sql request definition. For example, a request \"where name like '%:nameParam'\" are illegal in SQL, use \"where name like :nameParam\" ",
            "No result to display",
            "Check your SQL request");

    public NRStream execute(NRStream executorStream) throws NRException {

        executorStream.isOrderPossible = true;
        executorStream.isFilterPossible = true;

        // create the SQL Request

        StringBuilder sqlRequest = new StringBuilder();
        sqlRequest.append("SELECT ");
        StringBuilder groupBy = new StringBuilder();
        boolean existCol = false;
        for (ResultsetColumn column : executorStream.getResult().listColumnset) {

            if (column.attributeDefinition.relationTableName !=null)
            {
                // attention, this is a second table, skip it for the moment
                column.isVisible=false;
                column.isGroupBy=false;
                column.isSum=false;
                continue;
            }

            if (column.attributeDefinition.isCollection)
            {
                // attention, this is a second table, skip it for the moment
                column.isVisible=false;
                column.isGroupBy=false;
                column.isSum=false;
                continue;
            }
            if (column.attributeDefinition.name.equalsIgnoreCase("persistenceid"))
                column.isQueryable=true;
            if (column.isQueryable || column.isVisible || column.isGroupBy || column.isSum) {
                if (existCol)
                    sqlRequest.append( ", ");
                existCol = true;
                sqlRequest.append(column.attributeDefinition.name);
            }
            if (column.isGroupBy) {
                if (groupBy.length() > 0)
                    groupBy.append( ", " );
                groupBy.append( column.attributeDefinition.name);
            }
        }
        if (!existCol)
            throw new NRException(eventNoColum);

        sqlRequest.append( " FROM " + executorStream.selection.busDefinition.getTableName());

        List<Object> listValues = new ArrayList<>();

        StringBuilder whereClause = new StringBuilder();
        for (NRBusSelection.SelectionParameter parameter : executorStream.selection.listParameters) {
            if (parameter.value != null && parameter.value.toString().trim().length() > 0) {
                if (whereClause.length() > 0)
                    whereClause.append(  " " + parameter.operand.toString() + " ");

                StringBuilder condition = new StringBuilder();
                condition.append( parameter.name + " " + NRBusDefinition.OperatorSql.get(parameter.operator) + " ");

                if (parameter.operator == OPERATOR.ISNULL)
                    continue;
                if (parameter.operator == OPERATOR.LIKE) {
                    listValues.add("%" + parameter.value + "%");
                    condition.append( " ? ");
                } else if (parameter.operator == OPERATOR.RANGE) {

                    listValues.add(getValueParameter(parameter, parameter.value));
                    listValues.add(getValueParameter(parameter, parameter.valueTo));

                    condition.append( "( " + parameter.name + " >= ? AND " + parameter.name + " < ?) ");
                } else {
                    listValues.add(getValueParameter(parameter, parameter.value));

                    condition.append(  " ? " );
                }
                whereClause.append( condition.toString() + " ");
            }
        }
        if (whereClause.length() > 0)
            sqlRequest.append( " WHERE " + whereClause.toString());

        if (groupBy.length() > 0)
            sqlRequest.append( " GROUP BY " + groupBy.toString() );
        if (! executorStream.selection.listOrderBy.isEmpty()) {
            StringBuilder orderClause = new StringBuilder();
            for (NRBusSelection.OrderByParameter orderByparam : executorStream.selection.listOrderBy) {

                if (orderClause.length() > 0)
                    orderClause.append( ", ");
                orderClause.append( orderByparam.columnId + " " + (orderByparam.ascendant ? "ASC" : "DESC"));
            }
            sqlRequest.append( " ORDER BY " + orderClause.toString());
        }

        executorStream = executePreparedStatement(executorStream, sqlRequest.toString(), listValues,
                executorStream.selection.busDefinition.result);

        return executorStream;
    }

    /**
     * return the value according its type
     * 
     * @param parameter
     * @return
     * @throws NRException
     */
    private Object getValueParameter(NRBusSelection.SelectionParameter parameter, Object value) throws NRException {
        if (parameter.type == TYPECOLUMN.DATE) {
            // format is 2018-04-05T07:00:00.000Z
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000'Z'");
            try {
                return sdf.parse((String) value);                
            } catch (Exception e) {
                throw new NRException(new BEvent(eventSqlError, "parameter[" + parameter.name + "] Value["
                        + parameter.value + "] exception:" + e.getMessage()));
            }
        } else
            return value;

    }

    /**
     * execute the prepared statement
     * 
     * @param executorStream
     * @param sqlRequest
     * @param listValues
     * @param result
     * @return
     * @throws NRException
     */
    protected static NRStream executePreparedStatement(NRStream executorStream, String sqlRequest,
            List<Object> listValues, NRBusResult result) throws NRException {

        PreparedStatement pstmt = null;
        
        try (Connection con = getConnection();) {
            
            pstmt = con.prepareStatement(sqlRequest, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);

            for (int i = 0; i < listValues.size(); i++) {
                try {
                    pstmt.setObject(i + 1, listValues.get(i));

                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    e.printStackTrace(new PrintWriter(sw));
                    String exceptionDetails = sw.toString();

                    throw new NRException(new BEvent(eventSqlParameterError, "Invalid parameter[" + i + "] Value["
                            + listValues.get(i) + "] exception:" + e.getMessage() + " at " + exceptionDetails));
                }
            }
            ResultSet rs = pstmt.executeQuery();
            if (rs.last()) {
                executorStream.nbRecords = rs.getRow();
                // Move to beginning

            }
            ResultSetMetaData rsMetaData = rs.getMetaData();
            executorStream.listColumnName.clear();
            for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                executorStream.listColumnName.add(rsMetaData.getColumnName(i));
            }

            // we have to move BEFORE the row we want, because the loop start by a next()
            if (executorStream.startIndex == 0)
                rs.beforeFirst();
            else
                rs.absolute((int) executorStream.startIndex);
            int count = 0;

            // Data is retrieve as it, then the next executor will decide and transform it if needed
            while (rs.next() && count < executorStream.maxResults) {
                count++;
                Map<String, Object> record = new HashMap<>();
                // result pilot the item to retrieve
                if (result != null)
                    for (ResultsetColumn column : result.listColumnset) {
                        if (column.isQueryable|| column.isVisible || column.isSum)
                            record.put(column.attributeDefinition.name, rs.getObject(column.attributeDefinition.name));
                    }
                else {
                    // ask the meta data
                    for (int i = 1; i <= rsMetaData.getColumnCount(); i++) {
                        record.put(rsMetaData.getColumnName(i), rs.getObject(i));
                    }

                }
                executorStream.listData.add(record);
            }

        } catch (NRException nr) {
            throw nr;
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            throw new NRException(new BEvent(eventSqlError,
                    "sqlRequest[" + sqlRequest + "] exception:" + e.getMessage() + " at " + exceptionDetails));
        } finally {
            // explicitaly release the connection
            if (pstmt != null)
                try {
                    pstmt.close();
                } catch (SQLException e) {
                }
           
        }
        return executorStream;
    }
}
