package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRBusResult {

    private final static BEvent eventNoTypeFound = new BEvent(NRBusResult.class.getName(), 1, Level.APPLICATIONERROR,
            "Type of result not exist", "A type is given, but not found", "Action is not possible",
            "Report it as a bug");

    public enum TYPERESULTSET {
        TABLE, CHART, JASPER, EDITRECORD
                              
    }

    public TYPERESULTSET typeResultSet = TYPERESULTSET.TABLE;
    // choose between NRBussAttribute and NRResult.ColumnParameter
    public List<ResultsetColumn> listColumnset = new ArrayList<>();

    private NRBusDefinition busSelection;

    public NRBusResult(NRBusDefinition busSelection) {
        this.busSelection = busSelection;
    }

    /**
     * The business Object show a result:
     * - a Table, containing a list of ColumnParameters
     * - a Chart (to be define)
     * - a Jasper report (to be define)
     */
    public static class ResultsetColumn {

        public NRBusAttribute attributeDefinition;
        // ColName in UPPER CASE

        /** attribut */
        public boolean isVisible;
        public boolean isGroupBy;
        public boolean isSum;
        public boolean isOrder;
        public boolean isFilter;
        // this column must be part of the query, and on the result
        public boolean isQueryable=false;

        protected ResultsetColumn(NRBusAttribute attributeDefinition) {
            this.attributeDefinition = attributeDefinition;
            isVisible = true;
        }

        @Override
        public String toString() {
            return "ResultSetCol " + attributeDefinition.tableName + "." + attributeDefinition.name + "("
                    + attributeDefinition.type + ")";
        }

    }

    public void addListResultsetColumFromAttribute(NRBusAttribute attribute) {
        this.listColumnset.add(new ResultsetColumn(attribute));
    }

    public void setListResultsetColumn(List<ResultsetColumn> listColumns) {
        this.listColumnset = listColumns;
    }

    public List<ResultsetColumn> getListResultsetColumn() {
        return this.listColumnset;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* JSON */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public final static String cstJsonType = "typeresult";

    public final static String cstJsonColumns = "columns";
    public final static String cstJsonColumnName = "name";
    public final static String cstJsonColumnVisible = "visible";
    public final static String cstJsonColumnGroupBy = "groupby";
    public final static String cstJsonColumnSum = "sum";

    public final static String cstJsonColumnTitle = "title";
    public final static String cstJsonColumnId = "columnid";
    /**
     * the column can be ordered
     */
    public final static String cstJsonColumnIsordered = "isOrderer";
    /**
     * the colum can be filtered
     */
    public final static String cstJsonColumnIsfiltered = "isFiltered";
    public final static String cstJsonColumnIsVisible = "isVisible";
    public final static String cstJsonColumnType = "type";
    public final static String cstJsonColumnTypeEditRecord= "_EDITRECORD";
    
    public NRBusResult() {
        
    }
    
    public Map<String, Object> getJson() {
        Map<String, Object> resultJson = new HashMap<>();

        List<Map<String, Object>> listColumnJson = new ArrayList<>();
        for (ResultsetColumn column : listColumnset) {
            Map<String, Object> columnJson = new HashMap<>();
            columnJson.put(cstJsonColumnName, column.attributeDefinition.name);
            columnJson.put("type", column.attributeDefinition.type.toString());
            columnJson.put(cstJsonColumnVisible, column.isVisible);
            columnJson.put(cstJsonColumnGroupBy, column.isGroupBy);
            columnJson.put(cstJsonColumnSum, column.isSum);

            columnJson.put(cstJsonColumnIsordered, column.isOrder);
            columnJson.put(cstJsonColumnIsfiltered, column.isFilter);

            listColumnJson.add(columnJson);

        }
        resultJson.put(cstJsonColumns, listColumnJson);
        return resultJson;
    }

    public void fromJson(Map<String, Object> resultJson) throws NRException {
        String locationJson = "/" + busSelection.getName() + "/result";
        String typeResultSt = NRToolbox.getJsonSt(true, cstJsonType, resultJson, locationJson);
        try {
            typeResultSet = TYPERESULTSET.valueOf(typeResultSt);
        } catch (Exception e) {
            throw new NRException(new BEvent(eventNoTypeFound, "Type ResultSet(" + cstJsonType + ")"));
        }

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> columnsList = (List<Map<String, Object>>) NRToolbox.getJsonList(true, cstJsonColumns, resultJson,
                locationJson);
        for (Map<String, Object> columnJson : columnsList) {
            String columnName = NRToolbox.getJsonSt(true, cstJsonColumnName, columnJson,
                    locationJson);
            for (ResultsetColumn column : listColumnset) {
                if (column.attributeDefinition.name.equals(columnName)) {
                    String locationJsonCol = locationJson + cstJsonColumns;
                    column.isVisible = NRToolbox.getJsonBoolean(true, cstJsonColumnVisible, columnJson,
                            locationJsonCol);
                    column.isGroupBy = NRToolbox.getJsonBoolean(true, cstJsonColumnGroupBy, columnJson,
                            locationJsonCol);
                    column.isSum = NRToolbox.getJsonBoolean(true, cstJsonColumnSum, columnJson,
                            locationJsonCol);
                }
            }
        }
    }

}
