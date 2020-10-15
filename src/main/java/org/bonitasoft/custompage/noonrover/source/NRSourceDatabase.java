package org.bonitasoft.custompage.noonrover.source;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusAttribute;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPESOURCE;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSetAttributes;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.bonitasoft.properties.BonitaEngineConnection;


public class NRSourceDatabase extends NRSource {

    public static BEvent EventErrorAccessDatabase = new BEvent(NRSourceDatabase.class.getName(), 1, Level.ERROR,
            "Access the Business Database", "Check exception", "The list of Business Data can't be get",
            "Fix the error");

    public static BEvent EventInconsistenceTable = new BEvent(NRSourceDatabase.class.getName(), 1, Level.ERROR,
            "Inconsistence Table", "Table are inconsistence",
            "One table are inconsistent, missing some expected infortmation ", "Check the database");

    public final static String cstSuffixColumnPid = "_pid";
    
    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* get the list of object from the database */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public static Logger logger = Logger.getLogger(NoonRoverAccessAPI.class.getName());

    public NRSource.SourceStatus getListBusinessDefinition(APISession apiSession,
            NRBusDefinitionFactory businessFactory) {
        NRSource.SourceStatus sourceStatus = new NRSource.SourceStatus();
        List<NRBusDefinition> listSourceBusinessDefinition = new ArrayList<NRBusDefinition>();
        Connection con = null;
        ResultSet rs = null;
        try {
            con = getConnection();
            if (con == null) {
                sourceStatus.listEvents.add(new BEvent(EventErrorAccessDatabase, "Can't access the database"));
                return sourceStatus;
            }
            /**
             * keep the collection of orignal Name.
             * The table used after in in LOWER CASE.
             */
            final List<String> collectionChildTableSqlOriginalTableName = new ArrayList<String>();

            final DatabaseMetaData databaseMetaData = con.getMetaData();
            final String[] types = new String[] { "TABLE" };
            rs = databaseMetaData.getTables(null /* catalog */, null /* schemaPattern */,
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
                    NRBusDefinition businessDefinition = businessFactory.createDataDefinition(tableName);
                    listSourceBusinessDefinition.add(businessDefinition);
                    businessDefinition.setTableName(sqlOriginaltableName);
                    businessDefinition.setTypeSource(TYPESOURCE.TABLE);
                    sourceStatus.listBusinessDefinition.add(businessDefinition);
                }
            }
            rs.close();
            rs = null;

            // attach the collection table to the source table
            for (final String sqlOriginalComposedName : collectionChildTableSqlOriginalTableName) {
                final int pos = sqlOriginalComposedName.indexOf("_");
                final String sourceTableName = sqlOriginalComposedName.substring(0, pos).toLowerCase();
                final String childTableName = sqlOriginalComposedName.substring(pos + 1).toLowerCase();

                // Attention : if the table name is longueur than ParametersCalcul.maxNumberOfCharacterForCollectionTable, then the table name is truncated.
                // So, if the table name is "ThisIsATableNameVeryLong",the sourceTableName is "ThisIsATableNa"
                // Corrolair : a composition table name may be registered in TWO or MORE source ("ThisIsATableNa" can be registerd in "ThisIsATableNameVeryLong" and "ThisIsATableNameVeryVeryLong"
                // in that case, we'll find to jdbcTable at this moment. So, the child table stay in the collectionChildTable list
                final NRBusDefinition businessDefinition = businessFactory.getByName(sourceTableName);
                if (businessDefinition != null) {
                    businessDefinition.addCollectionTable(sqlOriginalComposedName, childTableName);
                } else {
                    // unknown
                }
            }

            // now, complete the list of find per element
            for (NRBusDefinition businessDefinition : listSourceBusinessDefinition) {
                readTable(con, businessDefinition, sourceStatus);
                // no list to add at this moment businessDefinition.setListSelection( new ArrayList<>() );

                // create 2 more find : the DirectSQL and the Standard one
                NRBusSelection findBy = businessDefinition.getInstanceBusSelection("Standard");
                findBy.typeFind = TYPESELECTION.STD;
                for (NRBusAttribute jdbcColum : businessDefinition.getListAttributes()) {
                    findBy.getInstanceSelectionParameter(jdbcColum.name, jdbcColum.type);
                }

                findBy = businessDefinition.getInstanceBusSelection("Direct SQL");
                findBy.typeFind = TYPESELECTION.SQL;
            }

        } catch (final SQLException e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            sourceStatus.listEvents.add(new BEvent(EventErrorAccessDatabase, e, exceptionDetails));
        } finally {
            if (rs != null)
                try {
                    rs.close();
                } catch (SQLException e) {
                }
            if (con != null)
                con = null;
        }
        return sourceStatus;
    }

    /**
     * getListOfFields form a table
     * 
     * @param con
     * @param operationStatus
     * @return
     */

    public String readTable(final Connection con, NRBusDefinition businessDefinition,
            NRSource.SourceStatus sourceStatus) {
        String result = "";
        ResultSet rs = null;
        try {

            boolean oneColumnFound = false;
            final DatabaseMetaData databaseMetaData = con.getMetaData();
            rs = databaseMetaData.getColumns(null /* catalog */, null /* schema */, businessDefinition.getTableName(),
                    null /* columnNamePattern */);
            while (rs.next()) {
                String tableNameCol = rs.getString("TABLE_NAME");
                tableNameCol = tableNameCol == null ? "" : tableNameCol;

                if (!tableNameCol.equalsIgnoreCase(businessDefinition.getTableName())) {
                    continue;
                }
                oneColumnFound = true;

                String colName = rs.getString("COLUMN_NAME");;
                colName = colName == null ? "" : colName.toLowerCase();
                boolean isCollection=false;
                // don't keep in mind the system column
                // this is a refence table : it should appears in the reference
                if (colName.endsWith(cstSuffixColumnPid)) {
                    // jdbcColumn.colName = jdbcColumn.colName.substring(0, jdbcColumn.colName.length()-4);
                    isCollection = true;
                }

                NRBusAttribute column = businessDefinition.getInstanceAttribute(businessDefinition.getTableName(),
                        colName, getTypeFromJdbc(rs.getInt("DATA_TYPE")),isCollection);

              
                column.length = rs.getInt("COLUMN_SIZE");
                if (column.type == TYPECOLUMN.STRING && column.length > 100000) {
                    column.type = TYPECOLUMN.STRING; // special marker for a Text
                }
                if (column.name.equals("style") || column.name.equals("string2composition")) {
                    // System.out.println("JdbcTable : jdbcColumn[" + jdbcColumn.name + "] type[" + jdbcColumn.dataType + "]");
                }
                column.nullable = "YES".equals(rs.getString("IS_NULLABLE"));
            }
            rs.close();

            if (!oneColumnFound) {
                sourceStatus.listEvents.add(new BEvent(EventInconsistenceTable, "No table["
                        + businessDefinition.getTableName()
                        + "] found: the table is detected on the CATALOG table, but no colum are found in the CATALOG column"));
            }

            // result set column ? Same as colum discover
            for (NRBusAttribute attribute : businessDefinition.getListAttributes()) {
                businessDefinition.result.addListResultsetColumFromAttribute(attribute);
            }

            // index & constraints
            // i==0 => unique = Constraint
            // i==1 => index

            Map<String, NRBusSetAttributes> mapIndexes = new HashMap<String, NRBusSetAttributes>();

            /**
             * constraints. Key must be in lower case
             */
            Map<String, NRBusSetAttributes> mapConstraints = new HashMap<String, NRBusSetAttributes>();

            rs = databaseMetaData.getIndexInfo(null /* String catalog */, null /* String schema */,
                    businessDefinition.getTableName(), false /* unique */, false /* boolean approximate */);
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
                NRBusSetAttributes oneListItem = nonUnique ? mapIndexes.get(indexName) : mapConstraints.get(indexName);
                if (oneListItem == null) {
                    oneListItem = new NRBusSetAttributes(true);
                    if (nonUnique) {
                        mapIndexes.put(indexName.toLowerCase(), oneListItem);
                    } else {
                        mapConstraints.put(indexName.toLowerCase(), oneListItem);
                    }

                }
                oneListItem.name = indexName;
                oneListItem.unique = !nonUnique;
                oneListItem.addColumns(columnName);
            }
            rs.close();

            // constraints and reference key
            rs = databaseMetaData.getImportedKeys(null, null, businessDefinition.getTableName());

            while (rs.next()) {
                String columnName = rs.getString("FKCOLUMN_NAME");
                columnName = columnName.toLowerCase();
                // search the column
                for (final NRBusAttribute jdbcColumn : businessDefinition.getListAttributes()) {
                    if (jdbcColumn.name.equals(columnName)) {
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

            businessDefinition.setMapIndexes(mapIndexes);
            businessDefinition.setMapConstraints(mapConstraints);
            /*
             * ResultSet getCrossReference(String parentCatalog,
             * String parentSchema,
             * String parentTable,
             * String foreignCatalog,
             * String foreignSchema,
             * String foreignTable)
             * throws SQLException
             */

            // let's check all column
            /*
             * for (final JdbcColumn jdbcColumn : listColumns)
             * {
             * if (jdbcColumn.isForeignKey && jdbcColumn.contraintsName == null)
             * {
             * operationStatus.addErrorMsg("Table[" + tableName + "] colum[" + jdbcColumn.colName + "] is a Foreign key without constraint name...");
             * jdbcColumn.isForeignKey = false;
             * }
             * }
             */
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

    /* -------------------------------------------------------------------- */
    /*                                                                      */
    /* Get a Datasource */
    /*                                                                      */
    /* -------------------------------------------------------------------- */
    public String datasourceName;

    public static Connection getConnection() throws SQLException {
        return BonitaEngineConnection.getBusinessConnection();
    }

    public TYPECOLUMN getTypeFromJdbc(int jdbcType) {
        if (jdbcType == Types.BIGINT || jdbcType == Types.DECIMAL || jdbcType == Types.INTEGER)
            return TYPECOLUMN.NUM;
        if (jdbcType == Types.VARCHAR)
            return TYPECOLUMN.STRING;

        // this is a TEXT
        if (jdbcType == Types.CLOB)
            return TYPECOLUMN.STRING;
        if (jdbcType == Types.BOOLEAN)
            return TYPECOLUMN.BOOLEAN;

        return TYPECOLUMN.STRING;
    }
}
