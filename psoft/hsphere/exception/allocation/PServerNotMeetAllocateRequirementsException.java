package psoft.hsphere.exception.allocation;

import psoft.hsphere.Localizer;

/* loaded from: hsphere.zip:psoft/hsphere/exception/allocation/PServerNotMeetAllocateRequirementsException.class */
public class PServerNotMeetAllocateRequirementsException extends Exception {
    public static final int SERVICE_MISSING = 1;
    public static final int LSERVER_MISSING = 2;
    public static final int LSERVER_IN_USE = 3;
    private int typeOfInconsistence;

    public PServerNotMeetAllocateRequirementsException(long pServerId) {
        super(Localizer.translateMessage("msg.exception.allocate.services_inconsistence", new String[]{Long.toString(pServerId)}));
        this.typeOfInconsistence = -1;
    }

    public PServerNotMeetAllocateRequirementsException(long pServerId, int serviceId, int _typeOfInconsistence) {
        super(Localizer.translateMessage(getMainExceptionMessage(_typeOfInconsistence), new String[]{Long.toString(pServerId), getServiceDescription(serviceId)}));
        this.typeOfInconsistence = -1;
        this.typeOfInconsistence = _typeOfInconsistence;
    }

    public PServerNotMeetAllocateRequirementsException(long pServerId, long lServerId) {
        super(Localizer.translateMessage("msg.exception.allocation.lserver_of_pserver_in_use", new String[]{Long.toString(pServerId), Long.toString(lServerId)}));
        this.typeOfInconsistence = -1;
        this.typeOfInconsistence = 3;
    }

    private static String getMainExceptionMessage(int _typeOfInconsistence) {
        if (_typeOfInconsistence == 1) {
            return "msg.exception.allocate.service_missing";
        }
        if (_typeOfInconsistence == 2) {
            return "msg.exception.allocate.lserver_missing";
        }
        return null;
    }

    public int getTypeOfInconsistence() {
        return this.typeOfInconsistence;
    }

    private static String getServiceDescription(int serviceId) {
        switch (serviceId) {
            case 1:
                return Localizer.translateMessage("msg.services.web");
            case 2:
            default:
                return null;
            case 3:
                return Localizer.translateMessage("msg.services.mail");
            case 4:
                return Localizer.translateMessage("msg.services.mysql");
        }
    }
}
