package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.source.NRSource;
import org.bonitasoft.custompage.noonrover.source.NRSourceFactory;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.engine.bdm.model.BusinessObject;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;

public class NRBusDefinitionFactory {

    private final static BEvent eventNoDefinitionFound = new BEvent(NRToolbox.class.getName(), 1,
            Level.APPLICATIONERROR, "Definition not exist", "A source is given, but not found",
            "Action is not possible", "Report it as a bug");

    private Map<String, NRBusDefinition> setBusinessDefinition = new HashMap<>();

    public static NRBusDefinitionFactory getInstance() {
        return new NRBusDefinitionFactory();
    };

    public NRBusDefinition createDataDefinition(String sourceName) {
        NRBusDefinition businessDefinition = new NRBusDefinition(sourceName);
        setBusinessDefinition.put(businessDefinition.getName(), businessDefinition);
        return businessDefinition;
    }
    
    /**
     * Create and register this BusinessObject in the factory
     * @param businessObject
     * @return
     */
    public NRBusDefinition createDataDefinitionBDM( BusinessObject businessObject ) {
        NRBusDefinitionBDM businessDefinition = new NRBusDefinitionBDM( businessObject );
        
        setBusinessDefinition.put( businessDefinition.getName(), businessDefinition);
        return businessDefinition;
    }

    public Map<String, NRBusDefinition> getSetBusinessDefinition() {
        return setBusinessDefinition;
    };

    public NRBusDefinition getByName(String sourceName) {
        return setBusinessDefinition.get(sourceName);
    }

    /**
     * @param busDefinitionJson
     * @return
     */
    public NRBusDefinition fromJson(APISession apiSession, Map<String, Object> requestJson) throws NRException {

        // is there are nothing in the cache? Then, reload it :-(
        if (setBusinessDefinition.isEmpty()) {
            // let call the SourceFactory instance, which load all the busDefinition in this factory
            NRSourceFactory sourceFactory = NRSourceFactory.getInstance();
            referenceSources(apiSession, sourceFactory);
        }

        // search the existing source 
        String sourceName = NRToolbox.getJsonSt(true, NRBusDefinition.cstJsonSourceName, requestJson, "/");

        NRBusDefinition busDefinition = getByName(sourceName);
        if (busDefinition == null)
            throw new NRException(new BEvent(eventNoDefinitionFound, sourceName));
        
        busDefinition.fromJson(requestJson);
        return busDefinition;
    }

    /**
     * Reference all sources
     */
    public void referenceSources(APISession apiSession, NRSourceFactory sourceFactory) {
        for (NRSource source : sourceFactory.getSources()) {
            source.loadBusinessDefinition(apiSession, this);
        }
    }

}
