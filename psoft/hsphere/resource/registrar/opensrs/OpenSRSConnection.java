package psoft.hsphere.resource.registrar.opensrs;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import org.xml.sax.SAXParseException;
import psoft.hsphere.Session;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.hsphere.resource.registrar.opensrs.xcp.CBC;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPCommands;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPMessage;
import psoft.hsphere.resource.registrar.opensrs.xcp.XCPParser;

/* loaded from: hsphere.zip:psoft/hsphere/resource/registrar/opensrs/OpenSRSConnection.class */
public class OpenSRSConnection {
    public String username;
    String host;
    int port;
    Socket socket;
    DataOutputStream out;

    /* renamed from: in */
    DataInputStream f210in;
    CBC cbc;
    boolean connected = false;
    boolean debugMode;

    public OpenSRSConnection(String username, String key, String host, int port) throws RegistrarException {
        this.debugMode = false;
        try {
            this.username = username;
            this.cbc = new CBC(key);
            this.host = host;
            this.port = port;
            this.debugMode = "TRUE".equals(Session.getPropertyString("REGISTRAR_DEBUG_MODE"));
            open();
        } catch (Exception ex) {
            Session.getLog().debug("OpenSRSConnection: ", ex);
            if (ex instanceof RegistrarException) {
                throw ((RegistrarException) ex);
            }
            RegistrarException re = new RegistrarException(RegistrarException.OTHER_ERROR, ex.getMessage());
            re.fillInStackTrace();
            throw re;
        }
    }

    public boolean isConnected() {
        return this.connected;
    }

    public void open() throws Exception {
        if (isConnected()) {
            return;
        }
        try {
            this.socket = new Socket(this.host, this.port);
            this.socket.setSoTimeout(120000);
            this.out = new DataOutputStream(this.socket.getOutputStream());
            this.f210in = new DataInputStream(this.socket.getInputStream());
            Session.getLog().debug("Inside OpenSRSConnection::open, socket opened, in and out data streams are received ");
            handshake();
            this.connected = true;
        } catch (UnknownHostException e) {
            throw new RegistrarException(RegistrarException.UNKNOWN_HOST, "Unable to connect to server:" + this.host + " port:" + this.port + " IP address of a host could not be determined");
        } catch (IOException ex) {
            Session.getLog().debug("OpenSRSConnection: ", ex);
            throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, "Unable to connect to server:" + this.host + " port:" + this.port);
        }
    }

    public void close() throws Exception {
        try {
            this.out.close();
            this.f210in.close();
            this.socket.close();
            this.connected = false;
        } catch (Throwable th) {
            this.connected = false;
            throw th;
        }
    }

    protected void sendRaw(byte[] msg) throws Exception {
        this.out.write(("Content-Length: " + msg.length + "\r\n\r\n").getBytes());
        this.out.write(msg);
        this.out.flush();
    }

    protected void sendRaw(XCPMessage msg) throws Exception {
        sendRaw(msg.toString());
    }

    protected void sendRaw(String msg) throws Exception {
        sendRaw(msg.getBytes());
    }

    public void send(XCPMessage msg) throws Exception {
        if (this.debugMode) {
            Session.getLog().debug("\n-----MESSAGE TO OPENSRS SERVER BEGIN-----\n" + msg.toString() + "\n\n-----MESSAGE TO OPENSRS SERVER END-----");
        }
        sendRaw(this.cbc.encrypt(msg.toString().getBytes()));
    }

    protected byte[] receiveBytesRaw() throws IOException, RegistrarException {
        String content = this.f210in.readLine();
        if (content == null) {
            Session.getLog().error("OpenSRS server returned an empty response");
            throw new RegistrarException(-1001, "The server :" + this.host + " return empty response");
        }
        Session.getLog().debug("Checking input \"Content-Length: \" is " + content.indexOf("Content-Length: ") + "\n input=\n" + content);
        if (content.indexOf("Content-Length: ") >= 0) {
            int contentLength = Integer.parseInt(content.substring("Content-Length: ".length()));
            byte[] buf = new byte[contentLength];
            this.f210in.readFully(buf, 0, 2);
            this.f210in.readFully(buf);
            return buf;
        }
        throw new RegistrarException(RegistrarException.UNABLE_TO_CONNECT, "Unable to connect to registrar server. Administrator should check hostname in registrar settings");
    }

    protected String receiveStringRaw() throws Exception {
        return new String(receiveBytesRaw());
    }

    protected byte[] receiveBytes() throws Exception {
        return this.cbc.decrypt(receiveBytesRaw());
    }

    protected String receive() throws Exception {
        String response = new String(receiveBytes());
        if (this.debugMode) {
            Session.getLog().debug("\n-----RESPONSE FROM OPENSRS SERVER BEGIN-----\n" + response + "\n-----RESPONSE FROM OPENSRS SERVER END-----");
        }
        return response;
    }

    public XCPParser receiveXCP() throws Exception {
        String response = receive();
        try {
            return XCPParser.getInstance(response);
        } catch (SAXParseException e) {
            Session.getLog().debug("\n-----INVALID RESPONSE FROM OPENSRS SERVER BEGIN-----\n" + response + "\n-----INVALID RESPONSE FROM OPENSRS SERVER END-----");
            throw new RegistrarException(RegistrarException.OPERATION_FAILED, "The server :" + this.host + " return invalid response:" + response);
        }
    }

    protected void handshake() throws Exception {
        receiveBytesRaw();
        sendRaw(XCPCommands.checkVersion());
        sendRaw(XCPCommands.auth(this.username));
        challenge();
    }

    protected void challenge() throws Exception {
        sendRaw(this.cbc.encrypt(this.cbc.challenge(receiveBytesRaw())));
        try {
            int responseCode = receiveXCP().getResponseCode();
            if (responseCode != 200) {
                throw new RegistrarException(responseCode, "Error getting connection ");
            }
        } catch (RegistrarException ex) {
            throw new RegistrarException(-1001, ex.getMessage() + "\nProbabaly you have invalid key");
        }
    }
}
