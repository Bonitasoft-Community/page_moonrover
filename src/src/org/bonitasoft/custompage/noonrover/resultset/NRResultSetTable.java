package org.bonitasoft.custompage.noonrover.resultset;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult.ResultsetColumn;
import org.bonitasoft.custompage.noonrover.executor.NRExecutor;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;

public class NRResultSetTable extends NRResultSet {

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd/MM/yyyy");
    DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss Z");

    public NRExecutor.ExecutorStream execute(NRExecutor.ExecutorStream requestData) throws NRException {

        // prepare the header
        Map<String, Double> sumData = new HashMap<String, Double>();
        for (ResultsetColumn column : requestData.result.listColumnset) {
            if (column.isVisible) {
                Map<String, Object> headerCol = new HashMap<String, Object>();
                headerCol.put(NRBusResult.cstJsonColumnTitle, column.attributeDefinition.name);
                headerCol.put(NRBusResult.cstJsonColumnId, column.attributeDefinition.name);
                headerCol.put(NRBusResult.cstJsonColumnIsordered, requestData.isOrderPossible);
                headerCol.put(NRBusResult.cstJsonColumnIsfiltered, requestData.isFilterPossible);

                headerCol.put("type", column.attributeDefinition.type.toString());

                requestData.listHeader.add(headerCol);
            }
            if (column.isSum)
                sumData.put(column.attributeDefinition.name, Double.valueOf(0));
        }
        // check that the listof data contains only the requested information, no more, not less
        // build the list of data to be set
        Set<String> setColumnsExpected = new HashSet<String>();

        for (ResultsetColumn column : requestData.result.listColumnset) {
            if (column.isVisible)
                setColumnsExpected.add(column.attributeDefinition.name);
        }

        for (Map<String, Object> record : requestData.listData) {
            Set<String> setColumnToRemove = new HashSet<String>();
            for (String key : record.keySet()) {
                if (!setColumnsExpected.contains(key))
                    setColumnToRemove.add(key);
                if (record.get(key) instanceof Date) {
                    // format the date,
                    record.put(key, simpleDateFormat.format(record.get(key)));
                }
                if (record.get(key) instanceof OffsetDateTime) {
                    // format the date
                    record.put(key, ((OffsetDateTime) record.get(key)).format(dateTimeFormatter));

                }
            }
            for (String key : setColumnToRemove)
                record.remove(key);

            // do the sum
            for (String keyRecord : sumData.keySet()) {
                try {
                    Double value = NRToolbox.getDoubleValue(record.get(keyRecord), Double.valueOf(0));
                    Double currentValue = sumData.get(keyRecord);
                    BigDecimal operation = BigDecimal.valueOf(value).add(BigDecimal.valueOf(currentValue));
                    sumData.put(keyRecord, operation.doubleValue());
                } catch (Exception e) {
                }
            }
        }

        // add the sum value if any
        if (sumData.size() > 0) {
            // Map<String,Object> myData = (Map) sumData;
            requestData.listFooterData.add((Map) sumData);
        }
        return requestData;
    }

}
