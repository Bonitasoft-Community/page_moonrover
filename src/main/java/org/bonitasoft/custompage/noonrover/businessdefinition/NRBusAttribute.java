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

    // the column is a Reference Key to this table
    public String referenceTable;
    public String contraintsName;
    public String tableName;
    
    // the attribut is a collection, a different sub table is created
    public boolean isCollection;
    
    // the attribut is a relation, then the item contains only an ID to the item
    // note : this can be combined by a collection
    public String relationTableName =null;
    
    protected NRBusAttribute(NRBusDefinition busDefinition, String tableName, String name, TYPECOLUMN type, boolean isCollection) {
        this.busDefinition = busDefinition;
        this.tableName = tableName;
        this.name = name;
        this.type = type;
        this.isCollection = isCollection;
    }
}
