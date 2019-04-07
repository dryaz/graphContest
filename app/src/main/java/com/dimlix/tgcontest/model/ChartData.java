package com.dimlix.tgcontest.model;

import android.util.Pair;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartData {
    private List<Long> mXValues;
    private List<Pair<String, String>> mXStringValues = new ArrayList<>();
    private List<YData> mYValues = new ArrayList<>();
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MMM dd", Locale.US);
    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("EEE, MMM dd", Locale.US);

    private String mType;

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public void addYValues(YData data) {
        mYValues.add(data);
    }

    public List<YData> getYValues() {
        return mYValues;
    }

    public List<Long> getXValues() {
        return mXValues;
    }

    public List<Pair<String, String>> getXStringValues() {
        if (mXStringValues.isEmpty()) {
            for (int i = 0; i < mXValues.size(); i++) {
                mXStringValues.add(Pair.create(SHORT_DATE_FORMAT.format(new Date(mXValues.get(i))),
                        LONG_DATE_FORMAT.format(new Date(mXValues.get(i)))));
            }
        }
        return mXStringValues;
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

    public int getSize() {
        return mXValues.size();
    }

    public static class YData {
        private String mVarName;
        private String mAlias;
        private String mType;
        private String mColor;
        private List<Long> mValues;
        private boolean mIsShown = true;

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

        public int getSize() {
            return mValues.size();
        }

        public boolean isShown() {
            return mIsShown;
        }

        public void setShown(boolean shown) {
            mIsShown = shown;
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
