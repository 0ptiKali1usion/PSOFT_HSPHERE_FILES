package psoft.hsphere.resource.sshpool;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;
import psoft.util.Base64;
import psoft.util.TimeUtils;

/* loaded from: hsphere.zip:psoft/hsphere/resource/sshpool/SSHConnection.class */
public class SSHConnection {
    protected static final String scriptRunner = "/hsphere/shared/scripts/script-runner.pl";
    protected static final String scriptRunnerB64 = "/hsphere/shared/scripts/script-runner-b64";
    protected String login;

    /* renamed from: ip */
    protected String f214ip;
    protected Process sshProcess;
    protected BufferedReader inputStream;
    protected DataOutputStream outputStream;
    protected boolean isBase64 = false;
    protected boolean busy = false;
    protected long idleTime = TimeUtils.currentTimeMillis();

    public SSHConnection(String ip, String login) {
        this.f214ip = ip;
        this.login = login;
    }

    public boolean isMatch(String ip, String login) {
        return ip.equals(this.f214ip) && login.equals(this.login);
    }

    protected void connect() throws Exception {
        Session.getLog().debug("Open SSH Connection to: " + this.f214ip);
        this.sshProcess = Runtime.getRuntime().exec("ssh -x -a " + this.login + "@" + this.f214ip + " " + scriptRunnerB64);
        this.outputStream = new DataOutputStream(this.sshProcess.getOutputStream());
        this.outputStream.writeBytes("PING\n");
        this.outputStream.flush();
        this.isBase64 = true;
        this.inputStream = new BufferedReader(new InputStreamReader(this.sshProcess.getInputStream()));
        if (!"PONG".equals(this.inputStream.readLine())) {
            this.sshProcess.destroy();
            this.sshProcess = Runtime.getRuntime().exec("ssh -x -a " + this.login + "@" + this.f214ip + " " + scriptRunner);
            this.outputStream = new DataOutputStream(this.sshProcess.getOutputStream());
            this.isBase64 = false;
            this.inputStream = new BufferedReader(new InputStreamReader(this.sshProcess.getInputStream()));
        }
    }

    public SSHResult execute(String command, Collection args, byte[] input) throws Exception {
        String response;
        if (this.sshProcess == null) {
            connect();
        }
        try {
            this.sshProcess.exitValue();
            connect();
        } catch (IllegalThreadStateException e) {
        }
        try {
            this.outputStream.writeBytes("PING\n");
            this.outputStream.flush();
            response = this.inputStream.readLine();
        } catch (IOException e2) {
            Session.getLog().warn("Broken connection, restoring");
            quit();
            connect();
        }
        if (response == null || !"PONG".equals(response)) {
            throw new IOException("Got wrong response " + response);
        }
        return realExecute(command, args, input);
    }

    public SSHResult realExecute(String command, Collection args, String input) throws Exception {
        return realExecute(command, args, input.getBytes(LanguageManager.STANDARD_CHARSET));
    }

    public SSHResult realExecute(String command, Collection args, byte[] input) throws Exception {
        StringBuffer buf = new StringBuffer();
        buf.append("COMMAND=").append(command);
        Iterator i = args.iterator();
        int j = 1;
        while (i.hasNext()) {
            String value = (String) i.next();
            int i2 = j;
            j++;
            buf.append("&").append("PARAM").append(i2).append("=");
            if (this.isBase64) {
                buf.append(value == null ? "" : Base64.encode(value));
            } else {
                buf.append(value == null ? "" : URLEncoder.encode(value, LanguageManager.STANDARD_CHARSET));
            }
        }
        if (input != null) {
            buf.append("&INPUT=");
            if (this.isBase64) {
                buf.append(Base64.encode(input));
            } else {
                buf.append(URLEncoder.encode(new String(input), LanguageManager.STANDARD_CHARSET));
            }
        }
        buf.append("\n");
        String line = buf.toString();
        this.outputStream.writeBytes(line);
        this.outputStream.flush();
        String response = this.inputStream.readLine();
        if (response == null) {
            StringBuffer errors = new StringBuffer();
            BufferedReader er = new BufferedReader(new InputStreamReader(this.sshProcess.getErrorStream()));
            while (true) {
                String tmp = er.readLine();
                if (null != tmp) {
                    errors.append(tmp).append("\n");
                } else {
                    return new SSHResult(-1, null, errors.toString());
                }
            }
        } else {
            int exitCode = -1;
            String error = null;
            String output = null;
            try {
                StringTokenizer st = new StringTokenizer(response, "&");
                while (st.hasMoreTokens()) {
                    String pair = st.nextToken();
                    int pos = pair.indexOf(61);
                    String key = pair.substring(0, pos);
                    String val = pair.substring(pos + 1, pair.length());
                    if (key.equals("EXIT_CODE")) {
                        exitCode = Integer.parseInt(val);
                    } else if (key.equals("ERROR")) {
                        error = this.isBase64 ? Base64.decodeToString(val) : URLDecoder.decode(val, LanguageManager.STANDARD_CHARSET);
                    } else if (key.equals("OUTPUT")) {
                        output = this.isBase64 ? Base64.decodeToString(val) : URLDecoder.decode(val, LanguageManager.STANDARD_CHARSET);
                    }
                }
                this.idleTime = TimeUtils.currentTimeMillis();
                return new SSHResult(exitCode, output, error);
            } catch (Exception e) {
                Session.getLog().error("Unusual response from script: ", e);
                return new SSHResult(-1, null, "Unusual response from script: " + response);
            }
        }
    }

    public void quit() throws Exception {
        this.outputStream.close();
        this.sshProcess.destroy();
    }

    public boolean isBusy() {
        return this.busy;
    }

    public void setBusy(boolean busy) {
        this.busy = busy;
    }

    protected void finalize() throws Throwable {
        this.sshProcess.destroy();
    }

    public void free() throws IOException {
        this.outputStream.writeBytes("QUIT\n");
        this.outputStream.flush();
        String response = this.inputStream.readLine();
        Session.getLog().debug(response);
        this.outputStream.close();
        this.outputStream = null;
        this.inputStream.close();
        this.inputStream = null;
        this.sshProcess.destroy();
    }

    public long getIdleTime() {
        return this.idleTime;
    }
}
