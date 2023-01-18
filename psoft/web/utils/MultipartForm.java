package psoft.web.utils;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import javax.servlet.ServletRequest;
import psoft.hsphere.resource.StreamReadingThread;

/* loaded from: hsphere.zip:psoft/web/utils/MultipartForm.class */
public class MultipartForm {

    /* renamed from: in */
    private InputStream f282in;
    private String delimiter;
    private byte[] buffer = new byte[StreamReadingThread.BUF_SIZE];
    private String filename = null;
    private boolean isFile = false;
    private String contentType = "ISO-8859-1";

    public boolean isFile() {
        return this.isFile;
    }

    public MultipartForm(ServletRequest req) throws IOException {
        this.delimiter = null;
        this.f282in = new BufferedInputStream(req.getInputStream());
        this.delimiter = req.getContentType();
        if (this.delimiter.indexOf("boundary=") != -1) {
            this.delimiter = "--" + this.delimiter.substring(this.delimiter.indexOf("boundary=") + 9, this.delimiter.length());
        }
    }

    private String readLine() throws IOException {
        int size = readLine(this.buffer, 0, this.buffer.length);
        if (size != -1) {
            return new String(this.buffer, 0, size, this.contentType);
        }
        return null;
    }

    public String getFilename() {
        return this.filename;
    }

    public String getParameter() throws IOException {
        return readLine().trim();
    }

    public String nextParameter() throws IOException {
        int end;
        while (true) {
            String line = readLine();
            if (line != null) {
                int start = line.indexOf("name=");
                if (start != -1 && (end = line.indexOf("\"", start + 6)) != -1) {
                    String name = line.substring(start + 6, end);
                    if (line.indexOf("filename=") != -1) {
                        this.isFile = true;
                        int start2 = line.indexOf("filename=") + 10;
                        this.filename = line.substring(start2, line.indexOf("\"", start2));
                        int start3 = this.filename.lastIndexOf("\\");
                        if (start3 != -1) {
                            this.filename = this.filename.substring(start3 + 1);
                        }
                        if (this.filename.length() == 0) {
                            this.filename = null;
                        }
                    } else {
                        this.isFile = false;
                    }
                    if (readLine().indexOf("Content-Type") != -1) {
                        readLine();
                    }
                    return name.trim();
                }
            } else {
                return null;
            }
        }
    }

    public void writeParameter(Writer w) throws IOException {
        while (true) {
            int size = readLine(this.buffer, 0, this.buffer.length);
            if (size == -1 || (this.buffer[0] == 45 && new String(this.buffer, 0, size, this.contentType).indexOf(this.delimiter) == 0)) {
                break;
            }
            System.err.println("Writing out " + size + " bytes");
            for (int i = 0; i < size; i++) {
                w.write((char) this.buffer[i]);
            }
        }
        w.flush();
    }

    private int readLine(byte[] b, int off, int len) throws IOException {
        int c;
        if (len <= 0) {
            return 0;
        }
        int count = 0;
        while (off < len && (c = this.f282in.read()) != -1) {
            int i = off;
            off++;
            b[i] = (byte) c;
            count++;
            if (c == 10) {
                break;
            }
        }
        if (count > 0) {
            return count;
        }
        return -1;
    }

    public long writeParameter(OutputStream out) throws IOException {
        long total;
        byte[] tmp_buffer = null;
        int tmp_size = 0;
        long j = 0;
        while (true) {
            total = j;
            int size = readLine(this.buffer, 0, this.buffer.length);
            if (size == -1 || (this.buffer[0] == 45 && new String(this.buffer, 0, size, this.contentType).indexOf(this.delimiter) == 0)) {
                break;
            }
            if (tmp_buffer != null) {
                out.write(tmp_buffer, 0, tmp_size);
            }
            tmp_buffer = (byte[]) this.buffer.clone();
            tmp_size = size;
            j = total + size;
        }
        if (tmp_buffer != null) {
            out.write(tmp_buffer, 0, tmp_size - 2);
        }
        out.flush();
        System.err.println("size ->" + (total - 2));
        return total - 2;
    }
}
