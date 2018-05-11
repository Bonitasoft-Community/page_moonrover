package com.bonitasoft.custompage.snowmobile;

/**
 * BdmField. One file in the BusinessDataModel object
 */

public class BdmField {

    // None is here to help development, to give a value in case this is not a relation
    // AGGREGATION is a Foreign Key
    // COMPOSITION is a child table
    enum enumRelationType {
        COMPOSITION, AGGREGATION
    };

    /**
    	 *
    	 */
    public boolean isRelationField;
    public enumRelationType relationType;
    public String name;
    public boolean nullable;
    public boolean collection;
    /**
     * in case of relation, this field is the reference field
     */
    public String reference;
    String referenceSqlTable;
    public String fieldType;
    public int fieldLength;

    private final BdmBusinessObject businessObject;

    public BdmField(final BdmBusinessObject bdmBusinessObject) {
        businessObject = bdmBusinessObject;
        assert bdmBusinessObject != null;
    };

    public String getLogId() {
        return name + "(" + fieldType
                + (collection ? "-collection" : "")
                + ") ";
    }

    @Override
    public String toString() {
        return name + "(" + fieldType + ") " + (nullable ? "null" : "")
                + (collection ? "-collection" : "-notcollection")
                + (isComposition() ? "-compo" : "-notcompo") + (isAggregation() ? "-aggre" : "-notaggre");
    }

    /**
     * return the column name in lower case
     *
     * @return
     */
    public String getSqlColName() {
        return name.toLowerCase() + (isRelationField ? GeneratorSql.cstSuffixColumnPid : "");
    }

    public String getFieldName() {
        return name;
    }

    public String getSqlCompleteColName() {
        return businessObject.getSqlTableName() + "." + name.toLowerCase()
                + (isRelationField ? GeneratorSql.cstSuffixColumnPid : "");
    }

    public String getSqlTableName() {
        return businessObject.getSqlTableName();
    }

    public String getReference() {
        return reference;
    };

    public String getSqlReferenceTable() {
        return referenceSqlTable == null ? null : referenceSqlTable.toLowerCase();
    };

    public BdmBusinessObject getBusinessObject() {
        return businessObject;
    }

    public boolean isAggregation() {
        return BdmField.enumRelationType.AGGREGATION.equals(relationType);
    }

    public boolean isComposition() {
        return BdmField.enumRelationType.COMPOSITION.equals(relationType);
    }

    public boolean isCollection() {
        return collection;
    }

}
