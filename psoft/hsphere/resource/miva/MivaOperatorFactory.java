package psoft.hsphere.resource.miva;

import psoft.hsphere.Resource;

/* loaded from: hsphere.zip:psoft/hsphere/resource/miva/MivaOperatorFactory.class */
public class MivaOperatorFactory {
    private static MivaOperatorFactory ourInstance = new MivaOperatorFactory();

    public static MivaOperatorFactory getInstance() {
        return ourInstance;
    }

    private MivaOperatorFactory() {
    }

    public MivaOperator getMivaOperator(String mivaVersion, int platform, Resource mivaResource) throws Exception {
        if ("4.14".equals(mivaVersion)) {
            return getMivaOperator(2, platform, mivaResource);
        }
        if ("5.0".equals(mivaVersion)) {
            return getMivaOperator(3, platform, mivaResource);
        }
        throw new Exception("Unsupported Miva version " + mivaVersion);
    }

    public MivaOperator getMivaOperator(int mivaVersion, int platform, Resource mivaResource) throws Exception {
        MivaOperator mop;
        if (mivaVersion == 2) {
            if (platform == 1) {
                mop = new Miva4UnixOperator();
            } else if (platform == 2) {
                mop = new Miva4WinOperator();
            } else {
                throw new Exception("Unsupported Miva platform");
            }
        } else if (mivaVersion == 3) {
            if (platform == 1) {
                mop = new Miva5UnixOperator();
            } else if (platform == 2) {
                mop = new Miva5WinOperator();
            } else {
                throw new Exception("Unsupported Miva platform");
            }
        } else {
            throw new Exception("Unsupported Miva version");
        }
        mop.setMivaResource(mivaResource);
        return mop;
    }
}
