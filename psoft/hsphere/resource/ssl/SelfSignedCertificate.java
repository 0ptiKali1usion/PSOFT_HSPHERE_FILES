package psoft.hsphere.resource.ssl;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ssl/SelfSignedCertificate.class */
public class SelfSignedCertificate {
    String key;
    String req;
    String crt;
    public static final SelfSignedCertificate DEMO_CERT = new SelfSignedCertificate("-----BEGIN RSA PRIVATE KEY-----\nMIICWwIBAAKBgQDiIPyjohw3dooNilL3uHVgLYGkKjkIxsYogMBmUzJ3aDY3oD/G\nxy2QDhItNILt7TxmR/n5IAc8FgqeBcwEPHHg4ioNUvwcQtiZr4cygdZ2K0Y0Zy6J\nakarDHfTqFH3uzTVx9t7MpKNOZuSGhGpr63cgAWRn1VNwrkEXsnjbnhaRQIDAQAB\nAoGAW7m1wUqI15al+Ugaz5FrS1AqPkVCTWUUDGntoZQt7HHBTF8cf61btgt/JAcg\n9RI+Zd7cb0mmpaDrPZ5sW2uJZUQRREYtmcIVNoVHnTvybPHFqqgu+HIH8IdpQp4w\nIIt1tHKx++0MGAAcFMipzW40bnKQIjWzJjD4V13OPkF4ZiECQQD3vBXrdc0UHtUO\nFYc1x3IKPWeQqf6ztN7LgmCC2CooDiFB1VJ3IvTFyIYC4dyxf3N1ZCT1+e4+3PtS\n15m/7JSfAkEA6axdVQRsJvRjiagwO1gxxcuWs0uapoqvhkbIgr5NgHKW3Bdt2Gb+\n36OywcOoF1rk2kOLkzH3PewiGiUEbTrimwJACHx7773wUYEg4UOhhxkW8fzagF0i\naXuHqkcEEVdgUlDxmLS7B3O+GMxestiT28y24s2UaoyuOZ8OSO8zyBxNlwJAPp4b\n3J50xPOUgNz7H8wAenqWBbHq3Voosxjgvnh0mEkcuBnnK6heAFwDmPzvXGLVFNAo\n5Obs1EEk2lC1IPg4pQJAfF7/bfoYhUAksH0g7kqP1YlxFUz3aB4Mnp4ztCQJx+gd\n2nw1PBrtgNjpkvWXyVu7uKiSDos44tnahGqsT47SNA==\n-----END RSA PRIVATE KEY-----", "-----BEGIN CERTIFICATE REQUEST-----\nMIIBwjCCASsCAQAwgYExCzAJBgNVBAYTAlVTMQswCQYDVQQIEwJOWTEOMAwGA1UE\nBxMFMTEyMzUxDjAMBgNVBAoTBXBzb2Z0MQ4wDAYDVQQLEwVQU09GVDEWMBQGA1UE\nAxMNd3d3LnBzb2Z0Lm5ldDEdMBsGCSqGSIb3DQEJARYOaW5mb0Bwc29mdC5uZXQw\ngZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAOIg/KOiHDd2ig2KUve4dWAtgaQq\nOQjGxiiAwGZTMndoNjegP8bHLZAOEi00gu3tPGZH+fkgBzwWCp4FzAQ8ceDiKg1S\n/BxC2JmvhzKB1nYrRjRnLolqRqsMd9OoUfe7NNXH23syko05m5IaEamvrdyABZGf\nVU3CuQReyeNueFpFAgMBAAGgADANBgkqhkiG9w0BAQQFAAOBgQAlyH2DrL7P877y\nFFAqDG/zGoUOCURNLgA0IqbDUEc2arld1KZMPBW17Ksjyv1t4cj18VpWDtmwdXof\n6wl+P3iUd0DtyC5n5WJyFF5aUm+uYyy1OnrY8R+ZBncNSga/a5HsuBo7Maqbmpcs\n3n4vtDdm9IfxkErCYOd3+HTzwBRv0Q==\n-----END CERTIFICATE REQUEST-----", "-----BEGIN CERTIFICATE-----\nMIIC/DCCAmWgAwIBAgIBATANBgkqhkiG9w0BAQQFADCBmTELMAkGA1UEBhMCVVMx\nCzAJBgNVBAgTAk5ZMREwDwYDVQQHEwhOZXcgWW9yazEaMBgGA1UEChMRUG9zaXRp\ndmUgU29mdHdhcmUxGjAYBgNVBAsTEUluc3RhbGxhdGlvbiBUZWFtMRAwDgYDVQQD\nEwdIU3BoZXJlMSAwHgYJKoZIhvcNAQkBFhFpbnN0YWxsQHBzb2Z0Lm5ldDAeFw0w\nNTAyMTMyMTI2NDdaFw0wNjAyMTMyMTI2NDdaMIGBMQswCQYDVQQGEwJVUzELMAkG\nA1UECBMCTlkxDjAMBgNVBAcTBTExMjM1MQ4wDAYDVQQKEwVwc29mdDEOMAwGA1UE\nCxMFUFNPRlQxFjAUBgNVBAMTDXd3dy5wc29mdC5uZXQxHTAbBgkqhkiG9w0BCQEW\nDmluZm9AcHNvZnQubmV0MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDiIPyj\nohw3dooNilL3uHVgLYGkKjkIxsYogMBmUzJ3aDY3oD/Gxy2QDhItNILt7TxmR/n5\nIAc8FgqeBcwEPHHg4ioNUvwcQtiZr4cygdZ2K0Y0Zy6JakarDHfTqFH3uzTVx9t7\nMpKNOZuSGhGpr63cgAWRn1VNwrkEXsnjbnhaRQIDAQABo2owaDAZBgNVHREEEjAQ\ngQ5pbmZvQHBzb2Z0Lm5ldDA4BglghkgBhvhCAQ0EKxYpbW9kX3NzbCBnZW5lcmF0\nZWQgdGVzdCBzZXJ2ZXIgY2VydGlmaWNhdGUwEQYJYIZIAYb4QgEBBAQDAgZAMA0G\nCSqGSIb3DQEBBAUAA4GBAGI1O3lkD2ygP/Xbtx0DDARUOKighvJv4FX3O0fHG6qI\nqeO+qeWfesrEcGH8hStVlaZUKIL6B9gQl/iyvliD2zF66BfZI++MW8YW4A8TDom2\nf7nnIS/SsbjgSJJyXc8M3izeBeUtMkP4AUrVOzCpu3pEv/XcVxPnUabtTrHjJz6x\n-----END CERTIFICATE-----");

    public SelfSignedCertificate(String key, String req, String crt) {
        this.key = key;
        this.req = req;
        this.crt = crt;
    }

    public String getCrt() {
        return this.crt;
    }

    public String getReq() {
        return this.req;
    }

    public String getKey() {
        return this.key;
    }
}
