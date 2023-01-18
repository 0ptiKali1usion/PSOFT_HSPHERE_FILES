package psoft.encryption;

/* compiled from: MD5.java */
/* loaded from: hsphere.zip:psoft/encryption/MD5State.class */
class MD5State {
    int[] state;
    int[] count;
    byte[] buffer;

    public MD5State() {
        this.buffer = new byte[64];
        this.count = new int[2];
        this.state = new int[4];
        this.state[0] = 1732584193;
        this.state[1] = -271733879;
        this.state[2] = -1732584194;
        this.state[3] = 271733878;
        int[] iArr = this.count;
        this.count[1] = 0;
        iArr[0] = 0;
    }

    public MD5State(MD5State from) {
        this();
        for (int i = 0; i < this.buffer.length; i++) {
            this.buffer[i] = from.buffer[i];
        }
        for (int i2 = 0; i2 < this.state.length; i2++) {
            this.state[i2] = from.state[i2];
        }
        for (int i3 = 0; i3 < this.count.length; i3++) {
            this.count[i3] = from.count[i3];
        }
    }
}
