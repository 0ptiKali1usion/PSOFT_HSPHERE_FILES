package psoft.hsphere.fmacl;

import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.Arrays;
import org.apache.log4j.Category;
import psoft.hsphere.AccessTemplateMethodWrapper;
import psoft.hsphere.HSUserException;
import psoft.hsphere.Session;
import psoft.hsphere.ipmanagement.IPCalc;
import psoft.hsphere.ipmanagement.IPRange;
import psoft.hsphere.ipmanagement.IPSubnet;
import psoft.hsphere.ipmanagement.IPSubnetManager;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/fmacl/IPSubnetManagerAccessor.class */
public class IPSubnetManagerAccessor implements TemplateHashModel {
    private static Category log = Category.getInstance(IPSubnetManagerAccessor.class.getName());
    public static final TemplateString STATUS_OK = new TemplateString("OK");

    public IPSubnet FM_calculateSubnetMask(String startIP, String endIP, String netmask) throws Exception {
        return IPSubnetManager.getInstance().calculateSubnet(startIP, endIP, netmask);
    }

    public IPSubnet FM_calculateSubnetSlash(String startIP, String endIP, int netmask) throws Exception {
        return IPSubnetManager.getInstance().calculateSubnet(startIP, endIP, netmask);
    }

    public IPSubnet FM_calculateSubnet(String startIP, String endIP) throws Exception {
        return IPSubnetManager.getInstance().calculateSubnet(startIP, endIP);
    }

    public IPSubnet FM_saveNewSubnet(long subnetId) throws Exception {
        log.debug("Inside IPSubnetManagerAccessor::FM_saveNewSubnet");
        IPSubnet _ips = IPSubnetManager.getInstance().getSubnet(subnetId);
        return IPSubnetManager.getInstance().saveNewSubnet(_ips);
    }

    public TemplateModel FM_getSubnets() throws Exception {
        return new TemplateList(IPSubnetManager.getInstance().getSubnets());
    }

    public IPSubnet FM_getSubnet(long id) {
        try {
            return IPSubnetManager.getInstance().getSubnet(id);
        } catch (Exception ex) {
            Session.getLog().error("Unable to get subnet with id " + id, ex);
            return null;
        }
    }

    public TemplateModel FM_splitRange(long subnetId, long rangeId, String limit) throws Exception {
        log.debug("Inside FM_splitRange sunbet_id=" + subnetId + " rangeId=" + rangeId + " limit=" + limit);
        long _limit = IPCalc.getInstance().string2IP(limit);
        IPSubnet ips = IPSubnetManager.getInstance().getSubnet(subnetId);
        IPRange ipr = ips.getRange(rangeId);
        if (ipr.containsIP(_limit)) {
            IPSubnetManager.getInstance().splitRange(ips, ipr, limit);
            return STATUS_OK;
        }
        throw new HSUserException("msg.ds_ip_pool.range_can_not_split_by_ip", new String[]{ipr.toString(), limit});
    }

    public TemplateModel FM_mergeRanges(long subnetId, long range1Id, long range2Id) throws Exception {
        log.debug("Inside FM_mergeRanges");
        IPSubnet ips = IPSubnetManager.getInstance().getSubnet(subnetId);
        IPRange ipr1 = ips.getRange(range1Id);
        if (ipr1 == null) {
            throw new Exception("Unable to get range with id " + range1Id + " for subnet with id " + subnetId);
        }
        IPRange ipr2 = ips.getRange(range2Id);
        if (ipr1 == null) {
            throw new Exception("Unable to get range with id " + range2Id + " for subnet with id " + subnetId);
        }
        IPRange[] ranges = {ipr1, ipr2};
        Arrays.sort(ranges);
        if (ranges[0].getEndIPasLong() + 1 == ranges[1].getStartIPasLong()) {
            if (ranges[0].getType() == ranges[1].getType()) {
                IPSubnetManager.getInstance().mergeRanges(ips, ranges[0], ranges[1]);
                return STATUS_OK;
            }
            throw new HSUserException("msg.ds_ip_pool.ranges_can_not_be_merged_type", new String[]{ranges[0].toString(), ranges[1].toString()});
        }
        throw new HSUserException("msg.ds_ip_pool.ranges_can_not_be_merged_limit", new String[]{ranges[0].toString(), ranges[1].toString()});
    }

    public TemplateModel get(String key) throws TemplateModelException {
        return AccessTemplateMethodWrapper.getMethod(this, key);
    }

    public boolean isEmpty() throws TemplateModelException {
        return false;
    }

    public TemplateModel FM_getRangesByDS(long dsId) throws Exception {
        return new TemplateList(IPSubnetManager.getInstance().getRangesByDS(dsId));
    }

    public TemplateModel FM_assignRange(long subnetId, long rangeId, long dsId, String usageInfo) throws Exception {
        try {
            IPSubnetManager.getInstance().changeRangeSet(subnetId, rangeId, dsId, IPSubnetManager.ACTION_ASSIGN);
            IPSubnetManager.getInstance().updateIPRangeUsageInfo(rangeId, usageInfo);
            return STATUS_OK;
        } catch (Exception ex) {
            Session.addMessage(ex.getMessage());
            throw ex;
        }
    }

    public TemplateModel FM_unassignRange(long subnetId, long rangeId, long dsId) throws Exception {
        IPSubnetManager.getInstance().changeRangeSet(subnetId, rangeId, dsId, IPSubnetManager.ACTION_UNASSIGN);
        return STATUS_OK;
    }

    public TemplateModel FM_deleteIPSubnet(long subnetId) throws Exception {
        IPSubnet ips = IPSubnetManager.getInstance().getSubnet(subnetId);
        synchronized (ips) {
            if (ips.canBeDeleted()) {
                if (ips.getResellerId() == Session.getResellerId()) {
                    IPSubnetManager.getInstance().delete(ips);
                } else {
                    throw new HSUserException("msg.exception.ds_ip_pool.delete_range.not_an_owner");
                }
            } else {
                throw new HSUserException("msg.exception.ds_ip_pool.delete_range.taken_ranges_available");
            }
        }
        return STATUS_OK;
    }

    public TemplateModel FM_getAssignedRangeInfo(long iprId) throws Exception {
        return new TemplateHash(IPSubnetManager.getInstance().getAssignedRangeInfo(iprId));
    }
}
