package psoft.hsphere.axis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Category;
import psoft.hsphere.Account;
import psoft.hsphere.ResourceId;
import psoft.hsphere.resource.Domain;
import psoft.hsphere.resource.UnixMixedIPResource;
import psoft.hsphere.resource.admin.AdmCustomDNSRecord;
import psoft.hsphere.resource.admin.AdmDNSManager;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.AdmInstantAlias;
import psoft.hsphere.resource.admin.ResellerCpAlias;
import psoft.hsphere.resource.dns.CustomDNSRecord;
import psoft.hsphere.resource.dns.DNSRecord;
import psoft.hsphere.resource.dns.DNSZone;
import psoft.hsphere.resource.email.MailService;

/* loaded from: hsphere.zip:psoft/hsphere/axis/DNSServices.class */
public class DNSServices {
    private static Category log = Category.getInstance(DNSServices.class.getName());

    public String getDescription() {
        return "Functions to work with DNS";
    }

    private AdmDNSManager getAdmDNSmanager(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        ResourceId rid = a.getId().findChild("adnsmanager");
        if (rid == null) {
            throw new Exception("Failed to find adnsmanager");
        }
        AdmDNSManager admDNSManager = (AdmDNSManager) rid.get();
        return admDNSManager;
    }

    private AdmDNSZone getDNSZone(AuthToken at, String zoneName) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        List<AdmDNSZone> dnsZones = admDNSManager.getDNSZones();
        for (AdmDNSZone zone : dnsZones) {
            if (zone.getName().equals(zoneName)) {
                return zone;
            }
        }
        throw new Exception("Failed to find dns zone [" + zoneName + "]");
    }

    private long getDNSZoneId(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        return zone.getId();
    }

    private long getDNSZoneInstantAliasId(AuthToken at, String zoneName, String prefix, int tag) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        List<AdmInstantAlias> aliases = zone.getAliases();
        for (AdmInstantAlias alias : aliases) {
            if (alias.getPrefix().equals(prefix) && alias.getTag() == tag) {
                return alias.getId();
            }
        }
        throw new Exception("Failed to find instant aliases with prefix [" + prefix + "]");
    }

    private long getDNSZoneResellerCPAliasId(AuthToken at, String zoneName, String prefix) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        List<ResellerCpAlias> aliases = zone.getCpAliases();
        for (ResellerCpAlias alias : aliases) {
            if (alias.getPrefix().equals(prefix)) {
                return alias.getId();
            }
        }
        throw new Exception("Failed to find reseller cp aliases with prefix [" + prefix + "]");
    }

    private CustomDNSRecord getCustomDNSRecord(AuthToken at, String fullRecordname) throws Exception {
        Account acc = Utils.getAccount(at);
        Collection<ResourceId> customRecords = acc.getId().findChildren("cust_dns_record");
        for (ResourceId customRecordId : customRecords) {
            CustomDNSRecord customRecord = (CustomDNSRecord) customRecordId.get();
            if (fullRecordname.equals(customRecord.getName())) {
                return customRecord;
            }
        }
        throw new Exception("Failed to find DNS record [" + fullRecordname + "]");
    }

    private DNSZone getUserDNSZone(AuthToken at, String zoneName) throws Exception {
        Account acc = Utils.getAccount(at);
        Collection<ResourceId> dnsZones = acc.getId().findAllChildren("dns_zone");
        for (ResourceId dnsZoneId : dnsZones) {
            DNSZone dnsZone = (DNSZone) dnsZoneId.get();
            if (zoneName.equals(dnsZone.getName())) {
                return dnsZone;
            }
        }
        throw new Exception("Failed to find custom DNS zone [" + zoneName + "]");
    }

    private Object[] getAliasesFromCollection(Collection aliases) throws Exception {
        int size = aliases.size();
        Object[] recordsInfo = new Object[size];
        int index = 0;
        Iterator iter = aliases.iterator();
        while (iter.hasNext()) {
            AdmInstantAlias alias = (AdmInstantAlias) iter.next();
            recordsInfo[index] = alias.get("prefix").toString();
            index++;
        }
        return recordsInfo;
    }

    private DNSRecordInfo[] getRecordsFromCollection(Collection records) throws Exception {
        int size = records.size();
        DNSRecordInfo[] recordsInfo = new DNSRecordInfo[size];
        int index = 0;
        Iterator iter = records.iterator();
        while (iter.hasNext()) {
            DNSRecord record = (DNSRecord) ((ResourceId) iter.next()).get();
            recordsInfo[index] = getDNSRecordInfo(record);
            index++;
        }
        return recordsInfo;
    }

    private DNSRecordInfo getDNSRecordInfo(DNSRecord record) throws Exception {
        DNSRecordInfo recordInfo = new DNSRecordInfo(record.get("id").toString(), record.get("name").toString(), record.get("ttl").toString(), "IN", record.get("type").toString(), record.get("data").toString(), record.get("pref").toString());
        return recordInfo;
    }

    private long getCustomDNSRecordId(AuthToken at, String zoneName, String prefix, String type) throws Exception {
        String name;
        AdmDNSZone zone = getDNSZone(at, zoneName);
        Collection<AdmCustomDNSRecord> records = zone.getCustomRecords();
        for (AdmCustomDNSRecord record : records) {
            if (prefix != null && prefix.length() > 0) {
                name = prefix + "." + zoneName;
            } else {
                name = zoneName;
            }
            if (record.get("name").toString().equals(name) && record.get("type").toString().equals(type)) {
                return Long.parseLong(record.get("id").toString());
            }
        }
        throw new Exception("Failed to find dns record [" + prefix + "." + zoneName + "] (type [" + type + "])");
    }

    private void addUserCustomRecord(AuthToken at, String type, String zoneName, String prefix, String priority, String ttl, String ip) throws Exception {
        try {
            DNSZone zone = getUserDNSZone(at, zoneName);
            String fullname = prefix + "." + zoneName;
            Collection initvals = type == "MX" ? Arrays.asList(fullname, type, priority, ip, ttl) : Arrays.asList(fullname, type, ttl, ip);
            zone.addChild("cust_dns_record", "", initvals);
        } catch (Exception e) {
            throw new Exception(e.getCause() + e.toString());
        }
    }

    public void addDNSZone(AuthToken at, String zoneName, String admEmail, boolean allowThirdDomainLevelHosting, long masterServer) throws Exception {
        String allow;
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        if (allowThirdDomainLevelHosting) {
            allow = "1";
        } else {
            allow = "0";
        }
        admDNSManager.FM_addDNSZone(zoneName, admEmail, allow, masterServer, 0L, 0L);
    }

    public void delDNSZone(AuthToken at, String zoneName) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_deleteDNSZone(zoneId);
    }

    public boolean isAllowHosting(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        return zone.allowHosting();
    }

    public void setAllowHosting(AuthToken at, String zoneName, boolean allowThirdDomainLevelHosting) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        zone.setAllowHosting(allowThirdDomainLevelHosting);
    }

    public Object[] getAllZones(AuthToken at) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        List<AdmDNSZone> dnsZones = admDNSManager.getDNSZones();
        Object[] zones = new Object[dnsZones.size()];
        int index = 0;
        for (AdmDNSZone zone : dnsZones) {
            zones[index] = zone.getName();
            index++;
        }
        return zones;
    }

    public String getAdminEmailForZone(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        return zone.getEmail();
    }

    public void addARecord(AuthToken at, String zoneName, String prefix, String ip, String ttl) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_addCustomDNSRecord(zoneId, ip, prefix, "A", ttl, "");
    }

    public void addMXRecord(AuthToken at, String zoneName, String prefix, String ip, String priority) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_addCustomDNSRecord(zoneId, ip, prefix, "MX", "", priority);
    }

    public void addCNAMERecord(AuthToken at, String zoneName, String prefix, String ip, String ttl) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_addCustomDNSRecord(zoneId, ip, prefix, "CNAME", ttl, "");
    }

    public void delCustomDNSRecord(AuthToken at, String zoneName, String prefix, String type) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        long recordId = getCustomDNSRecordId(at, zoneName, prefix, type);
        admDNSManager.FM_delCustomDNSRecord(zoneId, recordId);
    }

    public void addInstantAlias(AuthToken at, String zoneName, String prefix, int tag) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_addAlias(zoneId, prefix, tag);
    }

    public void delInstantAlias(AuthToken at, String zoneName, String prefix, int tag) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        AdmDNSZone zone = getDNSZone(at, zoneName);
        long zoneId = zone.getId();
        long aliasId = getDNSZoneInstantAliasId(at, zoneName, prefix, tag);
        admDNSManager.FM_delAlias(zoneId, aliasId);
    }

    public void addResellerCPAlias(AuthToken at, String zoneName, String prefix) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        long zoneId = getDNSZoneId(at, zoneName);
        admDNSManager.FM_addCpAlias(zoneId, prefix);
    }

    public void delResellerCPAlias(AuthToken at, String zoneName, String prefix) throws Exception {
        AdmDNSManager admDNSManager = getAdmDNSmanager(at);
        AdmDNSZone zone = getDNSZone(at, zoneName);
        long zoneId = zone.getId();
        long aliasId = getDNSZoneResellerCPAliasId(at, zoneName, prefix);
        admDNSManager.FM_delCpAlias(zoneId, aliasId);
    }

    public DNSRecordInfo[] getAllCustomDNSRecords(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        Collection records = zone.getCustomRecords();
        return getRecordsFromCollection(records);
    }

    public Object[] getAllInstantAliases(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        Collection records = zone.getAliases();
        return getAliasesFromCollection(records);
    }

    public Object[] getAllResellerCPAliases(AuthToken at, String zoneName) throws Exception {
        AdmDNSZone zone = getDNSZone(at, zoneName);
        Collection records = zone.getCpAliases();
        return getAliasesFromCollection(records);
    }

    public DNSRecordInfo[] getBuiltinARecords(AuthToken at, String domainName) throws Exception {
        Collection a_records;
        Account a = Utils.getAccount(at);
        Domain domain = Utils.getDomain(a, domainName);
        String domainType = domain.getResourceType().get("type").toString();
        if (domainType.equals("domain_alias") || domainType.equals("3ldomain_alias")) {
            DNSZone zone = (DNSZone) domain.getId().findChild("dns_zone").get();
            a_records = zone.getId().findAllChildren("domain_alias_a_record");
        } else {
            UnixMixedIPResource ip = (UnixMixedIPResource) Utils.getDomain(a, domainName).getId().findChild("ip").get();
            a_records = ip.getId().findAllChildren("a_record");
        }
        return getRecordsFromCollection(a_records);
    }

    public DNSRecordInfo[] getBuiltinMXRecords(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        Domain domain = Utils.getDomain(a, domainName);
        MailService mail = (MailService) domain.getId().findChild("mail_service").get();
        Collection mx_records = mail.getId().findAllChildren("mx");
        return getRecordsFromCollection(mx_records);
    }

    public DNSRecordInfo[] getBuiltinCNAMERecords(AuthToken at, String domainName) throws Exception {
        Account a = Utils.getAccount(at);
        Domain domain = Utils.getDomain(a, domainName);
        MailService mail = (MailService) domain.getId().findChild("mail_service").get();
        Collection cname_records = mail.getId().findAllChildren("cname_record");
        return getRecordsFromCollection(cname_records);
    }

    public Object[] getAllUserZones(AuthToken at) throws Exception {
        Account a = Utils.getAccount(at);
        Collection dnsZones = a.getId().findAllChildren("dns_zone");
        Collection dns3lZones = a.getId().findAllChildren("3l_dns_zone");
        ArrayList allZones = new ArrayList();
        allZones.addAll(dnsZones);
        allZones.addAll(dns3lZones);
        Object[] zones = new Object[allZones.size()];
        int index = 0;
        Iterator i = allZones.iterator();
        while (i.hasNext()) {
            ResourceId zoneRid = (ResourceId) i.next();
            DNSZone zone = (DNSZone) zoneRid.get();
            zones[index] = zone.getName();
            index++;
        }
        return zones;
    }

    public void addUserCustomARecord(AuthToken at, String zoneName, String prefix, String ttl, String ip) throws Exception {
        addUserCustomRecord(at, "A", zoneName, prefix, null, ttl, ip);
    }

    public void addUserCustomMXRecord(AuthToken at, String zoneName, String prefix, String priority, String ttl, String ip) throws Exception {
        addUserCustomRecord(at, "MX", zoneName, prefix, priority, ttl, ip);
    }

    public void addUserCustomCNAMERecord(AuthToken at, String zoneName, String prefix, String type, String ttl, String ip) throws Exception {
        addUserCustomRecord(at, "CNAME", zoneName, prefix, null, ttl, ip);
    }

    public void delUserCustomRecord(AuthToken at, String fullRecordname) throws Exception {
        CustomDNSRecord customRecord = getCustomDNSRecord(at, fullRecordname);
        customRecord.FM_cdelete(0);
        log.info("DNSServices: Deleted custom dns record [" + fullRecordname + "]");
    }

    public DNSRecordInfo[] getAllUserCustomDNSRecords(AuthToken at, String zoneName) throws Exception {
        DNSZone zone = getUserDNSZone(at, zoneName);
        Collection records = zone.getId().findChildren("cust_dns_record");
        return getRecordsFromCollection(records);
    }
}
