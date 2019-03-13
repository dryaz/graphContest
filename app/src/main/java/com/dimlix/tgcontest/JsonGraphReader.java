package com.dimlix.tgcontest;

import android.support.annotation.StringDef;

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
    @StringDef({Set.COLORS,
            Set.COLUMNS,
            Set.NAMES,
            Set.TYPES})
    private @interface Set {
        String COLUMNS = "columns";
        String TYPES = "types";
        String NAMES = "names";
        String COLORS = "colors";
    }

    @StringDef({Type.LINE,
            Type.X})
    private @interface Type {
        String LINE = "line";
        String X = "x";
    }

    public GraphData getGraphDataFromJson(String json) {

        GraphData data = new GraphData();

        try {
            JSONArray graphArray = new JSONArray(json);
            for (int i = 0; i < graphArray.length(); i++) {
                ChartData nextChartData = new ChartData();
                JSONObject chartObject = (JSONObject) graphArray.get(i);

                Map<String, String> types = parseKeyValueSet((JSONObject) chartObject.get(Set.TYPES));
                Map<String, String> names = parseKeyValueSet((JSONObject) chartObject.get(Set.NAMES));
                Map<String, String> colors = parseKeyValueSet((JSONObject) chartObject.get(Set.COLORS));

                JSONArray availColumns = (JSONArray) chartObject.get(Set.COLUMNS);
                for (int j = 0; j < availColumns.length(); j++) {
                    String chartId = "";
                    List<Long> chartData = new ArrayList<>();
                    JSONArray valueSet = (JSONArray) availColumns.get(j);
                    for (int k = 0; k < valueSet.length(); k++) {
                        Object nextObj = valueSet.get(k);
                        if (nextObj instanceof String) {
                            chartId = (String) nextObj;
                        } else if (nextObj instanceof Long
                                || nextObj instanceof Integer) {
                            chartData.add(((Number) nextObj).longValue());
                        }
                    }
                    String currentType = types.get(chartId);
                    switch (currentType) {
                        case Type.LINE:
                            nextChartData.addYValues(new ChartData.YData(chartId,
                                    names.get(chartId), currentType, colors.get(chartId), chartData));
                            break;
                        case Type.X:
                            nextChartData.setXValues(chartData);
                            break;
                    }
                }
                data.addChartData(nextChartData);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return data;
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

