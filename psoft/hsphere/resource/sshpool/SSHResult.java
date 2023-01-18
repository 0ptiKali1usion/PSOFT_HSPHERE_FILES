package psoft.hsphere.resource.sshpool;

/* loaded from: hsphere.zip:psoft/hsphere/resource/sshpool/SSHResult.class */
public class SSHResult {
    protected int exitCode;
    protected String output;
    protected String error;

    public SSHResult(int exitCode, String output, String error) {
        this.exitCode = exitCode;
        this.output = output;
        this.error = error;
    }

    public int getExitCode() {
        return this.exitCode;
    }

    public String getOutput() {
        return this.output == null ? "" : this.output;
    }

    public String getError() {
        return this.error == null ? "" : this.error;
    }
}
