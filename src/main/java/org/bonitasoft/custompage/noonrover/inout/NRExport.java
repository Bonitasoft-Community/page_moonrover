package org.bonitasoft.custompage.noonrover.inout;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bonitasoft.command.BonitaCommand.ExecuteAnswer;
import org.bonitasoft.command.BonitaCommand.ExecuteParameters;
import org.bonitasoft.custompage.noonrover.NoonRoverAccessAPI;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusCmdControl;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionBDM;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinitionFactory;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult.TYPERESULTSET;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusSelection.TYPESELECTION;
import org.bonitasoft.custompage.noonrover.executor.NRStream;
import org.bonitasoft.custompage.noonrover.executor.NRExecutor;
import org.bonitasoft.custompage.noonrover.executor.NRExecutorSql;
import org.bonitasoft.custompage.noonrover.resultset.NRResultSet;
import org.bonitasoft.custompage.noonrover.resultset.NRResultSetCsv;
import org.bonitasoft.custompage.noonrover.source.NRSourceBDM;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;
import org.bonitasoft.custompage.noonrover.source.NRSource.SourceStatus;
import org.bonitasoft.engine.api.APIAccessor;
import org.bonitasoft.engine.api.TenantAdministrationAPI;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.log.event.BEvent;

public class NRExport {

    public static Logger logger = Logger.getLogger(NRExport.class.getName());

    private static String logHeader = "NoonRover.cmd";

    private ArrayList<BEvent> listEvents = new ArrayList();

    public ExecuteAnswer launchExport(TenantAdministrationAPI tenantAdministrationAPI, ExecuteParameters executeParameters, APIAccessor apiAccessor, TenantServiceAccessor tenantServiceAccessor) {
        // start an export
        ExportParameter exportParameter = new ExportParameter();
        exportParameter.name = executeParameters.getParametersString(NRBusCmdControl.CST_EXPORTNAME);
        exportParameter.entities = (List) executeParameters.parametersCommand.get(NRBusCmdControl.CST_EXPORTLISTENTITIES);
        exportParameter.maxRecordsPerEntities = executeParameters.getParametersLong(NRBusCmdControl.CST_EXPORTMAXRECORDPERENTITY);
        exportParameter.directoryToExport = executeParameters.getParametersString(NRBusCmdControl.CST_EXPORTDIRECTORYEXPORT);
        exportParameter.tenantAdministrationAPI = tenantAdministrationAPI;
        exportParameter.launchThread();
        ExecuteAnswer executeAnswer = new ExecuteAnswer();
        executeAnswer.result.put(NRBusCmdControl.CST_STATUS, NRBusCmdControl.CST_STATUS_V_STARTED);
        return executeAnswer;
    }

    /**
     * This method is call in a specific thread
     * 
     * @param exportParameter
     */
    public void export(ExportParameter exportParameter) {
        // create a zip file
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        if (exportParameter.name == null || exportParameter.name.trim().isEmpty()) {
            if (exportParameter.entities.isEmpty())
                exportParameter.name = "Export";
            else
                exportParameter.name = exportParameter.entities.get(0);
        }

        String zipFileName = exportParameter.directoryToExport + "/" + exportParameter.name + "_" + LocalDateTime.now().format(formatter);
        try (
                ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFileName + ".zipinprogress"));) {
            for (String entity : exportParameter.entities) {
                ZipEntry zipEntry = new ZipEntry(entity + ".csv");
                zos.putNextEntry(zipEntry);
                exportEntity(exportParameter.tenantAdministrationAPI, entity, exportParameter.maxRecordsPerEntities, zos);
                zos.closeEntry();
            }
            zos.flush();
            zos.close();
            // rename zip now
            Path source = Paths.get(zipFileName + ".zipinprogress");
            Path target = Paths.get(zipFileName + ".zip");
            Files.move(source, target);
        } catch (Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logger.severe(logHeader + " Exception " + e.getMessage() + " at " + exceptionDetails);
        }
    }

    /**
     * Export one entity
     * 
     * @param entity
     * @param maxRecord
     * @param outputStream
     */
    private void exportEntity(TenantAdministrationAPI tenantAdministrationAPI, String entity, long maxRecord, OutputStream outputStream) {

        NRSourceBDM sourceBdm = new NRSourceBDM();

        NRBusDefinitionFactory busDefinitionFactory = NRBusDefinitionFactory.getInstance();
        SourceStatus sourceStatus = sourceBdm.loadBusinessObjectInFactory(tenantAdministrationAPI, busDefinitionFactory);
        listEvents.addAll(sourceStatus.listEvents);

        NRBusDefinition nrBusDefinition = busDefinitionFactory.getByName(entity);
        if (nrBusDefinition == null)
            return;
        NoonRoverAccessAPI.ParameterSource parameterSource = new NoonRoverAccessAPI.ParameterSource();
        parameterSource.startIndex = 0L;
        parameterSource.maxResults = maxRecord;

        NRStream executorStream = new NRStream(parameterSource);
        executorStream.setResult(nrBusDefinition.result);
        executorStream.setOutputstream(outputStream);

        // set the standard selection
        for (NRBusSelection selection : nrBusDefinition.listSelections) {
            if (NRBusSelection.CST_NAME_STANDARD.equals(selection.name))
                executorStream.selection = selection;
        }
        List<NRExecutor> listExecutor = new ArrayList<>();

        try {
            listExecutor.add(NRExecutor.getInstance(TYPESELECTION.STD));
            listExecutor.add(NRResultSet.getInstance(TYPERESULTSET.TABLE));
            listExecutor.add(new NRResultSetCsv());

            nrBusDefinition.execute(executorStream, listExecutor);

        } catch (NRException e) {
            logger.severe(logHeader + "ExportEntity " + e.getMessage());
        }

    }

    private class ExportParameter extends Thread {

        public String name;
        public List<String> entities;
        public long maxRecordsPerEntities;
        public String directoryToExport;
        public TenantAdministrationAPI tenantAdministrationAPI;

        public void launchThread() {
            this.start();
        }

        public void run() {
            NRExport nrExport = new NRExport();
            nrExport.export(this);
        }

    }

}
