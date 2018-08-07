package org.bonitasoft.custompage.noonrover.executor;

import java.util.ArrayList;
import java.util.List;

import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusAttribute;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusDefinition.TYPECOLUMN;
import org.bonitasoft.custompage.noonrover.businessdefinition.NRBusResult;
import org.bonitasoft.custompage.noonrover.toolbox.NRToolbox.NRException;

public class NRExecutorSql extends NRExecutor {

    public NRExecutor.ExecutorStream execute(NRExecutor.ExecutorStream executorStream) throws NRException {

        /**
         * It not possible to add any order, because the sql clause may be very complex
         */
        executorStream.isOrderPossible = false;
        executorStream.isFilterPossible = false;
        List<Object> listValues = new ArrayList<Object>();

        // run the sqlRequest and replace all ':XXX' by the current parameter. Attention, the :XX may be multiple so we must parse from the begining the string
        // select a,b,c where a = :XX and b= :YY and c <= :XX and c>= :XX
        // and we have to keep the order : here value[0] = value(xx), value[1]=value(yy), value[2]=value(xx),value[3]=value(xx),
        String sqlRequest = executorStream.selection.sqlText;
        // Attention, we have to look place
        int pos = 0;
        String minKey = null;

        do {
            // first the first visible key
            int minpos = 0;
            minKey = null;
            for (String key : executorStream.selection.parametersValue.keySet()) {
                // while we fin the key, replace it
                pos = sqlRequest.indexOf(":" + key);
                if (pos != -1) {
                    if (minKey == null || pos < minpos) {
                        minKey = key;
                        minpos = pos;
                    }
                }
            }
            if (minKey != null) {
                int lastReplacement = minpos + minKey.length() + 1;

                sqlRequest = sqlRequest.substring(0, minpos) + "?" + sqlRequest.substring(lastReplacement);
                listValues.add(executorStream.selection.parametersValue.get(minKey));
            }
        } while (minKey != null);

        executorStream = NRExecutorStandard.executePreparedStatement(executorStream, sqlRequest, listValues, null);

        // build a new requestData.result
        executorStream.result = new NRBusResult(executorStream.selection.busDefinition);
        for (String columnName : executorStream.listColumnName) {
            NRBusAttribute busAttribute = executorStream.selection.busDefinition.getInstanceAttribute(
                    executorStream.selection.busDefinition.getTableName(), columnName, TYPECOLUMN.STRING, false);
            executorStream.result.addListResultsetColumFromAttribute(busAttribute);
        }
        return executorStream;
    }

}
