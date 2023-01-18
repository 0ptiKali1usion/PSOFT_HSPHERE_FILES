package psoft.hsphere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/* loaded from: hsphere.zip:psoft/hsphere/TypeRegistry.class */
public class TypeRegistry {
    protected static Map id2type;
    protected static Map type2id;
    protected static List typeIds;
    protected static List pricedTypes;
    protected static List types;
    protected static Set monthlyBilledIds;
    protected static Set nonRefundableIds;
    protected static Set changebleIds;
    protected static Set domainRegistrationIds;
    protected static Set monthlyDummyResourceIds;
    protected static final String[] MOTHLY_BILLED_RESOURCES = {"traffic", "reseller_traffic", "ftp_user_traffic", "mail_traffic", "http_traffic", "ftp_traffic", "real_server_traffic", "mysqldb_quota", "pgsqldb_quota", "real_user", "summary_quota", "backup", "reseller_backup", "ds_bandwidth", "r_ds_bandwidth", "vps_traffic"};
    protected static final String[] NON_REFUNDABLE_RESOURCES = {"traffic", "reseller_traffic", "ftp_user_traffic", "mail_traffic", "http_traffic", "ftp_traffic", "real_server_traffic", "mysqldb_quota", "pgsqldb_quota", "real_user", "summary_quota", "ds_traffic"};
    protected static final String[] CHANGEBLE_RESOURCES = {"traffic", "ftp_user_traffic", "ftp_traffic", "real_server_traffic", "mysqldb_quota", "pgsqldb_quota", "real_user", "summary_quota", "reseller_traffic", "mail_traffic", "http_traffic", "quota", "mail_quota", "winquota", "real_user", "backup", "reseller_backup", "ds_ip_range", "ds_bandwidth", "r_ds_bandwidth", "vps_traffic"};
    protected static final String[] DOMAIN_REGISTRATION_RESOURCES = {"opensrs", "domain_transfer"};
    protected static final String[] MOTHLY_DUMMY_RESOURCES = {"mail_traffic", "http_traffic", "ftp_user_traffic", "ftp_traffic", "real_server_traffic", "vps_traffic"};

    static {
        init();
    }

    public static void reload() {
        init();
        Session.getLog().debug("TypeRegistry reloaded");
    }

    protected static synchronized void init() {
        id2type = new HashMap();
        type2id = new HashMap();
        typeIds = new ArrayList();
        pricedTypes = new ArrayList();
        types = new ArrayList();
        monthlyBilledIds = new HashSet();
        nonRefundableIds = new HashSet();
        changebleIds = new HashSet();
        domainRegistrationIds = new HashSet();
        monthlyDummyResourceIds = new HashSet();
        List montlyBilled = Arrays.asList(MOTHLY_BILLED_RESOURCES);
        List nonRefundable = Arrays.asList(NON_REFUNDABLE_RESOURCES);
        List changeble = Arrays.asList(CHANGEBLE_RESOURCES);
        List registration = Arrays.asList(DOMAIN_REGISTRATION_RESOURCES);
        List mothlyDummy = Arrays.asList(MOTHLY_DUMMY_RESOURCES);
        try {
            Connection con = Session.getDb();
            PreparedStatement ps = con.prepareStatement("SELECT id, name, description, price, rprice, required,  priority, ttl FROM type_name ORDER BY id");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                id2type.put(rs.getString(1), new TypeInfo(rs.getString(2), rs.getString(3), rs.getString(4), rs.getString(5), rs.getInt(6) == 1, rs.getInt(7), rs.getLong(8)));
                type2id.put(rs.getString(2), rs.getString(1));
                if (rs.getString(4) != null && rs.getString(4).length() > 0) {
                    pricedTypes.add(rs.getString(1));
                }
                typeIds.add(rs.getString(1));
                types.add(rs.getString(2));
                if (montlyBilled.contains(rs.getString(2))) {
                    monthlyBilledIds.add(new Integer(rs.getInt(1)));
                    Session.getLog().debug("Resource Type is monthly:" + rs.getString(2));
                }
                if (nonRefundable.contains(rs.getString(2))) {
                    nonRefundableIds.add(new Integer(rs.getInt(1)));
                    Session.getLog().debug("Resource Type is non-refundable:" + rs.getString(2));
                }
                if (changeble.contains(rs.getString(2))) {
                    changebleIds.add(new Integer(rs.getInt(1)));
                    Session.getLog().debug("Resource Type is changeble:" + rs.getString(2));
                }
                if (registration.contains(rs.getString(2))) {
                    domainRegistrationIds.add(new Integer(rs.getInt(1)));
                }
                if (mothlyDummy.contains(rs.getString(2))) {
                    monthlyDummyResourceIds.add(new Integer(rs.getInt(1)));
                }
            }
            Session.closeStatement(ps);
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
            throw new Error("Unable to load type_id type_name information");
        }
    }

    public static String getTypeId(String name) throws NoSuchTypeException {
        try {
            String result = (String) type2id.get(name);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
        }
        throw new NoSuchTypeException("No Such Type: " + name);
    }

    public static int getIntTypeId(String name) throws NoSuchTypeException {
        try {
            Object id = type2id.get(name);
            if (id != null) {
                return Integer.parseInt((String) id);
            }
        } catch (Exception e) {
            Session.getLog().error("Unable to get an id of the '" + name + "' resource type.", e);
        }
        throw new NoSuchTypeException("No such type '" + name + "'");
    }

    public static boolean isResourceName(String name) {
        return type2id.containsKey(name);
    }

    public static String getDescription(String typeId) throws NoSuchTypeException {
        return getTypeInfo(typeId).getDescription();
    }

    public static String getDescription(int typeId) throws NoSuchTypeException {
        return getDescription(Integer.toString(typeId));
    }

    public static String getType(String typeId) throws NoSuchTypeException {
        return getTypeInfo(typeId).getName();
    }

    public static String getType(int typeId) throws NoSuchTypeException {
        return getType(Integer.toString(typeId));
    }

    public static String getPriceType(String typeId) throws NoSuchTypeException {
        return getTypeInfo(typeId).getPriceType();
    }

    public static String getPriceType(int typeId) throws NoSuchTypeException {
        return getPriceType(Integer.toString(typeId));
    }

    public static String getRPriceType(String typeId) throws NoSuchTypeException {
        return getTypeInfo(typeId).getRPriceType();
    }

    public static boolean isRequired(int typeId) throws NoSuchTypeException {
        return isRequired(Integer.toString(typeId));
    }

    public static boolean isRequired(String typeId) throws NoSuchTypeException {
        return getTypeInfo(typeId).isRequired();
    }

    public static int getPriority(int typeId) {
        return getPriority(Integer.toString(typeId));
    }

    public static int getPriority(String typeId) {
        try {
            return getTypeInfo(typeId).getPriority();
        } catch (Exception e) {
            return 0;
        }
    }

    public static long getTTL(String typeId) {
        try {
            return getTypeInfo(typeId).getTTL();
        } catch (Exception e) {
            return 0L;
        }
    }

    protected static TypeInfo getTypeInfo(String typeId) throws NoSuchTypeException {
        try {
            TypeInfo result = (TypeInfo) id2type.get(typeId);
            if (result != null) {
                return result;
            }
        } catch (Exception e) {
        }
        throw new NoSuchTypeException("No Such Type Id: " + typeId);
    }

    public static String getRPriceType(int typeId) throws NoSuchTypeException {
        return getRPriceType(Integer.toString(typeId));
    }

    public static List getTypeIds() {
        return typeIds;
    }

    public static List getTypes() {
        return types;
    }

    public static List getPricedTypes() {
        Session.getLog().debug("INSIDE TypeRegistry.getPricedTypes()");
        return pricedTypes;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/TypeRegistry$TypeInfo.class */
    public static class TypeInfo {
        protected String name;
        protected String desc;
        protected String price;
        protected String rprice;
        protected boolean required;
        protected int priority;
        protected long ttl;

        TypeInfo(String name, String desc, String price, String rprice, boolean required, int priority, long ttl) {
            this.name = name;
            this.desc = desc;
            this.price = price;
            this.rprice = rprice;
            this.required = required;
            this.priority = priority;
            this.ttl = ttl;
        }

        public String getName() {
            return this.name;
        }

        public String getDescription() {
            return this.desc;
        }

        public String getPriceType() {
            return this.price == null ? "" : this.price;
        }

        public String getRPriceType() {
            return this.rprice == null ? "" : this.rprice;
        }

        public boolean isRequired() {
            return this.required;
        }

        public int getPriority() {
            return this.priority;
        }

        public long getTTL() {
            return this.ttl;
        }
    }

    public static boolean isMonthly(int type) {
        if (monthlyBilledIds.contains(new Integer(type))) {
            return true;
        }
        return false;
    }

    public static boolean isNonRefundable(int type) {
        return nonRefundableIds.contains(new Integer(type));
    }

    public static boolean isChangeable(int type) {
        if (changebleIds.contains(new Integer(type))) {
            return true;
        }
        return false;
    }

    public static boolean isDummy(int type) {
        if (monthlyDummyResourceIds.contains(new Integer(type))) {
            return true;
        }
        return false;
    }

    public static Set getMonthlyBilledIds() {
        return Collections.unmodifiableSet(monthlyBilledIds);
    }

    public static Set getDomainRegistrationIds() {
        return Collections.unmodifiableSet(domainRegistrationIds);
    }

    public static Set getMonthlyDummyIds() {
        return Collections.unmodifiableSet(monthlyDummyResourceIds);
    }
}
