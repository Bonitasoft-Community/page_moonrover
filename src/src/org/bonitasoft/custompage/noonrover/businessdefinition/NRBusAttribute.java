package org.bonitasoft.custompage.noonrover.businessdefinition;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;

/**
 * The business Object contains a list of Attributes.
 * BDM : attributes
 * TABLE : Columns
 * PROCESS : Variables
 */
public class NRBusAttribute {

    // ColName in UPPER CASE
    public NRBusDefinition busDefinition;
    public TYPECOLUMN type;
    public String name;
    public int length;
    public boolean nullable;
    public boolean isForeignKey = false;
    // the column is a Reference Key to this table
    public String referenceTable;
    public String contraintsName;
    public String tableName;

    protected NRBusAttribute(NRBusDefinition busDefinition, String tableName, String name, TYPECOLUMN type) {
        this.busDefinition = busDefinition;
        this.tableName = tableName;
        this.name = name;
        this.type = type;
    }
}
