package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource.TYPEOUTPUT;
import org.bonitasoft.custompage.noonrover.executor.NRExecutor;
import org.bonitasoft.custompage.noonrover.resultset.NRResultSet;
import org.bonitasoft.custompage.noonrover.resultset.NRResultSetCsv;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

/**
 * this object describe a Business Definition. A Business Definition may be a BDM object, or a process.
 * There are two main item in the description:
 * - the structure of the object : the list of FIND in a BDM, the list of column which compose the object.
 * - the user configuration to run a request. For example, user can select an operator LIKE for a parameters.
 * To be simple, the two concept are managed together. When a user create a "Report", we update the current definition. Then, user can save a "report", and load
 * it.
 * At loading, we compare the existing structure with the one sauvegarded, and a merge is done (a new field may be added in the BDM).
 * A Business Definition contains:
 * - a list of "Selection". There are for example one selection per Find in the BDM, plus 2 selection "SQL" and "DIRECT"
 * - a list of "Attributes". for a BDM, Attribute is the BDM attributes (JDBC column). For a Process, Attribute are process name, process version, String Index,
 * Variables
 * - a list of "Result". The result may be a Table, a Chart, a Jasper report.
 */
public class NRBusDefinition {

    private final static BEvent eventNoSelectionFound = new BEvent(NRToolbox.class.getName(), 1, Level.APPLICATIONERROR,
            "Selection not exist", "A selection is given, but not found", "Action is not possible",
            "Report it as a bug");

    private String sourceName;
    /**
     * source is a BDM ? Then we have a tableName
     */
    private String tableName;

    private String description;

    /**
     * source may want to transport a object
     */
    private Object objectTransported;

    public enum TYPESOURCE {
        BDM, PROCESS, TABLE
    };

    private TYPESOURCE typeSource = TYPESOURCE.BDM;

    public enum TYPECOLUMN {
        STRING, NUM, DATE, BOOLEAN,RELATION
    };

    /**
     * Should evoluate :
     * list of table is due to MULTIPLE FIELD
     */

    public List<String> listChilds = new ArrayList<String>();

    public enum OPERAND {
        AND, OR
    };

    public enum OPERATOR {
        EQUALS, ISNULL, DIFF, LIKE, UPPER, LOWER, RANGE
    };

    public static Map<OPERATOR, String> OperatorSql = new HashMap<OPERATOR, String>() {

        {
            put(OPERATOR.EQUALS, "=");
            put(OPERATOR.ISNULL, "isnull");
            put(OPERATOR.DIFF, "!=");
            put(OPERATOR.LIKE, "like");
            put(OPERATOR.UPPER, ">");
            put(OPERATOR.LOWER, "<");
            put(OPERATOR.RANGE, "");

        }
    };

    public List<NRBusSelection> listSelections = new ArrayList<NRBusSelection>();

    /**
     * reference the result for the definition
     * Build one result at this moment, but it may possible to have multiple result in the futur to combine it
     * (example, a table AND a chart), or a table AND a chart INSIDE a Jasper report
     */
    public NRBusResult result;

    /**
     * index. Key must be in lower case
     */
    private Map<String, NRBusSetAttributes> mapIndexes = new HashMap<String, NRBusSetAttributes>();

    /**
     * constraints. Key must be in lower case
     */
    private Map<String, NRBusSetAttributes> mapConstraints = new HashMap<String, NRBusSetAttributes>();

    private NRBusSelection selectedSelection = null;

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Instanciation */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * only the factory can create one
     * 
     * @param tableName
     */
    protected NRBusDefinition(String sourceName) {
        this.sourceName = sourceName;
        result = new NRBusResult(this);
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Getter / setter */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    public String getName() {
        return this.sourceName;
    }

    public String getDisplayName() {
        int pos = sourceName.lastIndexOf(".");
        if (pos == -1)
            return sourceName;
        else
            return sourceName.substring(pos + 1) + " (" + sourceName.substring(0, pos - 1) + ")";
    }

    /**
     * source is a BDM ? We should have a tablename
     * 
     * @param tableName
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getTableName() {
        return tableName;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    /**
     * according the type of Definition, an object may be transported, to be used at RunTime. It may be a Business Object, or a Process.
     * 
     * @param objectTransported
     */
    public void setObjectTransported(Object objectTransported) {
        this.objectTransported = objectTransported;
    }

    public Object getObjectTransported() {
        return objectTransported;
    }

    public void setTypeSource(TYPESOURCE typeSource) {
        this.typeSource = typeSource;
    }

    public TYPESOURCE getTypeSource() {
        return typeSource;
    }

    public void addCollectionTable(String completeTableName, String childTableName) {
        listChilds.add(completeTableName);
    }

    public void setMapIndexes(Map<String, NRBusSetAttributes> indexes) {
        this.mapIndexes = indexes;
    }

    public void setMapConstraints(Map<String, NRBusSetAttributes> constraints) {
        this.mapConstraints = constraints;
    }

    /**
     * set the list of Colum for this object
     * 
     * @param listColumnset
     */
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Attribute management */
    /*                                                                      */
    /* a Business Definition has a list of attribute */
    /* -------------------------------------------------------------------- */
    public List<NRBusAttribute> listAttributes = new ArrayList<NRBusAttribute>();

    public NRBusAttribute getInstanceAttribute(String tableName, String name, TYPECOLUMN typeColumn, boolean isCollection) {
        NRBusAttribute columnParameter = new NRBusAttribute(this, tableName, name, typeColumn, isCollection);
        this.listAttributes.add(columnParameter);
        return columnParameter;
    }
    public NRBusAttribute getInstanceRelationAttribute(String tableName, String name, String relationTableName, boolean isCollection) {
        NRBusAttribute columnParameter = new NRBusAttribute(this, tableName, name, TYPECOLUMN.RELATION, isCollection);
        columnParameter.relationTableName= relationTableName;
        this.listAttributes.add(columnParameter);
        return columnParameter;
    }

    public void setListAttributes(List<NRBusAttribute> listColumns) {
        this.listAttributes = listColumns;
    }

    public List<NRBusAttribute> getListAttributes() {
        return this.listAttributes;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* complete item per item */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public NRBusSelection getInstanceBusSelection(String selectionName) {
        NRBusSelection selection = new NRBusSelection(this);
        selection.name = selectionName;
        listSelections.add(selection);
        return selection;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Execute the request */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    /**
     * execute a request
     * 
     * @return
     */
    public NRExecutor.ExecutorStream execute(NoonRoverAccessAPI.ParameterSource parameterSource,
            NRExecutor.ExecutorStream executorStream) {
        try {
            // preload the executorStream with current Business Definition 
            executorStream.result = result;
            executorStream.selection = selectedSelection;
            executorStream.name = this.getName();
            List<NRExecutor> listExecutor = new ArrayList<>();

            listExecutor.add(NRExecutor.getInstance(selectedSelection.typeFind));
            listExecutor.add(NRResultSet.getInstance(result.typeResultSet));
            if (parameterSource.typeOutput == TYPEOUTPUT.CSV)
                listExecutor.add(new NRResultSetCsv());

            for (NRExecutor executor : listExecutor) {
                executorStream = executor.execute(executorStream);
            }
        } catch (NRException e) {
            executorStream.listEvents.addAll(e.listEvents);
        }
        return executorStream;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* getJson */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public final static String cstJsonSourceName = "sourcename";
    public final static String cstJsonDisplayName = "displayname";
    public final static String cstJsonResult = "result";
    public final static String cstJsonSelections = "selections";
    public final static String cstJsonSelection = "selection";
    public final static String cstJsonSelectionname = "name";

    public Map<String, Object> getJson() {
        Map<String, Object> resultJson = new HashMap<String, Object>();
        resultJson.put(cstJsonSourceName, sourceName);
        resultJson.put(cstJsonDisplayName, getDisplayName());

        resultJson.put("tablename", tableName);
        resultJson.put("type", typeSource.toString());
        resultJson.put("description", "Description of " + sourceName);

        /** possible selection */
        List<Map<String, Object>> listSelectionsJson = new ArrayList<Map<String, Object>>();

        for (NRBusSelection selection : listSelections) {

            listSelectionsJson.add(selection.getJson());

        }
        resultJson.put(cstJsonSelections, listSelectionsJson);

        resultJson.put(cstJsonResult, result.getJson());
        /** all result column */

        return resultJson;
    }

    /*
     * getEventFrom Json:
     * Example of JSON
     * {
     * "name": "communtranche",
     * "selection": {
     * "name": "Standard",
     * "type": "STD",
     * "parameters": [
     * {
     * "visible": true,
     * "name": "persistenceid",
     * "type": "NUM",
     * "operator": "EQUALS"
     * },
     * {
     * "visible": true,
     * "name": "communtranches_order",
     * "type": "NUM",
     * "operator": "EQUALS"
     * }
     * ],
     * "showAll": true
     * },
     * "result": {
     * "columns": [
     * {
     * "visible": true,
     * "name": "persistenceid",
     * "type": "NUM"
     * },
     * {
     * "visible": true,
     * "name": "communtranches_order",
     * "type": "NUM"
     * }
     * ]
     * }
     * }
     * }]
     */

    public void fromJson(Map<String, Object> requestJson) throws NRException {

        NRException nrException = new NRException();
        Map<String, Object> selectionJson = NRToolbox.getJsonMap(false, cstJsonSelection, requestJson, "/" + getName());
        // information may not be given
        if (selectionJson==null)
            return; 
        String selectionName = NRToolbox.getJsonSt(true, cstJsonSelectionname, selectionJson, "/" + getName());
        selectedSelection = null;
        for (NRBusSelection selectionIt : listSelections) {
            if (selectionIt.name.equals(selectionName))
                selectedSelection = selectionIt;
        }

        if (selectedSelection == null)
            nrException.addEvent(new BEvent(eventNoSelectionFound, "Selection[" + selectionName));
        else
            selectedSelection.fromJson(selectionJson);

        Map<String, Object> resultJson = NRToolbox.getJsonMap(true, cstJsonResult, requestJson,
                "/" + getName());
        result.fromJson(resultJson);

        List<Map<String, Object>> ordersList = NRToolbox.getJsonList(false, NRBusSelection.cstJsonOrder, requestJson,
                "/" + getName());
        if (ordersList != null) {
            selectedSelection.listOrderBy = new ArrayList<NRBusSelection.OrderByParameter>();
            for (Map<String, Object> orderMap : ordersList) {
                NRBusSelection.OrderByParameter orderByParameter = new NRBusSelection.OrderByParameter();
                orderByParameter.columnId = (String) orderMap.get(NRBusSelection.cstJsonOrderColumId);
                orderByParameter.ascendant = "ASC".equals(orderMap.get(NRBusSelection.cstJsonOrderDirection));
                selectedSelection.listOrderBy.add(orderByParameter);

            }
        }
        return;
    }

}
