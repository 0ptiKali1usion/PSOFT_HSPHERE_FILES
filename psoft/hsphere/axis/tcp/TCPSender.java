package psoft.hsphere.axis.tcp;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import org.apache.axis.AxisFault;
import org.apache.axis.Message;
import org.apache.axis.MessageContext;
import org.apache.axis.handlers.BasicHandler;
import psoft.hsphere.Session;
import psoft.hsphere.admin.LanguageManager;

/* loaded from: hsphere.zip:psoft/hsphere/axis/tcp/TCPSender.class */
public class TCPSender extends BasicHandler {
    public static final String CONNECTION_RESET = "Connection reset";
    public static final String BROKEN_PIPE = "Broken pipe";
    protected static final int RESPONSE_TIMEOUT = 1200000;
    protected static final int CONNECTION_TIMEOUT = 60000;
    protected static final int BUFFER_SIZE = 1024;
    protected Socket socket = null;
    protected InputStream inputStream;
    protected OutputStream outputStream;

    public void invoke(MessageContext msgContext) throws AxisFault {
        int index;
        Message message = msgContext.getRequestMessage();
        Session.getLog().debug("SOAP request size: " + message.getContentLength() + " bytes");
        try {
            String host = msgContext.getStrProp(TCPTransport.HOST);
            int port = Integer.parseInt(msgContext.getStrProp(TCPTransport.PORT));
            if (this.socket == null) {
                openSocket(host, port);
            }
            message.writeTo(this.outputStream);
            this.outputStream.flush();
            StringBuffer response = new StringBuffer();
            byte[] input = new byte[BUFFER_SIZE];
            int i = 0;
            do {
                this.socket.setSoTimeout(RESPONSE_TIMEOUT);
                int length = this.inputStream.read(input, 0, BUFFER_SIZE);
                if (length > 0) {
                    response.append(new String(input, 0, length, LanguageManager.STANDARD_CHARSET));
                    int i2 = i;
                    i++;
                    if (i2 <= 64) {
                        index = response.toString().indexOf("Envelope>");
                    } else {
                        throw new IOException("SOAP response is to big");
                    }
                } else {
                    throw new IOException(CONNECTION_RESET);
                }
            } while (index < 0);
            String soapResponse = response.substring(0, index + "Envelope>".length());
            Session.getLog().debug("SOAP response size: " + soapResponse.length() + " bytes");
            msgContext.setResponseMessage(new Message(soapResponse));
        } catch (Exception e) {
            try {
                free();
            } catch (IOException e2) {
            }
            if (e.getMessage().equals(CONNECTION_RESET) || e.getMessage().equals(BROKEN_PIPE)) {
                Session.getLog().debug(e.getMessage());
                throw new AxisFault(CONNECTION_RESET);
            }
            throw AxisFault.makeFault(e);
        }
    }

    protected void openSocket(String host, int port) throws IOException {
        try {
            if (this.socket != null) {
                this.socket.close();
            }
            Session.getLog().debug("Open SOAP connection to " + host + ":" + port);
            this.socket = new Socket(host, port);
            this.outputStream = this.socket.getOutputStream();
            this.inputStream = new BufferedInputStream(this.socket.getInputStream());
            this.socket.setSoTimeout(60000);
            byte[] input = new byte[BUFFER_SIZE];
            int length = this.inputStream.read(input);
            String response = new String(input, 0, length);
            Session.getLog().debug(new String(response));
        } catch (SocketException ex) {
            this.socket = null;
            throw new AxisFault("Cannot create connection to " + host + ":" + port + "; " + ex.getMessage());
        }
    }

    public void free() throws IOException {
        try {
            if (this.socket != null) {
                this.socket.close();
            }
        } finally {
            this.socket = null;
            this.inputStream = null;
            this.outputStream = null;
        }
    }
}
