package org.bonitasoft.custompage.noonrover.source;

import java.util.ArrayList;
import java.util.List;

/* -------------------------------------------------------------------- */
/*                                                                      */
/* SourceFactory */
/*                                                                      */
/* Multiple source can be set by the NoonRover : BDM, Process, External */
/* Database */
/* -------------------------------------------------------------------- */

public class NRSourceFactory {

    private static NRSourceFactory sourceFactory = new NRSourceFactory();

    public boolean isInitialised = false;

    public List<NRSource> listSources = new ArrayList<NRSource>();

    public static NRSourceFactory getInstance() {
        if (!sourceFactory.isInitialised)
            sourceFactory.initialise();
        return sourceFactory;
    }

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Manage different source */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    /**
     * Initialise : prepare the list of all available source
     */
    public void initialise() {
        if (!isInitialised) {
            listSources.add(new NRSourceBDM());
            listSources.add(new NRSourceProcess());
            //  listSources.add(new NRSourceDatabase());
            isInitialised = true;
        }
        // read the Properties and all additionnal source
    }

    public void addSource(NRSource source) {
        listSources.add(source);
        // save the source in the properties to be able to reload it
    }

    public List<NRSource> getSources() {
        return listSources;
    }
    
    
}
