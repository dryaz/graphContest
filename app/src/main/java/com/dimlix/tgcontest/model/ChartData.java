package com.dimlix.tgcontest.model;

import java.util.ArrayList;
import java.util.List;

public class ChartData {
    private List<Long> mXValues;
    private List<YData> mYValues = new ArrayList<>();

    public void addYValues(YData data) {
        mYValues.add(data);
    }

    public List<YData> getYValues() {
        return mYValues;
    }

    public List<Long> getXValues() {
        return mXValues;
    }

    public void setXValues(List<Long> mXValues) {
        this.mXValues = mXValues;
    }

    @Override
    public String toString() {
        return "ChartData{" +
                "mXValues=" + mXValues +
                ", mYValues=" + mYValues +
                '}';
    }

    public static class YData {
        private String mVarName;
        private String mAlias;
        private String mType;
        private String mColor;
        private List<Long> mValues;

        public YData(String varName, String alias, String type, String color, List<Long> values) {
            mVarName = varName;
            mAlias = alias;
            mType = type;
            mColor = color;
            mValues = values;
        }

        public String getVarName() {
            return mVarName;
        }

        public String getAlias() {
            return mAlias;
        }

        public String getType() {
            return mType;
        }

        public String getColor() {
            return mColor;
        }

        public List<Long> getValues() {
            return mValues;
        }

        @Override
        public String toString() {
            return "YData{" +
                    "mVarName='" + mVarName + '\'' +
                    ", mAlias='" + mAlias + '\'' +
                    ", mType='" + mType + '\'' +
                    ", mColor='" + mColor + '\'' +
                    ", mValues=" + mValues +
                    '}';
        }
    }
}
