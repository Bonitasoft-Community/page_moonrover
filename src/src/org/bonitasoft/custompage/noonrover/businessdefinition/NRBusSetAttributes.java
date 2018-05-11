package org.bonitasoft.custompage.noonrover.businessdefinition;

import java.util.HashSet;
import java.util.Set;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;

public class NRBusSetAttributes {

    public TYPECOLUMN type;
    public String name;
    public boolean unique;
    public boolean isIndex;
    private final Set<String> setColumns = new HashSet<String>();

    public NRBusSetAttributes(final boolean isIndex) {
        this.isIndex = isIndex;
    };

    @Override
    public String toString() {
        return name + "(" + setColumns + ")";
    }

    public void addColumns(final String colName) {
        setColumns.add(colName.toUpperCase());
    }

    public Set<String> getListColumns() {
        return setColumns;
    }

}
