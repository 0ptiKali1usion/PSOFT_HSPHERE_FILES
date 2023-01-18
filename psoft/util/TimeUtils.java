package psoft.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.TimeZone;

/* loaded from: hsphere.zip:psoft/util/TimeUtils.class */
public class TimeUtils {
    private static long startTime;
    private static long trueTimeStarted;
    private static int multiplier;
    private static boolean trueTime = true;
    private static String filename = null;

    static {
        try {
            ResourceBundle rb = PropertyResourceBundle.getBundle("timeutils");
            String enabled = rb.getString("ENABLED");
            if ("TRUE".equalsIgnoreCase(enabled)) {
                init(rb.getString("FILE_NAME"), Integer.parseInt(rb.getString("MULTIPLIER")));
            }
        } catch (Throwable th) {
        }
    }

    public static void main(String[] args) throws Exception {
        System.out.println("TimeUtils now:" + getDate());
        Thread.sleep(2000L);
        System.out.println("TimeUtils now:" + getDate());
        Thread.sleep(2000L);
        System.out.println("TimeUtils now:" + getDate());
        sleep(2000L);
        System.out.println("TimeUtils now:" + getDate());
    }

    private static void _init(long st, int multiplier2) {
        trueTimeStarted = currentTimeMillis();
        startTime = st;
        multiplier = multiplier2;
        trueTime = false;
    }

    public static void init(String filename2, int multiplier2) {
        long st;
        filename = filename2;
        DataInputStream is = null;
        try {
            try {
                is = new DataInputStream(new FileInputStream(filename2));
                st = is.readLong();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e) {
                    }
                }
            } catch (IOException e2) {
                st = currentTimeMillis();
                if (is != null) {
                    try {
                        is.close();
                    } catch (IOException e3) {
                    }
                }
            }
            _init(st, multiplier2);
        } catch (Throwable th) {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e4) {
                    throw th;
                }
            }
            throw th;
        }
    }

    private static void save(long st) {
        try {
            DataOutputStream out = new DataOutputStream(new FileOutputStream(filename));
            out.writeLong(st);
            out.close();
        } catch (IOException e) {
        }
    }

    public static Date getSQLDate() {
        return new Date(currentTimeMillis());
    }

    public static Timestamp getSQLTimestamp() {
        return new Timestamp(currentTimeMillis());
    }

    public static Time getSQLTime() {
        return new Time(currentTimeMillis());
    }

    public static java.util.Date getDate() {
        return new java.util.Date(currentTimeMillis());
    }

    public static long currentTimeMillis() {
        if (trueTime) {
            return System.currentTimeMillis();
        }
        long st = startTime + ((System.currentTimeMillis() - trueTimeStarted) * multiplier);
        save(st);
        return st;
    }

    public static Calendar getCalendar(java.util.Date t) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(t);
        return cal;
    }

    public static Calendar getCalendar() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(getDate());
        return cal;
    }

    public static Calendar getCalendar(Locale aLocale) {
        Calendar cal = Calendar.getInstance(aLocale);
        cal.setTime(getDate());
        return cal;
    }

    public static Calendar getCalendar(TimeZone zone) {
        Calendar cal = Calendar.getInstance(zone);
        cal.setTime(getDate());
        return cal;
    }

    public static Calendar getCalendar(TimeZone zone, Locale aLocale) {
        Calendar cal = Calendar.getInstance(zone, aLocale);
        cal.setTime(getDate());
        return cal;
    }

    public static void sleep(long s) throws InterruptedException {
        if (trueTime) {
            Thread.sleep(s);
        } else {
            Thread.sleep(s / multiplier);
        }
    }

    public static java.util.Date dropMinutes(java.util.Date date) {
        Calendar cal = getCalendar(date);
        cal.set(cal.get(1), cal.get(2), cal.get(5), 0, 0, 0);
        cal.set(14, 0);
        return cal.getTime();
    }

    public static long getDateTimeInSeconds(java.util.Date date) throws Exception {
        if (date != null) {
            return date.getTime() / 1000;
        }
        return 0L;
    }
}
