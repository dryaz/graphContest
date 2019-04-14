package com.dimlix.tgcontest.model;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChartData {
    public @interface Type {
        String LINE = "line";
        String AREA = "area";
        String BAR = "bar";

        String X = "x";
    }

    private List<Long> mXValues;
    private List<ChartDates> mXStringValues = new ArrayList<>();
    private List<YData> mYValues = new ArrayList<>();
    private static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("dd MMM", Locale.US);
    private static final SimpleDateFormat LONG_DATE_FORMAT = new SimpleDateFormat("dd MMM yyyy", Locale.US);
    private static final SimpleDateFormat EXTENDED_DATE_FORMAT = new SimpleDateFormat("EEE, dd MMM yyyy", Locale.US);

    private boolean mPercentage = false;
    private boolean mStacked = false;
    private boolean mDoubleYAxis;

    private String mName;

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
    }

    public boolean isPercentage() {
        return mPercentage;
    }

    public void setPercentage(boolean percentage) {
        mPercentage = percentage;
    }

    public boolean isStacked() {
        return mStacked;
    }

    public void setStacked(boolean stacked) {
        this.mStacked = stacked;
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

    public List<ChartDates> getXStringValues() {
        if (mXStringValues.isEmpty()) {
            for (int i = 0; i < mXValues.size(); i++) {
                mXStringValues.add(new ChartDates(
                        SHORT_DATE_FORMAT.format(new Date(mXValues.get(i))),
                        LONG_DATE_FORMAT.format(new Date(mXValues.get(i))),
                        EXTENDED_DATE_FORMAT.format(new Date(mXValues.get(i)))
                ));
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

    public boolean isDoubleYAxis() {
        return mDoubleYAxis;
    }

    public void setDoubleYAxis(boolean doubleYAxis) {
        mDoubleYAxis = doubleYAxis;
    }

    public static class YData {
        private String mVarName;
        private String mAlias;
        private String mType;
        private String mColor;
        private List<Long> mValues;
        private boolean mIsShown = true;

        private boolean mIsBar = false;

        public boolean isBar() {
            return mIsBar;
        }

        public YData(String varName, String alias, String type, String color, List<Long> values) {
            mVarName = varName;
            mAlias = alias;
            mType = type;
            mColor = color;
            mValues = values;
            mIsBar = mType.equals(Type.BAR);
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

    public static class ChartDates {
        private String dateMonthOnly;
        private String fullDate;
        private String extendedDate;

        public ChartDates(String dateMonthOnly, String fullDate, String extendedDate) {
            this.dateMonthOnly = dateMonthOnly;
            this.fullDate = fullDate;
            this.extendedDate = extendedDate;
        }

        public String getDateMonthOnly() {
            return dateMonthOnly;
        }

        public String getFullDate() {
            return fullDate;
        }

        public String getExtendedDate() {
            return extendedDate;
        }
    }
}
