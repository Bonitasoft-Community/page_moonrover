package org.bonitasoft.custompage.noonrover.toolbox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRToolbox {

    public static class NRException extends Exception {

        public List<BEvent> listEvents = new ArrayList<BEvent>();

        public NRException() {
        }

        public NRException(BEvent event) {
            listEvents.add(event);
        }

        public void addEvent(BEvent event) {
            listEvents.add(event);
        }
    }

    private final static BEvent eventMissingMandatoryParameter = new BEvent(NRToolbox.class.getName(), 1, Level.ERROR,
            "Missing Mandatory parameters", "A parameter is expected in the JSON, and it's missing",
            "Action is not possible", "Report it as a bug");
    private final static BEvent eventBadTypeParameter = new BEvent(NRToolbox.class.getName(), 2, Level.ERROR,
            "Bad type parameters", "A parameter is expected with a certain type, and a different type is provided",
            "Action is not possible", "Report it as a bug");

    public static Object getJsonParameter(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation, Class className) throws NRException {
        if (mandatory && !jsonMap.containsKey(parameterName)) {
            NRException nrException = new NRException();
            nrException.listEvents.add(new BEvent(eventMissingMandatoryParameter,
                    "Parametername[" + parameterName + "] localisation[" + localisation + "]"));
            throw nrException;
        }
        Object value = jsonMap.get(parameterName);
        if (value == null)
            return null;
        if (!className.isInstance(value)) {
            NRException nrException = new NRException();
            nrException.listEvents.add(new BEvent(eventBadTypeParameter,
                    "Parametername[" + parameterName + "] localisation[" + localisation + "]"));
            throw nrException;
        }
        return value;
    }

    public static String getJsonSt(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (String) getJsonParameter(mandatory, parameterName, jsonMap, localisation, String.class);
    }

    public static Boolean getJsonBoolean(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (Boolean) getJsonParameter(mandatory, parameterName, jsonMap, localisation, Boolean.class);
    }

    public static Integer getJsonInteger(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (Integer) getJsonParameter(mandatory, parameterName, jsonMap, localisation, Integer.class);
    }

    public static Long getJsonLong(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (Long) getJsonParameter(mandatory, parameterName, jsonMap, localisation, Long.class);
    }

    public static List getJsonList(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (List) getJsonParameter(mandatory, parameterName, jsonMap, localisation, List.class);
    }

    public static Map getJsonMap(boolean mandatory, String parameterName, Map<String, Object> jsonMap,
            String localisation) throws NRException {
        return (Map) getJsonParameter(mandatory, parameterName, jsonMap, localisation, Map.class);

    }

    public static Double getDoubleValue(Object value, Double defaultValue) {
        try {
            if (value == null)
                return defaultValue;
            if (value instanceof Double)
                return (Double) value;
            return Double.valueOf(value.toString());

        } catch (Exception e) {
            return defaultValue;
        }
    }
}
