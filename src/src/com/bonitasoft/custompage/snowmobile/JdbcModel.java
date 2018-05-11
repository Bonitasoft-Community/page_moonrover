package com.bonitasoft.custompage.snowmobile;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JdbcModel {

    /**
     * the setTable contains only the MASTER table. Table like "customer_address" is consider as a subTable of customer.
     */
    private Map<String, JdbcTable> setTables;
    private String databaseProductName;

    /**
     * reference all the ChildTable : a child table contains a _
     * Theses tables should be added in each main table EXCEPT one situation : if the parent table has a too long name, it's not possible to
     * register it in the main table : table in database is "ThisTableNameI" where the model is "ThisTableNameIsTooLong".
     * And the table name "ThisTableNameI" can be use for two model : "ThisTableNameIsTooLong" and "ThisTableNameIsVeryVeryTooLong"
     * NOTA : key is a table name in LOWER CASE (not the Original Sql Name)
     */
    public Map<String, JdbcTable> collectionUnknowChildTables = new HashMap<String, JdbcTable>();

    public void readFromConnection(final Connection con, final OperationStatus operationStatus) {
        setTables = new HashMap<String, JdbcTable>();
        /**
         * keep the collection of orignal Name.
         * The table used after in in LOWER CASE.
         */
        final List<String> collectionChildTableSqlOriginalTableName = new ArrayList<String>();
        try {
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            databaseProductName = databaseMetaData.getDatabaseProductName();
            final String[] types = new String[] { "TABLE" };
            final ResultSet rs = databaseMetaData.getTables(null /* catalog */, null /* schemaPattern */,
                    null /* tableNamePattern */, types /* types */);
            while (rs.next()) {
                final String sqlOriginaltableName = rs.getString("TABLE_NAME");

                // TableName is set in LOWER CASE : in H2, all table name are UPPER CASE for example
                final String tableName = sqlOriginaltableName == null ? "" : sqlOriginaltableName.toLowerCase();
                final String tableType = rs.getString("TABLE_TYPE");
                if (tableName.indexOf("_") != -1) {
                    // we are on a Collection table.
                    collectionChildTableSqlOriginalTableName.add(sqlOriginaltableName);
                } else {
                    final JdbcTable jdbcTable = new JdbcTable(this, tableName, sqlOriginaltableName);
                    setTables.put(tableName, jdbcTable);
                }
            }
            rs.close();

            // attach the collection table to the source table
            for (final String sqlOriginalComposedName : collectionChildTableSqlOriginalTableName) {
                final int pos = sqlOriginalComposedName.indexOf("_");
                final String sourceTableName = sqlOriginalComposedName.substring(0, pos).toLowerCase();
                final String childTableName = sqlOriginalComposedName.substring(pos + 1).toLowerCase();

                // Attention : if the table name is longueur than ParametersCalcul.maxNumberOfCharacterForCollectionTable, then the table name is truncated.
                // So, if the table name is "ThisIsATableNameVeryLong",the sourceTableName is "ThisIsATableNa"
                // Corrolair : a composition table name may be registered in TWO or MORE source ("ThisIsATableNa" can be registerd in "ThisIsATableNameVeryLong" and "ThisIsATableNameVeryVeryLong"
                // in that case, we'll find to jdbcTable at this moment. So, the child table stay in the collectionChildTable list
                final JdbcTable jdbcTable = setTables.get(sourceTableName);
                if (jdbcTable != null) {
                    jdbcTable.addCollectionTable(this, sourceTableName, sqlOriginalComposedName, childTableName);
                } else {
                    collectionUnknowChildTables.put(sqlOriginalComposedName.toLowerCase(),
                            new JdbcTable(this, sqlOriginalComposedName.toLowerCase(),
                                    sqlOriginalComposedName));
                }
            }

            // now, create each table
            for (final JdbcTable table : setTables.values()) {
                table.getContentFromConnection(con, operationStatus);
            }

        } catch (final SQLException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            operationStatus.addErrorMsg("Error during read meta data :" + exceptionDetails);
        }

        return;
    }

    /**
     * @return
     */
    public String getDatabaseProductName() {
        return databaseProductName;
    }

    /**
     * @return
     */
    public Map<String, JdbcTable> getSetTables() {
        return setTables;
    }

    public Map<String, JdbcTable> getCollectionUnknowChildTable() {
        return collectionUnknowChildTables;
    }

}
