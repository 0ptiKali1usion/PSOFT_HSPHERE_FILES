package psoft.hsphere.tools;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.StringTokenizer;
import org.apache.log4j.Category;

/* loaded from: hsphere.zip:psoft/hsphere/tools/ToolLogger.class */
public class ToolLogger {
    private Category internalLog;
    private FileWriter log;
    private boolean doOutput;
    private boolean detailOutput;
    private String prfx = "";
    private static ToolLogger defaultLogger = null;

    public ToolLogger(String[] argv) throws Exception {
        this.internalLog = null;
        this.log = null;
        this.doOutput = true;
        this.detailOutput = false;
        boolean noLogging = false;
        int i = 0;
        while (i < argv.length) {
            try {
                if ("-l".equals(argv[i]) || "--log".equals(argv[i])) {
                    try {
                        this.log = new FileWriter(argv[i + 1]);
                    } catch (IOException e) {
                        System.out.println("Error initializing log file");
                    }
                    i++;
                } else if ("-dl".equals(argv[i]) || "--detailedlog".equals(argv[i])) {
                    this.detailOutput = true;
                } else if ("-nil".equals(argv[i]) || "--nointernallogging".equals(argv[i])) {
                    noLogging = true;
                } else if ("-s".equals(argv[i]) || "--silent".equals(argv[i])) {
                    this.doOutput = false;
                }
                i++;
            } catch (ArrayIndexOutOfBoundsException e2) {
                return;
            }
        }
        defaultLogger = this;
        if (!noLogging) {
            this.internalLog = Category.getInstance(ToolLogger.class);
        }
    }

    public void outOK() {
        if (this.doOutput) {
            System.out.println("\t [  OK  ]");
        }
        if (this.log != null) {
            try {
                this.log.write("\t [  OK  ]\n");
                this.log.flush();
            } catch (IOException ex) {
                if (this.doOutput) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public void setPrfx(String prfx) {
        this.prfx = prfx;
    }

    public void outFail(String errMessage) {
        try {
            outFail(errMessage, null);
        } catch (Exception e) {
        }
    }

    public void outMessage(String message) {
        if (this.doOutput) {
            System.out.print(this.prfx + message);
        }
        if (this.internalLog != null) {
            this.internalLog.debug(message);
        }
        if (this.log != null) {
            try {
                this.log.write(this.prfx + message);
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public void outMessage(String errMessage, Throwable tr) {
        if (this.doOutput) {
            System.out.println(errMessage);
            if (this.detailOutput) {
                tr.printStackTrace(System.out);
            }
        }
        if (this.internalLog != null) {
            if (tr != null) {
                this.internalLog.error(errMessage, tr);
            } else {
                this.internalLog.error(errMessage);
            }
        }
        if (this.log != null) {
            try {
                this.log.write(errMessage + "\n");
                if (this.detailOutput) {
                    tr.printStackTrace(new PrintWriter((Writer) this.log, true));
                }
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public void outFail() {
        if (this.doOutput) {
            System.out.println("\t[  FAILED  ]");
        }
        if (this.log != null) {
            try {
                this.log.write("\t[  FAILED  ]\n");
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public void outFail(String message, Throwable ex) {
        if (this.doOutput) {
            System.out.println("\t[  FAILED  ]");
            System.out.println(message);
            if (this.detailOutput) {
                ex.printStackTrace(System.out);
            }
        }
        if (this.internalLog != null) {
            if (ex == null) {
                this.internalLog.error(message);
            } else {
                this.internalLog.error(message, ex);
            }
        }
        if (this.log != null) {
            try {
                this.log.write("\t[  FAILED  ]\n");
                this.log.write(this.prfx + message + "\n");
                if (this.detailOutput) {
                    ex.printStackTrace(new PrintWriter((Writer) this.log, true));
                }
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public static ToolLogger getDefaultLogger() {
        if (defaultLogger == null) {
            try {
                return new ToolLogger(new String[]{"-dl -s"});
            } catch (Exception e) {
            }
        }
        return defaultLogger;
    }

    public void smartPrintLn(String text, String delimiter, int width) {
        String tmp = "";
        StringTokenizer st = new StringTokenizer(text, " ");
        while (st.hasMoreTokens()) {
            String t = st.nextToken();
            if ((tmp + " " + t).length() < width) {
                tmp = tmp + " " + t;
            } else {
                System.out.println(delimiter + tmp);
                tmp = t;
            }
        }
        System.out.println(delimiter + tmp);
    }

    public void outWarn() {
        if (this.doOutput) {
            System.out.println("\t[  WARNING ]");
        }
        if (this.log != null) {
            try {
                this.log.write("\t[  WARNING ]\n");
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public void outWarn(String message, Throwable ex) {
        if (this.doOutput) {
            System.out.println("\t[  WARNING ]");
            System.out.println(message);
            if (this.detailOutput) {
                ex.printStackTrace(System.out);
            }
        }
        if (this.internalLog != null) {
            if (ex == null) {
                this.internalLog.error(message);
            } else {
                this.internalLog.error(message, ex);
            }
        }
        if (this.log != null) {
            try {
                this.log.write("\t[  WARNING ]\n");
                this.log.write(this.prfx + message + "\n");
                if (this.detailOutput) {
                    ex.printStackTrace(new PrintWriter((Writer) this.log, true));
                }
                this.log.flush();
            } catch (IOException e) {
            }
        }
    }

    public void outWarn(String errMessage) {
        try {
            outWarn(errMessage, null);
        } catch (Exception e) {
        }
    }
}
