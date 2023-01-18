package psoft.encryption;

/* loaded from: hsphere.zip:psoft/encryption/MD5.class */
public class MD5 {
    MD5State state;
    MD5State finals;
    static byte[] padding = {Byte.MIN_VALUE, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    public synchronized void Init() {
        this.state = new MD5State();
        this.finals = null;
    }

    public MD5() {
        Init();
    }

    public MD5(Object ob) {
        this();
        Update(ob.toString());
    }

    private int rotate_left(int x, int n) {
        return (x << n) | (x >>> (32 - n));
    }

    private int uadd(int a, int b) {
        long aa = a & 4294967295L;
        long bb = b & 4294967295L;
        return (int) ((aa + bb) & 4294967295L);
    }

    private int uadd(int a, int b, int c) {
        return uadd(uadd(a, b), c);
    }

    private int uadd(int a, int b, int c, int d) {
        return uadd(uadd(a, b, c), d);
    }

    /* renamed from: FF */
    private int m40FF(int a, int b, int c, int d, int x, int s, int ac) {
        return uadd(rotate_left(uadd(a, (b & c) | ((b ^ (-1)) & d), x, ac), s), b);
    }

    /* renamed from: GG */
    private int m39GG(int a, int b, int c, int d, int x, int s, int ac) {
        return uadd(rotate_left(uadd(a, (b & d) | (c & (d ^ (-1))), x, ac), s), b);
    }

    /* renamed from: HH */
    private int m38HH(int a, int b, int c, int d, int x, int s, int ac) {
        return uadd(rotate_left(uadd(a, (b ^ c) ^ d, x, ac), s), b);
    }

    /* renamed from: II */
    private int m37II(int a, int b, int c, int d, int x, int s, int ac) {
        return uadd(rotate_left(uadd(a, c ^ (b | (d ^ (-1))), x, ac), s), b);
    }

    private int[] Decode(byte[] buffer, int len, int shift) {
        int[] out = new int[16];
        int i = 0;
        for (int j = 0; j < len; j += 4) {
            out[i] = (buffer[j + shift] & 255) | ((buffer[(j + 1) + shift] & 255) << 8) | ((buffer[(j + 2) + shift] & 255) << 16) | ((buffer[(j + 3) + shift] & 255) << 24);
            i++;
        }
        return out;
    }

    private void Transform(MD5State state, byte[] buffer, int shift) {
        int a = state.state[0];
        int b = state.state[1];
        int c = state.state[2];
        int d = state.state[3];
        int[] x = Decode(buffer, 64, shift);
        int a2 = m40FF(a, b, c, d, x[0], 7, -680876936);
        int d2 = m40FF(d, a2, b, c, x[1], 12, -389564586);
        int c2 = m40FF(c, d2, a2, b, x[2], 17, 606105819);
        int b2 = m40FF(b, c2, d2, a2, x[3], 22, -1044525330);
        int a3 = m40FF(a2, b2, c2, d2, x[4], 7, -176418897);
        int d3 = m40FF(d2, a3, b2, c2, x[5], 12, 1200080426);
        int c3 = m40FF(c2, d3, a3, b2, x[6], 17, -1473231341);
        int b3 = m40FF(b2, c3, d3, a3, x[7], 22, -45705983);
        int a4 = m40FF(a3, b3, c3, d3, x[8], 7, 1770035416);
        int d4 = m40FF(d3, a4, b3, c3, x[9], 12, -1958414417);
        int c4 = m40FF(c3, d4, a4, b3, x[10], 17, -42063);
        int b4 = m40FF(b3, c4, d4, a4, x[11], 22, -1990404162);
        int a5 = m40FF(a4, b4, c4, d4, x[12], 7, 1804603682);
        int d5 = m40FF(d4, a5, b4, c4, x[13], 12, -40341101);
        int c5 = m40FF(c4, d5, a5, b4, x[14], 17, -1502002290);
        int b5 = m40FF(b4, c5, d5, a5, x[15], 22, 1236535329);
        int a6 = m39GG(a5, b5, c5, d5, x[1], 5, -165796510);
        int d6 = m39GG(d5, a6, b5, c5, x[6], 9, -1069501632);
        int c6 = m39GG(c5, d6, a6, b5, x[11], 14, 643717713);
        int b6 = m39GG(b5, c6, d6, a6, x[0], 20, -373897302);
        int a7 = m39GG(a6, b6, c6, d6, x[5], 5, -701558691);
        int d7 = m39GG(d6, a7, b6, c6, x[10], 9, 38016083);
        int c7 = m39GG(c6, d7, a7, b6, x[15], 14, -660478335);
        int b7 = m39GG(b6, c7, d7, a7, x[4], 20, -405537848);
        int a8 = m39GG(a7, b7, c7, d7, x[9], 5, 568446438);
        int d8 = m39GG(d7, a8, b7, c7, x[14], 9, -1019803690);
        int c8 = m39GG(c7, d8, a8, b7, x[3], 14, -187363961);
        int b8 = m39GG(b7, c8, d8, a8, x[8], 20, 1163531501);
        int a9 = m39GG(a8, b8, c8, d8, x[13], 5, -1444681467);
        int d9 = m39GG(d8, a9, b8, c8, x[2], 9, -51403784);
        int c9 = m39GG(c8, d9, a9, b8, x[7], 14, 1735328473);
        int b9 = m39GG(b8, c9, d9, a9, x[12], 20, -1926607734);
        int a10 = m38HH(a9, b9, c9, d9, x[5], 4, -378558);
        int d10 = m38HH(d9, a10, b9, c9, x[8], 11, -2022574463);
        int c10 = m38HH(c9, d10, a10, b9, x[11], 16, 1839030562);
        int b10 = m38HH(b9, c10, d10, a10, x[14], 23, -35309556);
        int a11 = m38HH(a10, b10, c10, d10, x[1], 4, -1530992060);
        int d11 = m38HH(d10, a11, b10, c10, x[4], 11, 1272893353);
        int c11 = m38HH(c10, d11, a11, b10, x[7], 16, -155497632);
        int b11 = m38HH(b10, c11, d11, a11, x[10], 23, -1094730640);
        int a12 = m38HH(a11, b11, c11, d11, x[13], 4, 681279174);
        int d12 = m38HH(d11, a12, b11, c11, x[0], 11, -358537222);
        int c12 = m38HH(c11, d12, a12, b11, x[3], 16, -722521979);
        int b12 = m38HH(b11, c12, d12, a12, x[6], 23, 76029189);
        int a13 = m38HH(a12, b12, c12, d12, x[9], 4, -640364487);
        int d13 = m38HH(d12, a13, b12, c12, x[12], 11, -421815835);
        int c13 = m38HH(c12, d13, a13, b12, x[15], 16, 530742520);
        int b13 = m38HH(b12, c13, d13, a13, x[2], 23, -995338651);
        int a14 = m37II(a13, b13, c13, d13, x[0], 6, -198630844);
        int d14 = m37II(d13, a14, b13, c13, x[7], 10, 1126891415);
        int c14 = m37II(c13, d14, a14, b13, x[14], 15, -1416354905);
        int b14 = m37II(b13, c14, d14, a14, x[5], 21, -57434055);
        int a15 = m37II(a14, b14, c14, d14, x[12], 6, 1700485571);
        int d15 = m37II(d14, a15, b14, c14, x[3], 10, -1894986606);
        int c15 = m37II(c14, d15, a15, b14, x[10], 15, -1051523);
        int b15 = m37II(b14, c15, d15, a15, x[1], 21, -2054922799);
        int a16 = m37II(a15, b15, c15, d15, x[8], 6, 1873313359);
        int d16 = m37II(d15, a16, b15, c15, x[15], 10, -30611744);
        int c16 = m37II(c15, d16, a16, b15, x[6], 15, -1560198380);
        int b16 = m37II(b15, c16, d16, a16, x[13], 21, 1309151649);
        int a17 = m37II(a16, b16, c16, d16, x[4], 6, -145523070);
        int d17 = m37II(d16, a17, b16, c16, x[11], 10, -1120210379);
        int c17 = m37II(c16, d17, a17, b16, x[2], 15, 718787259);
        int b17 = m37II(b16, c17, d17, a17, x[9], 21, -343485551);
        int[] iArr = state.state;
        iArr[0] = iArr[0] + a17;
        int[] iArr2 = state.state;
        iArr2[1] = iArr2[1] + b17;
        int[] iArr3 = state.state;
        iArr3[2] = iArr3[2] + c17;
        int[] iArr4 = state.state;
        iArr4[3] = iArr4[3] + d17;
    }

    public void Update(MD5State stat, byte[] buffer, int offset, int length) {
        int i;
        this.finals = null;
        if (length - offset > buffer.length) {
            length = buffer.length - offset;
        }
        int index = (stat.count[0] >>> 3) & 63;
        int[] iArr = stat.count;
        int i2 = iArr[0] + (length << 3);
        iArr[0] = i2;
        if (i2 < (length << 3)) {
            int[] iArr2 = stat.count;
            iArr2[1] = iArr2[1] + 1;
        }
        int[] iArr3 = stat.count;
        iArr3[1] = iArr3[1] + (length >>> 29);
        int partlen = 64 - index;
        if (length >= partlen) {
            for (int i3 = 0; i3 < partlen; i3++) {
                stat.buffer[i3 + index] = buffer[i3 + offset];
            }
            Transform(stat, stat.buffer, 0);
            i = partlen;
            while (i + 63 < length) {
                Transform(stat, buffer, i);
                i += 64;
            }
            index = 0;
        } else {
            i = 0;
        }
        if (i < length) {
            int start = i;
            while (i < length) {
                stat.buffer[(index + i) - start] = buffer[i + offset];
                i++;
            }
        }
    }

    public void Update(byte[] buffer, int offset, int length) {
        Update(this.state, buffer, offset, length);
    }

    public void Update(byte[] buffer, int length) {
        Update(this.state, buffer, 0, length);
    }

    public void Update(byte[] buffer) {
        Update(buffer, 0, buffer.length);
    }

    public void Update(byte b) {
        byte[] buffer = {b};
        Update(buffer, 1);
    }

    public void Update(String s) {
        byte[] chars = new byte[s.length()];
        s.getBytes(0, s.length(), chars, 0);
        Update(chars, chars.length);
    }

    public void Update(int i) {
        Update((byte) (i & 255));
    }

    private byte[] Encode(int[] input, int len) {
        byte[] out = new byte[len];
        int i = 0;
        for (int j = 0; j < len; j += 4) {
            out[j] = (byte) (input[i] & 255);
            out[j + 1] = (byte) ((input[i] >>> 8) & 255);
            out[j + 2] = (byte) ((input[i] >>> 16) & 255);
            out[j + 3] = (byte) ((input[i] >>> 24) & 255);
            i++;
        }
        return out;
    }

    public synchronized byte[] Final() {
        if (this.finals == null) {
            MD5State fin = new MD5State(this.state);
            byte[] bits = Encode(fin.count, 8);
            int index = (fin.count[0] >>> 3) & 63;
            int padlen = index < 56 ? 56 - index : 120 - index;
            Update(fin, padding, 0, padlen);
            Update(fin, bits, 0, 8);
            this.finals = fin;
        }
        return Encode(this.finals.state, 16);
    }

    public static String asHex(byte[] hash) {
        StringBuffer buf = new StringBuffer(hash.length * 2);
        for (int i = 0; i < hash.length; i++) {
            if ((hash[i] & 255) < 16) {
                buf.append("0");
            }
            buf.append(Long.toString(hash[i] & 255, 16));
        }
        return buf.toString();
    }

    public String asHex() {
        return asHex(Final());
    }
}
