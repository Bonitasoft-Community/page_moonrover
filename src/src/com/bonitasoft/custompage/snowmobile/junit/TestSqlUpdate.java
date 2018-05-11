package com.bonitasoft.custompage.snowmobile.junit;

import static org.junit.Assert.assertTrue;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.json.simple.JSONValue;
import org.junit.Test;

import com.bonitasoft.custompage.snowmobile.OperationStatus;
import com.bonitasoft.custompage.snowmobile.SnowMobileAccess;
import com.bonitasoft.custompage.snowmobile.SnowMobileAccess.ParametersCalcul;

public class TestSqlUpdate {

    // @ T e s t
    public void testLoadBdm() {
        final SnowMobileAccess sqlUpdate = new SnowMobileAccess();
        final OperationStatus operationStatus = new OperationStatus();
        sqlUpdate.setBdmFromFile("E:/pym/Google Drive/developpement/customPage/custompage_snowmobile/bdm-V2.zip",
                operationStatus);

        if (sqlUpdate.getBdm() != null) {
            final String bdmDescription = JSONValue.toJSONString(sqlUpdate.getBdm().getJsonDescription());
            System.out.println(bdmDescription);
        }
        assertTrue(!operationStatus.isError());
    }

    // @ T e s t
    public void testAccessDatasource() {
        final SnowMobileAccess sqlUpdate = new SnowMobileAccess();
        final OperationStatus operationStatus = new OperationStatus();
        sqlUpdate.setDatamodelFromDatabaseConnection("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/BDM",
                "postgres", "manager", operationStatus);
        /*
         * {
         * String bdmDescription = JSONValue.toJSONString( sqlUpdate.getBdm().getJsonDescription()) ;
         * System.out.println(bdmDescription);
         * }
         */
        assertTrue(!operationStatus.isError());
    }

    // test in 6.4.0 and 6.4.1d
    @Test
    public void testCalculScript() {
        final SnowMobileAccess sqlUpdate = new SnowMobileAccess();
        final OperationStatus operationStatus = new OperationStatus();
        try {
            // E:\pym\Google Drive\developpement\customPage\custompage_snowmobile\bdm-V2
            // sqlUpdate.setBdmFromFile("E:/pym/Google Drive/developpement/customPage/custompage_snowmobile/bdm-V2/bom.zip", operationStatus);
            // sqlUpdate.setBdmFromFile("E:/pym/Google Drive/developpement/customPage/custompage_snowmobile/bdm-V2/bdm.zip", operationStatus);

            // sqlUpdate.setDatamodelFromDatabaseConnection("org.postgresql.Driver", "jdbc:postgresql://localhost:5432/BDM", "bonita", "bonita", operationStatus);

            sqlUpdate.setBdmFromFile("E:/pym/Google Drive/Bonita 7/Bonita V7 Exercice Bdm/bdmWithNewModification.zip",
                    operationStatus);
            sqlUpdate.setDatamodelFromDatabaseConnection("org.h2.Driver",
                    "jdbc:h2:tcp://localhost:9091/business_data.db;MVCC=TRUE;DB_CLOSE_ON_EXIT=TRUE;IGNORECASE=TRUE;",
                    "sa", "", operationStatus);

            sqlUpdate.calculSqlScript(new ParametersCalcul(), operationStatus);
        } catch (final Exception e) {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            System.out.println("Exception " + e.toString() + exceptionDetails);
        }
        System.out.println("Error Message: " + operationStatus.getErrorMsg());
        System.out.println("Message      : " + operationStatus.getMsg());
        System.out.println("Delta Message: " + operationStatus.getDeltaMsg());
        System.out.println("SQL          : " + operationStatus.getSql());

        assertTrue(!operationStatus.isError());
    }
}
