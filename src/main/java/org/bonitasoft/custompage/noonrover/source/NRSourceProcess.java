package org.bonitasoft.custompage.noonrover.source;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.engine.session.APISession;

public class NRSourceProcess extends NRSource {

    public NRSource.SourceStatus loadBusinessDefinition(APISession apiSession,
            NRBusDefinitionFactory businessFactory) {
        return new SourceStatus();

    }

}
