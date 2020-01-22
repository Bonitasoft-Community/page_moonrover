package org.bonitasoft.custompage.noonrover.test;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI.ParameterSource;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.api.LoginAPI;
import org.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestBdm {

    public APISession apiSession = null;

    @Before
    public void setUp() throws Exception {
        final Map<String, String> map = new HashMap<String, String>();

        map.put("server.url", "http://localhost:61547");
        map.put("application.name", "bonita");
        APITypeManager.setAPITypeAndParams(ApiAccessType.HTTP, map);

        // Set the username and password
        // final String username = "helen.kelly";
        final String username = "walter.bates";
        final String password = "bpm";

        // get the LoginAPI using the TenantAPIAccessor
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();

        // log in to the tenant to create a session
        // apiSession = loginAPI.login(username, password);

    }

    @Test
    public void testBdm() {
        if (apiSession != null) {
            NoonRoverAccessAPI noonRoverAccessAPI = NoonRoverAccessAPI.getInstance();
            ParameterSource parameterSource = ParameterSource.getInstanceFromJson(apiSession,
                    "{\"request\":{\"name\":\"fr.edf.model.EvenementSte\",\"selection\":{\"name\":\"findBycaseId\",\"type\":\"FIND\",\"parameters\":[{\"visible\":true,\"name\":\"caseId\",\"type\":\"NUM\",\"value\":11001,\"operator\":\"EQUALS\"}]},\"result\":{\"columns\":[{\"visible\":true,\"name\":\"numSemaine\",\"sum\":false,\"groupby\":false,\"type\":\"NUM\"},{\"visible\":true,\"name\":\"creePar\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"quart\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"caseId\",\"sum\":false,\"groupby\":false,\"type\":\"NUM\"},{\"visible\":true,\"name\":\"dateEvenement\",\"sum\":false,\"groupby\":false,\"type\":\"DATE\"},{\"visible\":true,\"name\":\"tranche\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"se\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"domaineExpl\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"groupe\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"evenementTitre\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"evenementCumul\",\"sum\":false,\"groupby\":false,\"type\":\"BOOLEAN\"},{\"visible\":true,\"name\":\"description\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"conduite\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"nature\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"amorcageDelai\",\"sum\":false,\"groupby\":false,\"type\":\"NUM\"},{\"visible\":true,\"name\":\"amorcageUnit\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"type\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"reparationDelai\",\"sum\":false,\"groupby\":false,\"type\":\"NUM\"},{\"visible\":true,\"name\":\"reparationUnit\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"etatRepli\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"dateButeeSTE\",\"sum\":false,\"groupby\":false,\"type\":\"DATE\"},{\"visible\":true,\"name\":\"dateFinIndisponibilite\",\"sum\":false,\"groupby\":false,\"type\":\"DATE\"},{\"visible\":true,\"name\":\"descriptionFinIndisponibilite\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"constatRex\",\"sum\":false,\"groupby\":false,\"type\":\"NUM\"},{\"visible\":true,\"name\":\"verifierPar\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"verifierDate\",\"sum\":false,\"groupby\":false,\"type\":\"DATE\"},{\"visible\":true,\"name\":\"validerPar\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"},{\"visible\":true,\"name\":\"validerDate\",\"sum\":false,\"groupby\":false,\"type\":\"DATE\"},{\"visible\":true,\"name\":\"stateEvent\",\"sum\":false,\"groupby\":false,\"type\":\"STRING\"}],\"showAll\":true,\"typeresult\":\"TABLE\"},\"startindex\":0,\"maxresults\":1000}}");
            noonRoverAccessAPI.executeRequest(parameterSource, null);
        }
    }

}
