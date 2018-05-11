package com.bonitasoft.custompage.snowmobile;

import java.util.HashSet;
import java.util.Set;

import com.bonitasoft.custompage.snowmobile.BdmBusinessObject.BdmListOfFields;
import com.bonitasoft.custompage.snowmobile.JdbcTable.JdbcColumn;
import com.bonitasoft.custompage.snowmobile.SnowMobileAccess.ParametersCalcul;

/**
 * this class genere some SQL
 */
public class GeneratorSql {

    public BdmContent bdmContent;
    public JdbcModel metaModel;
    public OperationStatus operationStatus;
    public ParametersCalcul parametersCalcul;
    private final HashSet<String> keepDropTables = new HashSet<String>();

    public final static String cstSuffixColumnOrder = "_order";
    public final static String cstSuffixColumnPid = "_pid";
    public final static String cstSuffixColumnPersistenceId = "_persistenceid";
    public final static String cstColumnPersistenceId = "persistenceid";
    public final static String cstColumnPersistenceVersion = "persistenceversion";

    public GeneratorSql(final ParametersCalcul parametersCalcul, final BdmContent bdmContent, final JdbcModel metaModel,
            final OperationStatus operationStatus) {
        this.bdmContent = bdmContent;
        this.metaModel = metaModel;
        this.operationStatus = operationStatus;
        this.parametersCalcul = parametersCalcul;
    }

    protected void addComment(final String comment) {
        operationStatus.addCommentUpdate(comment);

    }

    /* ----------------------------------------------------- */
    /*                                                       */
    /* Table */
    /*                                                       */
    /* ----------------------------------------------------- */

    protected void sqlGenerateTable(final BdmBusinessObject bdmContent) {
        final StringBuffer sqlCreate = new StringBuffer();
        sqlCreate.append("create table " + bdmContent.tableName + " (\n");
        sqlCreate.append("  " + cstColumnPersistenceId + " bigint NOT NULL,\n");
        sqlCreate.append("  " + cstColumnPersistenceVersion + " bigint,\n");

        for (final BdmField bdmField : bdmContent.listFields) {
            if (!bdmField.isCollection()) {
                sqlCreate.append("  " + getSqlCreate(bdmField) + ",\n");
            }
        }
        sqlCreate
                .append("  constraint " + bdmContent.tableName + "_pkey primary key(" + cstColumnPersistenceId + ")\n");

        sqlCreate.append(");");

        operationStatus.addSqlUpdate(sqlCreate.toString(), false);

        // add all index
        for (final BdmListOfFields bdmIndex : bdmContent.listIndexes) {
            sqlCreateIndex(bdmIndex);
        }
        // add all constraint

        for (final BdmListOfFields bdmIndex : bdmContent.listConstraints) {
            sqlCreateIndex(bdmIndex);
        }

        // generate collection
        for (final BdmField bdmField : bdmContent.listFields) {
            if (bdmField.isCollection() && !bdmField.isComposition()) {
                sqlCreateCollection(bdmField);
            }
        }
    }

    public void sqlDropTable(final String tableName) {
        operationStatus.addSqlUpdate("drop table " + tableName + ";", parametersCalcul.commentDropTable);
        keepDropTables.add(tableName);
    }

    public void sqlDropTable(final String tableName, final boolean isComment) {
        operationStatus.addSqlUpdate("drop table " + tableName + ";", isComment);
        keepDropTables.add(tableName);
    }

    public HashSet<String> getDropTables() {
        return keepDropTables;
    };

    /* ----------------------------------------------------- */
    /*                                                       */
    /* Column */
    /*                                                       */
    /* ----------------------------------------------------- */

    public void sqlCreateColumn(final BdmField bdmField, final boolean isComment) {
        operationStatus.addSqlUpdate("alter table " + bdmField.getBusinessObject().getSqlTableName() + " add "
                + getSqlCreate(bdmField) + ";", isComment);
        // is this is a Aggregation(foreign Key OR a Composition Unique ==> Add the foreign key (a composition - non collection is an agregation in fact
        if (bdmField.isAggregation() || bdmField.isComposition() && !bdmField.isCollection()) {
            // add the contrainst
            sqlCreateForeignConstraint(bdmField, isComment);
        }
    }

    public void sqlColumnToNullable(final BdmField bdmField) {
        operationStatus.addSqlUpdate(
                " alter table "
                        + getSqlTableName(bdmField)
                        + " alter column "
                        + getSqlName(bdmField, false)
                        + " "
                        + getSqlType(bdmField)
                        + " "
                        + " NULL",
                false);
    }

    public void sqlColumnToNotNullable(final BdmField bdmField) {
        operationStatus.addSqlUpdate(
                " alter table "
                        + getSqlTableName(bdmField)
                        + " alter column "
                        + getSqlName(bdmField, false)
                        + " "
                        + getSqlType(bdmField)
                        + " "
                        + " NOT NULL",
                false);

    }

    public void sqlColumnLengthChange(final BdmField bdmField) {
        // ALTER TABLE [mytable] ALTER COLUMN data varchar(4000);
        operationStatus.addSqlUpdate(
                "alter table " + getSqlTableName(bdmField) + " alter column " + getSqlCreate(bdmField) + ";", false);
    }

    public void sqlColumnTypeChange(final BdmField bdmField) {
        // try to alter it
        operationStatus.addSqlUpdate(
                "alter table " + getSqlTableName(bdmField) + " alter column " + getSqlCreate(bdmField) + ";",
                !parametersCalcul.calculChangeTypeColumn);
        // else drop it
        sqlDropColumn(bdmField.getBusinessObject().getSqlTableName(), bdmField.getSqlColName(),
                parametersCalcul.calculChangeTypeColumn);
        sqlCreateColumn(bdmField, parametersCalcul.calculChangeTypeColumn);
    }

    public void sqlIsForeignKey(final BdmField bdmField) {
        // the name change : before it was "mycustomer", now it's "mycustomer_pid"

        // operationStatus.addSqlUpdate("alter table "+ bdmField.getBusinessObject().getSqlTableName()+" drop column "+bdmField.getSqlColName()+";", false);
        operationStatus.addSqlUpdate("alter table " + bdmField.getBusinessObject().getSqlTableName() + " create column "
                + getSqlCreate(bdmField) + ";", false);
        sqlCreateForeignConstraint(bdmField, false);
    }

    public void sqlIsNotForeignKey(final BdmField bdmField, final JdbcColumn jdbcColumn) {
        sqlDropForeignConstraint(bdmField, jdbcColumn.contraintsName);
        // the new field may be a new foreign field to a new table (so end by _pid) or a new field.
        // drop the foreign name
        operationStatus.addSqlUpdate("alter table " + bdmField.getBusinessObject().getSqlTableName() + " drop column "
                + jdbcColumn.colName + ";", false);
    }

    /**
     * remove a column
     *
     * @param tableName
     * @param colName
     */
    public void sqlDropColumn(final String tableName, final String colName, final boolean isComment) {
        operationStatus.addSqlUpdate("alter table " + tableName + " drop column " + colName + ";", isComment);

    }

    /* ----------------------------------------------------- */
    /*                                                       */
    /* Collection */
    /*                                                       */
    /* ----------------------------------------------------- */

    public void sqlColumnToCollection(final BdmField bdmField) {
        final CollectionAttributes collectionAttributes = sqlCreateCollection(bdmField);

        // attention : the bdmField.getSqlColName can contains _pid if this is a relation, and we dont want that to create the table name

        // copy all data
        final StringBuffer createTableScript = new StringBuffer();
        createTableScript
                .append("insert into " + collectionAttributes.tableName + " (" + collectionAttributes.columnId + ",");
        createTableScript.append(collectionAttributes.columValue + ",");
        createTableScript.append(collectionAttributes.columnOrder + ") ");
        createTableScript.append("select " + cstColumnPersistenceId + ", " + bdmField.getSqlColName() + ",1 from "
                + bdmField.getSqlTableName());

        operationStatus.addSqlUpdate(createTableScript.toString(), false);

        // 	field can be drop now
        sqlDropColumn(bdmField.getSqlTableName(), bdmField.getSqlColName(), parametersCalcul.commentDropColumn);
    }

    public void sqlCollectionToColumn(final BdmField bdmField) {
        final String collectionTableName = bdmField.getSqlReferenceTable() + "_" + bdmField.getSqlColName();
        sqlCreateCollection(bdmField);
        operationStatus.addSqlUpdate(
                "update " + getSqlName(bdmField, true) + " set (select " + getSqlName(bdmField, true) + " from "
                        + bdmField.getSqlReferenceTable()
                        + " where order = 1)",
                false);
        sqlDropTable(collectionTableName);
    }

    public String getCollectionTableName(final BdmField bdmField) {
        // if this is a relation, then the slqColName will finish by _pid and we don't want that
        if (bdmField.isRelationField) {
            return bdmField.getBusinessObject().getSqlTableName() + "_" + bdmField.name.toLowerCase();
        } else {
            return bdmField.getBusinessObject().getSqlTableName() + "_" + bdmField.getSqlColName();
        }
    }

    /**
     * create the collection table
     *
     * @param bdmField
     */
    // the collection table attributes change if the field is a foreign key or not ( ! ) so the generator will return the differentes attribut of the table
    public class CollectionAttributes {

        public String tableName;
        public String columnId;
        public String columValue;
        public String columnOrder;
    }

    public CollectionAttributes sqlCreateCollection(final BdmField bdmField) {
        final CollectionAttributes collectionAttributes = new CollectionAttributes();
        collectionAttributes.tableName = getCollectionTableName(bdmField);
        if (bdmField.isRelationField) {
            collectionAttributes.columnId = bdmField.getSqlTableName() + cstSuffixColumnPid;
            collectionAttributes.columValue = bdmField.getSqlReferenceTable() + cstSuffixColumnPid;
        } else {
            collectionAttributes.columnId = bdmField.getSqlTableName() + cstSuffixColumnPersistenceId;
            collectionAttributes.columValue = bdmField.getSqlColName();
        }
        collectionAttributes.columnOrder = bdmField.name + cstSuffixColumnOrder;

        final ConversionItem conversionItem = getConversionItemFromType("INTEGER");

        final StringBuffer sqlCreate = new StringBuffer();
        sqlCreate.append("create table " + collectionAttributes.tableName + " (\n");
        sqlCreate.append("  " + collectionAttributes.columnId + " bigint NOT NULL,\n");

        sqlCreate.append("  " + collectionAttributes.columValue + " " + getSqlType(bdmField) + ",\n");

        sqlCreate.append("  " + collectionAttributes.columnOrder + " "
                + (conversionItem == null ? "integer" : conversionItem.jdbcSqlType) + " not null,\n");

        sqlCreate.append("  constraint " + collectionAttributes.tableName + "_pkey primary key ("
                + collectionAttributes.columnId + ","
                + collectionAttributes.columnOrder + "),\n");
        sqlCreate.append(
                "  constraint " + collectionAttributes.tableName + "_fk foreign key (" + collectionAttributes.columnId
                        + ") references " + bdmField.getBusinessObject().getSqlTableName() + "("
                        + cstColumnPersistenceId + ")\n);");

        operationStatus.addSqlUpdate(sqlCreate.toString(), false);

        if (bdmField.isAggregation()) {
            // add the foreign key
            final StringBuffer script = new StringBuffer();
            script.append("alter table " + collectionAttributes.tableName);
            script.append(" add constraints fk_" + collectionAttributes.columValue + " foreign key("
                    + collectionAttributes.columValue + ") reference "
                    + bdmField.getSqlReferenceTable() + " (" + cstColumnPersistenceId + ");");
            operationStatus.addSqlUpdate(script.toString(), false);
        }
        return collectionAttributes;
    }

    /* ----------------------------------------------------- */
    /*                                                       */
    /* Index */
    /*                                                       */
    /* ----------------------------------------------------- */

    public void sqlCreateIndex(final BdmListOfFields bdmIndex) {
        operationStatus.addSqlUpdate("create index " + bdmIndex.name.toLowerCase() + " on "
                + bdmIndex.getBusinessObject().getSqlTableName() + " ("
                + getListFields(bdmIndex.getListFields()) + ");", false);
    }

    public void sqlDropIndex(final String tableName, final String indexName) {
        operationStatus.addSqlUpdate("drop index " + indexName.toLowerCase() + " on " + tableName + ";",
                parametersCalcul.commentDropIndex);
    }

    /* ----------------------------------------------------- */
    /*                                                       */
    /* Constraint */
    /*                                                       */
    /* ----------------------------------------------------- */

    public void sqlCreateConstraint(final BdmListOfFields bdmConstraint) {
        // ALTER TABLE Customer ADD CONSTRAINT Con_First UNIQUE (Address);
        final StringBuffer script = new StringBuffer();
        script.append("alter table " + bdmConstraint.getBusinessObject().getSqlTableName());
        script.append(" add constraint " + bdmConstraint.getSqlName());
        script.append(" (" + getListFields(bdmConstraint.getListFields()) + ");");
        operationStatus.addSqlUpdate(script.toString(), false);
    }

    public void sqlDropConstraint(final String tableName, final String constraintName) {
        final StringBuffer script = new StringBuffer();
        script.append("alter table " + tableName);
        script.append(" drop constraint " + constraintName + ";");

        operationStatus.addSqlUpdate(script.toString(), parametersCalcul.commentDropConstraint);

    }

    public void sqlCreateForeignConstraint(final BdmField bdmField, final boolean isComment) {
        final StringBuffer script = new StringBuffer();

        script.append("alter table " + bdmField.getBusinessObject().getSqlTableName());
        script.append(" add constraints fk_" + bdmField.getSqlColName() + " foreign key(" + bdmField.getSqlColName()
                + ") reference "
                + bdmField.getSqlReferenceTable() + " (" + cstColumnPersistenceId + ");");

        operationStatus.addSqlUpdate(script.toString(), isComment);
    }

    public void sqlDropForeignConstraint(final BdmField bdmField, final String constraintName) {
        sqlDropConstraint(bdmField.getBusinessObject().getSqlTableName(), constraintName);

    }

    /*----------------------------------------------------- */
    /*                                                      */
    /* Private toolbox method */
    /*                                                      */
    /*----------------------------------------------------- */

    private String getSqlTableName(final BdmField bdmField) {
        return bdmField.getBusinessObject().getSqlTableName();
    }

    private String getSqlName(final BdmField bdmField, final boolean completeName) {
        if (completeName) {
            return bdmField.getBusinessObject().getSqlTableName() + "." + bdmField.getSqlColName();
        }
        return bdmField.getSqlColName();
    }

    private String getSqlCreate(final BdmField bdmField) {
        return bdmField.getSqlColName() + " " + getSqlType(bdmField);
    }

    /*
     * from a set of String, return it on a list field1,field2,field3
     */
    private String getListFields(final Set<String> listFields) {
        String list = "";
        for (final String field : listFields) {
            list += field + ",";
        }
        if (list.length() > 0) {
            return list.substring(0, list.length() - 1);
        }
        return list;
    }

    /*----------------------------------------------------- */
    /*                                                      */
    /* Conversion */
    /*                                                      */
    /*----------------------------------------------------- */
    /**
     * conversion
     */
    public class ConversionItem {

        public String bdmType;
        public String jdbcSqlType;
        public int jdbcType;
        public boolean lengthIsImportant = false;

        public ConversionItem(final String btmType, final String jdbcSqlType, final int jdbcType,
                final boolean lengthIsImportant) {
            bdmType = btmType;
            this.jdbcSqlType = jdbcSqlType;
            this.jdbcType = jdbcType;
            this.lengthIsImportant = lengthIsImportant;
        }
    }

    private final ConversionItem[] postgresConversion = new ConversionItem[] {
            new ConversionItem("STRING", "varying", java.sql.Types.VARCHAR, true),
            new ConversionItem("TEXT", "text", -333, true),
            new ConversionItem("BOOLEAN", "boolean", java.sql.Types.BIT, false),
            new ConversionItem("LONG", "bigint", java.sql.Types.BIGINT, false),
            new ConversionItem("INTEGER", "integer", java.sql.Types.INTEGER, false),
            new ConversionItem("FLOAT", "real", java.sql.Types.REAL, false),
            new ConversionItem("DOUBLE", "double precision", java.sql.Types.DOUBLE, false),
            new ConversionItem("DATE", "timestamp without time zone", java.sql.Types.TIMESTAMP, false),
            new ConversionItem("", "integer", java.sql.Types.INTEGER, false)

    };
    private final ConversionItem[] h2Conversion = new ConversionItem[] {
            new ConversionItem("STRING", "varchar", java.sql.Types.VARCHAR, true),
            new ConversionItem("TEXT", "text", -333, true),
            new ConversionItem("BOOLEAN", "boolean", java.sql.Types.BIT, false),
            new ConversionItem("LONG", "bigint", java.sql.Types.BIGINT, false),
            new ConversionItem("INTEGER", "integer", java.sql.Types.INTEGER, false),
            new ConversionItem("FLOAT", "real", java.sql.Types.REAL, false),
            new ConversionItem("DOUBLE", "double precision", java.sql.Types.DOUBLE, false),
            new ConversionItem("DATE", "date", java.sql.Types.TIMESTAMP, false),
            new ConversionItem("", "integer", java.sql.Types.INTEGER, false)

    };
    private final ConversionItem[] sqlConversion = new ConversionItem[] {
            new ConversionItem("STRING", "varchar", java.sql.Types.VARCHAR, true),
            new ConversionItem("TEXT", "text", java.sql.Types.VARCHAR, true),
            new ConversionItem("BOOLEAN", "boolean", java.sql.Types.BIT, false),
            new ConversionItem("LONG", "bigint", java.sql.Types.BIGINT, false),
            new ConversionItem("INTEGER", "integer", java.sql.Types.INTEGER, false),
            new ConversionItem("FLOAT", "real", java.sql.Types.REAL, false),
            new ConversionItem("DOUBLE", "double", java.sql.Types.DOUBLE, false),
            new ConversionItem("DATE", "timestamp", java.sql.Types.TIMESTAMP, false),
            new ConversionItem("", "integer", java.sql.Types.INTEGER, false)

    };

    /**
     * get the sql type of one field, according the database
     *
     * @param bdmField
     * @return
     */
    public String getSqlType(final BdmField bdmField) {
        final ConversionItem oneConversion = getConversionItemFromType(bdmField.fieldType);
        if (oneConversion != null) {
            return oneConversion.jdbcSqlType
                    + (oneConversion.lengthIsImportant ? "(" + bdmField.fieldLength + ")" : "");
        }
        operationStatus.addErrorMsg("Type unknow [" + bdmField.fieldType + "] BusinessObject["
                + bdmField.getBusinessObject().name + "] field[" + bdmField.name
                + "]");
        return "varchar(10)";
    }

    public String getSqlType(final JdbcColumn jdbcColumn) {
        final ConversionItem[] conversionTable = getConversionTable(metaModel.getDatabaseProductName());
        for (final ConversionItem oneConversion : conversionTable) {
            if (oneConversion.jdbcType == jdbcColumn.dataType) {
                return oneConversion.jdbcSqlType
                        + (oneConversion.lengthIsImportant ? "(" + jdbcColumn.length + ")" : "");
            }
        }
        operationStatus.addErrorMsg("JdbcType unknow [" + jdbcColumn.dataType + "] JdbcTable["
                + jdbcColumn.getJdbcTable().getTableName() + "] field["
                + jdbcColumn.colName + "]");
        return "varchar";
    }

    public ConversionItem getConversionItemFromType(final String type) {
        final ConversionItem[] conversionTable = getConversionTable(metaModel.getDatabaseProductName());
        for (final ConversionItem oneConversion : conversionTable) {
            if (oneConversion.bdmType.equals(type)) {
                return oneConversion;
            }
        }
        return null;
    }

    /**
     * @param databaseproductName
     * @return
     */
    public ConversionItem[] getConversionTable(final String databaseproductName) {
        if ("PostgreSQL".equals(databaseproductName)) {
            return postgresConversion;
        }
        if ("H2".equals(databaseproductName)) {
            return h2Conversion;
        }

        operationStatus.addErrorMsg("No conversion for base[" + databaseproductName + "] use sqlDefault");
        return sqlConversion;
    }
}
