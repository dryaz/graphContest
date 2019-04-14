package com.dimlix.tgcontest;

import com.dimlix.tgcontest.model.ChartData;
import com.dimlix.tgcontest.model.GraphData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class JsonGraphReader {
    public @interface Set {
        String COLUMNS = "columns";
        String TYPES = "types";
        String NAMES = "names";
        String COLORS = "colors";
    }


    public GraphData getGraphDataFromJson(String json) {

        GraphData data = new GraphData();

        try {
            JSONArray graphArray = new JSONArray(json);
            for (int i = 0; i < graphArray.length(); i++) {
                data.addChartData(getChartDataFromJson((JSONObject) graphArray.get(i)));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return data;
    }

    public ChartData getChartDataFromJson(JSONObject chartObject) throws JSONException {
        ChartData nextChartData = new ChartData();
        Map<String, String> types = parseKeyValueSet((JSONObject) chartObject.get(Set.TYPES));
        Map<String, String> names = parseKeyValueSet((JSONObject) chartObject.get(Set.NAMES));
        Map<String, String> colors = parseKeyValueSet((JSONObject) chartObject.get(Set.COLORS));
        boolean fillFirstLine = false;

        JSONArray availColumns = (JSONArray) chartObject.get(Set.COLUMNS);
        for (int j = 0; j < availColumns.length(); j++) {
            String chartId = "";
            List<Long> chartData = new ArrayList<>();
            JSONArray valueSet = (JSONArray) availColumns.get(j);
            int index = 0;
            String currentType = null;
            for (int k = 0; k < valueSet.length(); k++) {
                Object nextObj = valueSet.get(k);
                if (nextObj instanceof String) {
                    chartId = (String) nextObj;
                    currentType = types.get(chartId);
                } else if (nextObj instanceof Long
                        || nextObj instanceof Integer) {
                    long value = ((Number) nextObj).longValue();
                    chartData.add(value);
                }
            }
            switch (currentType) {
                case ChartData.Type.BAR:
                case ChartData.Type.AREA:
                case ChartData.Type.LINE:
                    nextChartData.addYValues(new ChartData.YData(chartId,
                            names.get(chartId), currentType, colors.get(chartId), chartData));
                    break;
                case ChartData.Type.X:
                    nextChartData.setXValues(chartData);
                    break;
            }
        }
        nextChartData.setDoubleYAxis(chartObject.has("y_scaled") && chartObject.getBoolean("y_scaled"));
        nextChartData.setStacked(chartObject.has("stacked") && chartObject.getBoolean("stacked"));
        nextChartData.setPercentage(chartObject.has("percentage") && chartObject.getBoolean("percentage"));
        return nextChartData;
    }

    public ChartData getChartDataFromJson(String json) throws JSONException {
        JSONObject chartObject = new JSONObject(json);
        return getChartDataFromJson(chartObject);
    }

    private Map<String, String> parseKeyValueSet(JSONObject objectToParse) throws JSONException {
        Map<String, String> result = new HashMap<>();
        for (Iterator<String> it = objectToParse.keys(); it.hasNext(); ) {
            String key = it.next();
            result.put(key, objectToParse.getString(key));
        }
        return result;
    }
}

