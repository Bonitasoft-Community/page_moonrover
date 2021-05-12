package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.OPERAND;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;

public class NRBusSelection {

    public enum TYPESELECTION {
        STD, SQL, FIND, PROCESS
    };

    public NRBusDefinition busDefinition;
    public static final String CST_NAME_STANDARD = "Standard"; 
    public String name;
    public List<NRBusSelection.SelectionParameter> listParameters = new ArrayList<>();
    public TYPESELECTION typeFind;

    public List<OrderByParameter> listOrderBy = new ArrayList<>();

    /** selectionParameter */
    public static class SelectionParameter {

        public String name;
        public TYPECOLUMN type;
        public NRBusDefinition.OPERAND operand = OPERAND.AND;
        public NRBusDefinition.OPERATOR operator = NRBusDefinition.OPERATOR.EQUALS;
        public boolean isVisible = true; // default
        public Object value;
        public Object valueTo;

        protected SelectionParameter(String nameParameter, TYPECOLUMN typeParameter) {
            this.name = nameParameter;
            this.type = typeParameter;
            this.isVisible = true;

        }
    }

    public Object objectTransported;

    public static class OrderByParameter {

        public String columnId;
        public boolean ascendant;
    }

    /**
     * simple mechanism to save a map of parameters Name / value
     */
    public Map<String, Object> parametersValue = new HashMap<>();

    public String sqlText = null;

    public final static String cstJsonParameters = "parameters";
    public final static String cstJsonParametersValue = "parametersvalue";

    public final static String cstJsonSelectionName = "name";
    public final static String cstJsonSelectionType = "type";

    // in case of a Direct SQL selection
    public final static String cstJsonSqlText = "sqltext";

    public final static String cstJsonParameterName = "name";
    public final static String cstJsonParameterOperator = "operator";
    public final static String cstJsonParameterType = "type";
    public final static String cstJsonParameterValue = "value";
    public final static String cstJsonParameterValueTo = "valueTo";
    public final static String cstJsonParameterVisible = "visible";

    public final static String cstJsonOrder = "order";
    public final static String cstJsonOrderColumId = "columnid";
    public final static String cstJsonOrderDirection = "direction";

    public NRBusSelection(NRBusDefinition busDefinition) {
        this.busDefinition = busDefinition;
    }

    public SelectionParameter getInstanceSelectionParameter(String name, TYPECOLUMN typeColumn) {
        SelectionParameter selectionParameter = new SelectionParameter(name, typeColumn);
        listParameters.add(selectionParameter);
        return selectionParameter;
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
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* JSON */
    /*                                                                      */
    /* -------------------------------------------------------------------- */

    /**
     * @return
     */
    public Map<String, Object> getJson() {

        Map<String, Object> selectionJson = new HashMap<String, Object>();
        selectionJson.put(cstJsonSelectionName, name);
        selectionJson.put(cstJsonSelectionType, typeFind.toString());
        // add all parameters
        List<Map<String, Object>> listParametersJson = new ArrayList<Map<String, Object>>();

        for (NRBusSelection.SelectionParameter parameter : listParameters) {
            Map<String, Object> parameterJson = new HashMap<String, Object>();
            parameterJson.put(cstJsonParameterName, parameter.name);
            parameterJson.put(cstJsonParameterType, parameter.type.toString());
            parameterJson.put(cstJsonParameterOperator, parameter.operator.toString());
            parameterJson.put(cstJsonParameterVisible, parameter.isVisible);
            parameterJson.put(cstJsonParameterValue, parameter.value);

            listParametersJson.add(parameterJson);
        }
        selectionJson.put(cstJsonParameters, listParametersJson);
        return selectionJson;
    }

    /**
     * "visible": true,
     * "name": "persistenceid",
     * "type": "NUM",
     * "operator": "EQUALS"
     * 
     * @param parametersList
     * @throws NRException
     */
    public void fromJson(Map<String, Object> selectionJson) throws NRException {

        List<Map<String, Object>> parametersList = NRToolbox.getJsonList(true,
                NRBusSelection.cstJsonParameters, selectionJson, "/" + this.busDefinition.getName());

        List<Map<String, Object>> parametersValueList = NRToolbox.getJsonList(true,
                NRBusSelection.cstJsonParametersValue, selectionJson, "/" + this.busDefinition.getName());

        // we can do a N² algorithm because there are not too much parameters (<100).
        for (Map<String, Object> oneParameter : parametersList) {
            String parameterName = NRToolbox.getJsonSt(true, cstJsonParameterName, oneParameter,
                    "/" + busDefinition.getName() + "/selection");
            NRBusDefinition.OPERATOR operator = NRBusDefinition.OPERATOR.valueOf(NRBusDefinition.OPERATOR.class,
                    NRToolbox.getJsonSt(true, cstJsonParameterOperator, oneParameter,
                            "/" + busDefinition.getName() + "/selection"));
            Object value = NRToolbox.getJsonParameter(false, cstJsonParameterValue, oneParameter,
                    "/" + busDefinition.getName() + "/selection", Object.class);

            Object valueTo = NRToolbox.getJsonParameter(false, cstJsonParameterValueTo, oneParameter,
                    "/" + busDefinition.getName() + "/selection", Object.class);

            for (SelectionParameter selectionParameter : listParameters) {
                if (selectionParameter.name.equals(parameterName)) {
                    selectionParameter.value = value;
                    selectionParameter.valueTo = valueTo;
                    selectionParameter.operator = operator;
                }
            }

        }

        // the parameters value is a simple mecanism, and the parameters name is not related to a selection parameters
        for (Map<String, Object> oneParameter : parametersValueList) {
            String parameterName = NRToolbox.getJsonSt(true, cstJsonParameterName, oneParameter,
                    "/" + busDefinition.getName() + "/selection");
            Object value = NRToolbox.getJsonParameter(false, cstJsonParameterValue, oneParameter,
                    "/" + busDefinition.getName() + "/selection", Object.class);
            // String type = NRToolbox.getJsonSt(true, cstJsonParameterType, oneParameter,  "/" + busDefinition.getName() + "/selection");
            parametersValue.put(parameterName, value);

        }

        sqlText = NRToolbox.getJsonSt(false, cstJsonSqlText, selectionJson,
                "/" + busDefinition.getName() + "/selection");
    }
}
