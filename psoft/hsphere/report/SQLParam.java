package psoft.hsphere.report;

/* loaded from: hsphere.zip:psoft/hsphere/report/SQLParam.class */
public class SQLParam {
    public static final int ARRAY = 1;
    public static final int ASCIISTREAM = 2;
    public static final int BIGDECIMAL = 3;
    public static final int BINARYSTREAM = 4;
    public static final int BLOB = 5;
    public static final int BYTE = 6;
    public static final int BYTES = 7;
    public static final int CHARACTERSTREAM = 8;
    public static final int CLOB = 9;
    public static final int DATE = 10;
    public static final int DOUBLE = 11;
    public static final int FLOAT = 12;
    public static final int INTEGER = 13;
    public static final int LONG = 14;
    public static final int NULL = 15;
    public static final int OBJECT = 16;
    public static final int REF = 17;
    public static final int SHORT = 18;
    public static final int STRING = 19;
    public static final int TIME = 20;
    public static final int TIMESTAMP = 21;
    public static final int UNICODESTREAM = 22;
    protected int parameterIndex;
    protected String type;
    protected int sqlType;

    public SQLParam(String paramIndex, String type) throws Exception {
        this(Integer.parseInt(paramIndex), type);
    }

    public SQLParam(int paramIndex, String type) throws Exception {
        this.parameterIndex = paramIndex;
        this.type = type;
        setSQLType();
    }

    public int getParamIndex() {
        return this.parameterIndex;
    }

    public int getSQLType() {
        return this.sqlType;
    }

    private void setSQLType() throws Exception {
        if (this.type.toUpperCase().equals("ARRAY")) {
            this.sqlType = 1;
        } else if (this.type.toUpperCase().equals("ASCIISTREAM")) {
            this.sqlType = 2;
        } else if (this.type.toUpperCase().equals("BIGDECIMAL")) {
            this.sqlType = 3;
        } else if (this.type.toUpperCase().equals("BINARYSTREAM")) {
            this.sqlType = 4;
        } else if (this.type.toUpperCase().equals("BLOB")) {
            this.sqlType = 5;
        } else if (this.type.toUpperCase().equals("BYTE")) {
            this.sqlType = 6;
        } else if (this.type.toUpperCase().equals("BYTES")) {
            this.sqlType = 7;
        } else if (this.type.toUpperCase().equals("CHARACTERSTREAM")) {
            this.sqlType = 8;
        } else if (this.type.toUpperCase().equals("CLOB")) {
            this.sqlType = 9;
        } else if (this.type.toUpperCase().equals("DATE")) {
            this.sqlType = 10;
        } else if (this.type.toUpperCase().equals("DOUBLE")) {
            this.sqlType = 11;
        } else if (this.type.toUpperCase().equals("FLOAT")) {
            this.sqlType = 12;
        } else if (this.type.toUpperCase().equals("INTEGER")) {
            this.sqlType = 13;
        } else if (this.type.toUpperCase().equals("LONG")) {
            this.sqlType = 14;
        } else if (this.type.toUpperCase().equals("NULL")) {
            this.sqlType = 15;
        } else if (this.type.toUpperCase().equals("OBJECT")) {
            this.sqlType = 16;
        } else if (this.type.toUpperCase().equals("REF")) {
            this.sqlType = 17;
        } else if (this.type.toUpperCase().equals("SHORT")) {
            this.sqlType = 18;
        } else if (this.type.toUpperCase().equals("STRING")) {
            this.sqlType = 19;
        } else if (this.type.toUpperCase().equals("TIME")) {
            this.sqlType = 20;
        } else if (this.type.toUpperCase().equals("TIMESTAMP")) {
            this.sqlType = 21;
        } else if (this.type.toUpperCase().equals("UNICODESTREAM")) {
            this.sqlType = 22;
        } else {
            throw new Exception("Unknown SQL type " + this.type.toUpperCase());
        }
    }
}
