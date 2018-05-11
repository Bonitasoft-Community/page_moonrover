package org.bonitasoft.custompage.noonrover.source;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEventFactory;

public abstract class NRSource {

    public static class SourceStatus {

        public List<BEvent> listEvents = new ArrayList<BEvent>();
        public List<NRBusDefinition> listBusinessDefinition = new ArrayList<NRBusDefinition>();

        public void add(SourceStatus sourceStatus) {
            this.listEvents.addAll(sourceStatus.listEvents);
            this.listBusinessDefinition.addAll(sourceStatus.listBusinessDefinition);
        }

        public Map<String, Object> getJson() {
            Map<String, Object> result = new HashMap<String, Object>();

            List<Map<String, Object>> listBusiness = new ArrayList<Map<String, Object>>();
            Collections.sort(listBusinessDefinition, new Comparator<NRBusDefinition>() {

                public int compare(NRBusDefinition s1,
                        NRBusDefinition s2) {
                    return ((NRBusDefinition) s1).getDisplayName().compareTo(((NRBusDefinition) s2).getDisplayName());
                }
            });

            for (NRBusDefinition businessDefinition : listBusinessDefinition) {
                listBusiness.add(businessDefinition.getJson());
            }
            result.put("listsources", listBusiness);
            result.put("listevents", BEventFactory.getHtml(listEvents));
            return result;
        }
    }

    /**
     * the source has to return all the BusinessDefinitionList.
     * 
     * @param businessFactory
     * @return
     */
    public abstract SourceStatus getListBusinessDefinition(APISession apiSession,
            NRBusDefinitionFactory businessFactory);
}
