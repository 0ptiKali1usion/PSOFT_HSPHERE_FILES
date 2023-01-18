package psoft.util;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

/* loaded from: hsphere.zip:psoft/util/USFormat.class */
public class USFormat {
    public static NumberFormat getInstance() {
        return NumberFormat.getNumberInstance(Locale.US);
    }

    public static NumberFormat getInstanceIn() {
        NumberFormat engFormatIn = NumberFormat.getNumberInstance(Locale.US);
        engFormatIn.setGroupingUsed(true);
        return engFormatIn;
    }

    public static NumberFormat getInstanceOut() {
        NumberFormat engFormatOut = NumberFormat.getNumberInstance(Locale.US);
        engFormatOut.setGroupingUsed(false);
        return engFormatOut;
    }

    public static double parseDouble(String str) throws ParseException {
        return getInstanceIn().parse(str).doubleValue();
    }

    public static String format(double number) throws ParseException {
        return getInstanceOut().format(number);
    }

    public static String parseString(String str) throws ParseException {
        double tmpValue = getInstanceIn().parse(str).doubleValue();
        return format(tmpValue);
    }
}
