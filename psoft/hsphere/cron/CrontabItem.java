package psoft.hsphere.cron;

import java.util.StringTokenizer;

/* loaded from: hsphere.zip:psoft/hsphere/cron/CrontabItem.class */
public class CrontabItem {
    private String mailTo;
    private String minute;
    private String hour;
    private String day_of_month;
    private String month;
    private String day_of_week;
    private String job;

    private CrontabItem(String mailTo, String minute, String hour, String day_of_month, String month, String day_of_week, String job) {
        this.mailTo = mailTo;
        this.minute = minute;
        this.hour = hour;
        this.day_of_month = day_of_month;
        this.month = month;
        this.day_of_week = day_of_week;
        this.job = job;
    }

    public String getCommand() {
        return this.minute + " " + this.hour + " " + this.day_of_month + " " + this.month + " " + this.day_of_week + " " + this.job;
    }

    public String getMailto() {
        return this.mailTo;
    }

    public String getMinute() {
        return this.minute;
    }

    public String getHour() {
        return this.hour;
    }

    public String getDOM() {
        return this.day_of_month;
    }

    public String getMonth() {
        return this.month;
    }

    public String getDOW() {
        return this.day_of_week;
    }

    public String getJob() {
        return this.job;
    }

    public static CrontabItem instance(String mailTo, String command) throws Exception {
        if (command == null) {
            return null;
        }
        StringTokenizer tok = new StringTokenizer(command, " ");
        try {
            String minute = tok.nextToken().trim();
            if (isValidMinute(minute)) {
                String hour = tok.nextToken().trim();
                if (isValidHour(hour)) {
                    String dom = tok.nextToken().trim();
                    if (isValidDom(dom)) {
                        String month = tok.nextToken().trim();
                        if (isValidMonth(month)) {
                            String dow = tok.nextToken().trim();
                            if (isValidDow(dow)) {
                                String job = "";
                                while (tok.hasMoreTokens()) {
                                    job = job + tok.nextToken().trim() + " ";
                                }
                                return new CrontabItem(mailTo, minute, hour, dom, month, dow, job.trim());
                            }
                            return null;
                        }
                        return null;
                    }
                    return null;
                }
                return null;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    static boolean isValidMinute(String minuteStr) {
        if (minuteStr.equalsIgnoreCase("*")) {
            return true;
        }
        int minute = Integer.parseInt(minuteStr);
        if (minute >= 0 && minute <= 59) {
            return true;
        }
        return false;
    }

    static boolean isValidHour(String hourStr) {
        if (hourStr.equalsIgnoreCase("*")) {
            return true;
        }
        int hour = Integer.parseInt(hourStr);
        if (hour >= 0 && hour <= 23) {
            return true;
        }
        return false;
    }

    static boolean isValidDom(String mdomStr) {
        if (mdomStr.equalsIgnoreCase("*")) {
            return true;
        }
        int dom = Integer.parseInt(mdomStr);
        if (dom >= 1 && dom <= 31) {
            return true;
        }
        return false;
    }

    static boolean isValidMonth(String monthStr) {
        if (monthStr.equalsIgnoreCase("*")) {
            return true;
        }
        int month = Integer.parseInt(monthStr);
        if (month >= 1 && month <= 12) {
            return true;
        }
        return false;
    }

    static boolean isValidDow(String dowStr) {
        if (dowStr.equalsIgnoreCase("*")) {
            return true;
        }
        int dow = Integer.parseInt(dowStr);
        if (dow >= 0 && dow <= 7) {
            return true;
        }
        return false;
    }
}
