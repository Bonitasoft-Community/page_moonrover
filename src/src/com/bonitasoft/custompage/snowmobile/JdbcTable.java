package com.bonitasoft.custompage.snowmobile;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.bonitasoft.custompage.snowmobile.SnowMobileAccess.ParametersCalcul;

public class JdbcTable {

    private final JdbcModel jdbcModel;
    /**
     * the complete Jdbc tableName, in lower case. This table name is use in the complete calcul.
     */
    private final String tableName;
    /**
     * keep the original table name, because when we want to query to search column, then this information is needed.
     */
    private final String sqlOriginaltableName;

    /**
     * in case of a sub table, the childTableName contains the name of the child. Example : table is "customer_address", childTableName is "address"
     */

    private String childTableName;

    public class JdbcColumn {

        // ColName in UPPER CASE
        public String colName;
        public int dataType;
        public int length;
        public boolean nullable;
        public boolean isForeignKey = false;
        // the column is a Reference Key to this table
        public String referenceTable;
        public String contraintsName;
        public JdbcTable jdbcTable;

        public JdbcColumn(final JdbcTable jdbcTable) {
            this.jdbcTable = jdbcTable;
        }

        public String getSqlType() {
            return "varchar";
        }

        public JdbcTable getJdbcTable() {
            return jdbcTable;
        };

        @Override
        public String toString() {
            return jdbcTable + "." + colName + "(" + dataType + ")";
        };

    }

    private List<JdbcColumn> listColumns = new ArrayList<JdbcColumn>();

    public class TableListOfColums {

        public String name;
        public boolean unique;
        public boolean isIndex;
        private final Set<String> setColumns = new HashSet<String>();

        public TableListOfColums(final boolean isIndex) {
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

    /**
     * index. Key must be in lower case
     */
    private final Map<String, TableListOfColums> indexes = new HashMap<String, TableListOfColums>();

    /**
     * constraints. Key must be in lower case
     */
    private final Map<String, TableListOfColums> constraints = new HashMap<String, TableListOfColums>();

    /**
     * in the BDM, an attribut can be declare as a Collection. Then, in the database, a sub table is created.
     * Key is the complete SQL tablename, like "customer_address" where address is the name of the data model field
     * Key is the table name, in lower case
     */
    private final Map<String, JdbcTable> collectionsTableName = new HashMap<String, JdbcTable>();

    /**
     * all index discover in the database
     */

    public JdbcTable(final JdbcModel jdbcModel, final String tableName, final String sqlOriginaltableName) {
        this.tableName = tableName;
        this.sqlOriginaltableName = sqlOriginaltableName;
        this.jdbcModel = jdbcModel;
    }

    public JdbcTable(final JdbcModel jdbcModel, final String tableName, final String sqlOriginaltableName,
            final String childTableName) {
        this.tableName = tableName;
        this.sqlOriginaltableName = sqlOriginaltableName;
        this.childTableName = childTableName;
        this.jdbcModel = jdbcModel;
    }

    /**
     * add a new Collection table
     *
     * @param parentTableName is the complete source like "customer"
     * @param childTableName is the name of the child, "address"
     *        The complete tableName is then source parentTableName+"_"+childTableName
     * @param collectionName
     */
    public void addCollectionTable(final JdbcModel jdbcModel, final String parentTableName,
            final String sqlOriginaltableName, final String childTableName) {
        collectionsTableName.put(
                parentTableName.toLowerCase() + "_" + childTableName.toLowerCase(),
                new JdbcTable(jdbcModel, parentTableName.toLowerCase() + "_" + childTableName.toLowerCase(),
                        sqlOriginaltableName, childTableName));
    }

    public JdbcTable getCollectionTable(final String parentTableName, final String childTableName,
            final ParametersCalcul parametersCalcul) {
        final String parentTable = parentTableName.toLowerCase();
        if (parametersCalcul.maxNumberOfCharacterForCollectionTable != -1
                && parentTable.length() > parametersCalcul.maxNumberOfCharacterForCollectionTable) {
            final String collectionTableName = parentTable.substring(0,
                    parametersCalcul.maxNumberOfCharacterForCollectionTable) + "_"
                    + childTableName.toLowerCase();
            JdbcTable jdbcTable = collectionsTableName.get(collectionTableName);
            if (jdbcTable != null) {
                return jdbcTable;
            }
            // let's have a look in the unknow table
            jdbcTable = jdbcModel.getCollectionUnknowChildTable().get(collectionTableName);
            return jdbcTable;

        }
        return collectionsTableName.get(parentTableName.toLowerCase() + "_" + childTableName.toLowerCase());
    }

    /**
     * use the connection to rebuild the datamodel
     *
     * @param con
     * @return
     */
    public String getContentFromConnection(final Connection con, final OperationStatus operationStatus) {
        String result = "";
        ResultSet rs = null;
        try {
            listColumns = new ArrayList<JdbcColumn>();
            boolean oneColumnFound = false;
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            rs = databaseMetaData.getColumns(null /* catalog */, null /* schema */, sqlOriginaltableName,
                    null /* columnNamePattern */);
            while (rs.next()) {
                String tableNameCol = rs.getString("TABLE_NAME");
                tableNameCol = tableNameCol == null ? "" : tableNameCol.toLowerCase();

                if (!tableNameCol.equals(tableName)) {
                    continue;
                }
                oneColumnFound = true;

                final JdbcColumn jdbcColumn = new JdbcColumn(this);
                jdbcColumn.colName = rs.getString("COLUMN_NAME");
                jdbcColumn.colName = jdbcColumn.colName == null ? "" : jdbcColumn.colName.toLowerCase();
                // don't keep in mind the system column
                if (jdbcColumn.colName.equals(GeneratorSql.cstColumnPersistenceId)
                        || jdbcColumn.colName.equals(GeneratorSql.cstColumnPersistenceVersion)) {
                    continue;
                }
                // this is a refence table : it should appears in the reference
                if (jdbcColumn.colName.endsWith(GeneratorSql.cstSuffixColumnPid)) {
                    // jdbcColumn.colName = jdbcColumn.colName.substring(0, jdbcColumn.colName.length()-4);
                    jdbcColumn.isForeignKey = true;
                }

                jdbcColumn.dataType = rs.getInt("DATA_TYPE");
                jdbcColumn.length = rs.getInt("COLUMN_SIZE");
                if (jdbcColumn.dataType == java.sql.Types.VARCHAR && jdbcColumn.length > 100000) {
                    jdbcColumn.dataType = -333; // special marker for a Text
                }
                if (jdbcColumn.colName.equals("style") || jdbcColumn.colName.equals("string2composition")) {
                    System.out.println(
                            "JdbcTable : jdbcColumn[" + jdbcColumn.colName + "] type[" + jdbcColumn.dataType + "]");
                }
                jdbcColumn.nullable = "YES".equals(rs.getString("IS_NULLABLE"));
                listColumns.add(jdbcColumn);
            }
            rs.close();

            if (!oneColumnFound) {
                operationStatus.addErrorMsg("No table[" + sqlOriginaltableName
                        + "] found: the table is detected on the CATALOG table, but no colum are found in the CATALOG column");
            }

            // index & constraints
            // i==0 => unique = Constraint
            // i==1 => index

            rs = databaseMetaData
                    .getIndexInfo(null /* String catalog */, null /* String schema */, sqlOriginaltableName,
                            false /* unique */, false /* boolean approximate */);
            while (rs.next()) {
                String indexName = rs.getString("INDEX_NAME");
                indexName = indexName.toLowerCase();
                if (indexName.endsWith("_pkey")) {
                    continue; // this is the primary key
                }
                final String columnName = rs.getString("COLUMN_NAME");
                // String indexQualifier = rs.getString("INDEX_QUALIFIER");
                final boolean nonUnique = rs.getBoolean("NON_UNIQUE");
                // nonUnique = true => index (else constraints)
                TableListOfColums oneListItem = nonUnique ? indexes.get(indexName) : constraints.get(indexName);
                if (oneListItem == null) {
                    oneListItem = new TableListOfColums(true);
                    if (nonUnique) {
                        indexes.put(indexName.toLowerCase(), oneListItem);
                    } else {
                        constraints.put(indexName.toLowerCase(), oneListItem);
                    }

                }
                oneListItem.name = indexName;
                oneListItem.unique = !nonUnique;
                oneListItem.addColumns(columnName);
            }
            rs.close();

            // constraints and reference key
            rs = databaseMetaData.getImportedKeys(null, null, sqlOriginaltableName);

            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                columnName = columnName.toLowerCase();
                // search the column
                for (final JdbcColumn jdbcColumn : listColumns) {
                    if (jdbcColumn.colName.equals(columnName)) {
                        jdbcColumn.referenceTable = rs.getString("PKTABLE_NAME");
                        // all table in lower case
                        jdbcColumn.referenceTable = jdbcColumn.referenceTable.toLowerCase();
                        jdbcColumn.contraintsName = rs.getString("FK_NAME");
                    }
                    //   	String pk_column = rs.getString("PKCOLUMN_NAME");
                    //    String constraint_name = rs.getString("FK_NAME");
                }
            }
            rs.close();

            /*
             * ResultSet getCrossReference(String parentCatalog,
             * String parentSchema,
             * String parentTable,
             * String foreignCatalog,
             * String foreignSchema,
             * String foreignTable)
             * throws SQLException
             */

            // now, explore all collectionName
            for (final JdbcTable collectionName : collectionsTableName.values()) {
                result += collectionName.getContentFromConnection(con, operationStatus);
            }

            // let's check all column
            for (final JdbcColumn jdbcColumn : listColumns) {
                if (jdbcColumn.isForeignKey && jdbcColumn.contraintsName == null) {
                    operationStatus.addErrorMsg("Table[" + tableName + "] colum[" + jdbcColumn.colName
                            + "] is a Foreign key without constraint name...");
                    jdbcColumn.isForeignKey = false;
                }
            }
        } catch (final Exception e) {

            result = e.toString();
            if (rs != null) {
                try {
                    rs.close();
                } catch (final SQLException e1) {
                }
            } ;
        }

        return result;

    }

    public String getTableName() {
        return tableName;
    }

    public String getChildTableName() {
        return childTableName;
    }

    public List<JdbcColumn> getListColumns() {
        return listColumns;
    }

    public Map<String, TableListOfColums> getMapIndexes() {
        return indexes;
    }

    public Map<String, TableListOfColums> getMapConstraints() {
        return constraints;
    }

    public Map<String, JdbcTable> getMapTables() {
        return collectionsTableName;
    }

    /**
     * search in the constaint one who define a constraint on this column (expected a foreign key constraint)
     *
     * @return
     */
    public String getContraintsContainsField(final String colName) {
        for (final String name : constraints.keySet()) {
            for (final String colNameInConstraint : constraints.get(name).getListColumns()) {
                // this is the constraint we look for
                if (colNameInConstraint.equals(colName)) {
                    return name;
                }
            }
        }
        return null;
    }

    public JdbcColumn addColName(final String colName) {
        final JdbcColumn jdbcColumn = new JdbcColumn(this);
        jdbcColumn.colName = colName;
        listColumns.add(jdbcColumn);
        return jdbcColumn;

    }

    @Override
    public String toString() {
        return tableName;
    };
}
