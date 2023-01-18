package psoft.hsphere;

import freemarker.template.Template;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import freemarker.template.TemplateModelRoot;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import psoft.epayment.ISOCodes;
import psoft.hsphere.admin.LanguageManager;
import psoft.hsphere.admin.Settings;
import psoft.hsphere.calc.Calc;
import psoft.hsphere.global.Globals;
import psoft.hsphere.manager.Entity;
import psoft.hsphere.p001ds.DSHolder;
import psoft.hsphere.p001ds.DedicatedServerTemplate;
import psoft.hsphere.payment.ExternalPayServlet;
import psoft.hsphere.plan.ResourceType;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.admin.AdmDNSZone;
import psoft.hsphere.resource.admin.MerchantManager;
import psoft.hsphere.resource.epayment.BillingInfoObject;
import psoft.hsphere.resource.epayment.GenericCreditCard;
import psoft.hsphere.resource.registrar.LoggableRegistrar;
import psoft.hsphere.resource.registrar.Registrar;
import psoft.hsphere.resource.registrar.RegistrarException;
import psoft.hsphere.resource.ssl.SSLTools;
import psoft.hsphere.resource.system.MailServices;
import psoft.hsphere.util.XMLManager;
import psoft.util.Config;
import psoft.util.LocalExec;
import psoft.util.TimeUtils;
import psoft.util.USFormat;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateMap;
import psoft.util.freemarker.TemplatePair;
import psoft.util.freemarker.TemplateString;
import psoft.util.freemarker.Toolbox;
import psoft.web.utils.HTMLEncoder;
import psoft.web.utils.URLEncoder;

/* loaded from: hsphere.zip:psoft/hsphere/HsphereToolbox.class */
public class HsphereToolbox extends Toolbox {
    public static final TemplateModel toolbox = new HsphereToolbox();
    public static final TemplateModel DBLookup = new DBDomainLookup();
    public static final TemplateModel smartTraffic = new SmartTraffic();
    public static final TemplateModel currency = new HSCurrency();
    private static Hashtable typeLists = new Hashtable();
    public static final TemplateModel signupGuardParams = new SignupGuardParams();
    private static boolean cpSSLEnabled;
    public static final DateFormat defaultDateFormat;
    static final String specialChars = "<>&\"\n";
    static final String[] htmlSymbols;

    static {
        String cpProt = Config.getProperty("CLIENT", "CP_PROTOCOL");
        cpSSLEnabled = cpProt != null && cpProt.startsWith("https");
        defaultDateFormat = new SimpleDateFormat("MM/dd/yyyy");
        htmlSymbols = new String[]{"&lt;", "&gt;", "&amp;", "&quot;", "<br>"};
    }

    /* loaded from: hsphere.zip:psoft/hsphere/HsphereToolbox$DBDomainLookup.class */
    static class DBDomainLookup implements TemplateMethodModel {
        DBDomainLookup() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) {
            String domainName = HTMLEncoder.decode((String) list.get(0));
            return HsphereToolbox.databaseLookup(domainName) ? new TemplateString("1") : new TemplateString("0");
        }
    }

    public static boolean databaseLookup(String domainName) {
        if (domainName == null) {
            return true;
        }
        Connection con = null;
        PreparedStatement ps = null;
        try {
            try {
                con = Session.getDb();
                ps = con.prepareStatement("SELECT id FROM domains WHERE name = ?");
                ps.setString(1, domainName.toLowerCase());
                ResultSet rs = ps.executeQuery();
                boolean next = rs.next();
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se) {
                        Session.getLog().error("Error closing connection", se);
                    }
                }
                return next;
            } catch (SQLException se2) {
                Session.getLog().error("Error checking domain name", se2);
                Session.closeStatement(ps);
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException se3) {
                        Session.getLog().error("Error closing connection", se3);
                        return true;
                    }
                }
                return true;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException se4) {
                    Session.getLog().error("Error closing connection", se4);
                    throw th;
                }
            }
            throw th;
        }
    }

    public TemplateModel FM_generateSSL(String country, String state, String locality, String organization, String organ_unit, String site, String email) throws Exception {
        return SSLTools.generateSSL(country, state, locality, organization, organ_unit, site, email);
    }

    public static boolean isCPSSLEnabled() {
        return cpSSLEnabled;
    }

    public static String stripPhone(String str) {
        StringBuffer buf = new StringBuffer();
        if (str == null) {
            return "";
        }
        for (int i = 0; i < str.length(); i++) {
            if (Character.isDigit(str.charAt(i))) {
                buf.append(str.charAt(i));
            }
        }
        String phone = buf.toString();
        if (phone.length() > 2 && phone.charAt(0) == '1') {
            return phone.substring(1);
        }
        return phone;
    }

    public TemplateModel FM_getPhone(String str) {
        String phone = stripPhone(str);
        String areaCode = "";
        String phoneNum = "";
        if (phone.length() <= 3 && phone.length() > 0) {
            phoneNum = phone;
        } else if (phone.length() > 3) {
            areaCode = phone.substring(0, 3);
            phoneNum = phone.substring(3);
        }
        return new TemplatePair(areaCode, phoneNum);
    }

    public static TemplateModel FM_dismissReq() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("DELETE FROM shell_req WHERE id = ?");
            ps.setLong(1, Session.getAccountId());
            ps.executeUpdate();
            TemplateOKResult templateOKResult = new TemplateOKResult();
            Session.closeStatement(ps);
            con.close();
            return templateOKResult;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static TemplateModel FM_traceRoute(String host, String timeout) throws Exception {
        Collection<Object> lines = LocalExec.exec(new String[]{"/hsphere/shared/scripts/traceroutepsoft", MailServices.shellQuote(host), timeout}, null);
        TemplateList trace = new TemplateList();
        for (Object obj : lines) {
            trace.add((TemplateModel) new TemplateString(obj));
        }
        return trace;
    }

    public static TemplateModel FM_getClientLocation() throws Exception {
        return new TemplateString(Session.getRequest().getRemoteHost());
    }

    public TemplateModel FM_getInvoice(String modId) {
        return Invoice.getInvoice(modId);
    }

    @Override // psoft.util.freemarker.Toolbox
    public TemplateModel get(String key) {
        if (key.equals("currency")) {
            return currency;
        }
        if (key.equals("DBlookup")) {
            return DBLookup;
        }
        if (key.equals("smartTraffic")) {
            return smartTraffic;
        }
        if (key.equals("now")) {
            return new TemplateString(TimeUtils.getDate());
        }
        if (key.equals("SignupGuardParams")) {
            return signupGuardParams;
        }
        if (key.equals("random")) {
            try {
                return new TemplateString(String.valueOf(Session.getNewIdAsLong()));
            } catch (Exception e) {
                return null;
            }
        }
        try {
            if (key.equals("hosted_zones")) {
                return new ListAdapter(AdmDNSZone.getHostedZones());
            }
            if (key.equals("free_zones")) {
                return new ListAdapter(AdmDNSZone.getFreeZones());
            }
            if (key.equals("ssl_zones")) {
                return new ListAdapter(AdmDNSZone.getSSLZones());
            }
            if (key.equals("multiplier")) {
                try {
                    return new TemplateString(Calc.getMultiplier());
                } catch (Exception e2) {
                    return new TemplateString("1");
                }
            } else if (key.equals("taxes")) {
                try {
                    return new TemplateHash(Session.getReseller().getTaxes());
                } catch (Exception e3) {
                    return null;
                }
            } else if (key.equals("lang_list")) {
                return new TemplateList(LanguageManager.getLanguageList());
            } else {
                if (key.equals("current_lang")) {
                    return new TemplateString(Session.getCurrentLocale().getLanguage());
                }
                if (key.equals("current_country")) {
                    return new TemplateString(Session.getCurrentLocale().getCountry());
                }
                if (key.equals("cpSSLEnabled")) {
                    return new TemplateString(cpSSLEnabled ? "1" : "");
                } else if (key.equals("reseller_id")) {
                    try {
                        return new TemplateString(Session.getResellerId());
                    } catch (Exception e4) {
                        return null;
                    }
                } else if (key.equals("new_estimate")) {
                    return new Estimator();
                } else {
                    TemplateModel res = super.get(key);
                    if (res != null) {
                        return res;
                    }
                    return AccessTemplateMethodWrapper.getMethod(this, key);
                }
            }
        } catch (Exception e5) {
            return null;
        }
    }

    public TemplateModel FM_getSSLZoneByTag(int tag) throws Exception {
        for (AdmDNSZone zone : AdmDNSZone.getSSLZones()) {
            if (zone.getIpTag() == tag) {
                return zone;
            }
        }
        return null;
    }

    public TemplateModel FM_getAliases() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT prefix, dns_zones.name, tag FROM e_ialiases, dns_zones, e_zones WHERE reseller_id = ? AND e_zones.id = zone_id AND zone_id = dns_zones.id");
                ps.setLong(1, Session.getResellerId());
                ResultSet rs = ps.executeQuery();
                TemplateList lst = new TemplateList();
                while (rs.next()) {
                    TemplateHash t = new TemplateHash();
                    t.put("prefix", rs.getString(1));
                    t.put("zone", rs.getString(2));
                    t.put("tag", rs.getString(3));
                    lst.add((TemplateModel) t);
                }
                Session.closeStatement(ps);
                con.close();
                return lst;
            } catch (Exception e) {
                Session.getLog().error("Unable to get aliases", e);
                Session.closeStatement(ps);
                con.close();
                return null;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getCpAliases() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT a.prefix, b.name, a.tag FROM e_ialiases a, dns_zones b, e_zones c WHERE a.zone_id = b.id AND a.zone_id = c.id AND c.reseller_id = ? AND a.tag = ?");
                ps.setLong(1, Session.getResellerId());
                ps.setInt(2, 4);
                ResultSet rs = ps.executeQuery();
                TemplateList lst = new TemplateList();
                while (rs.next()) {
                    TemplateHash t = new TemplateHash();
                    t.put("prefix", rs.getString(1));
                    t.put("zone", rs.getString(2));
                    lst.add((TemplateModel) t);
                }
                Session.closeStatement(ps);
                con.close();
                return lst;
            } catch (Exception e) {
                Session.getLog().error("Unable to get reseller cp aliases", e);
                Session.closeStatement(ps);
                con.close();
                return null;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getSharedIPTags() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT DISTINCT flag FROM l_server_ips WHERE flag = 2 OR (flag >= 10 AND flag != ? AND flag != ?) ORDER BY flag");
                ps.setInt(1, HostEntry.VPS_IP);
                ps.setInt(2, HostEntry.TAKEN_VPS_IP);
                ResultSet rs = ps.executeQuery();
                TemplateList lst = new TemplateList();
                while (rs.next()) {
                    lst.add(rs.getString(1));
                }
                Session.closeStatement(ps);
                con.close();
                return lst;
            } catch (Exception e) {
                Session.getLog().error("Unable to get shared IP tags", e);
                Session.closeStatement(ps);
                con.close();
                return null;
            }
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getResourceDescription(String typeId) throws Exception {
        return new TemplateString(TypeRegistry.getDescription(typeId));
    }

    public TemplateModel FM_getResourcePriceType(String typeId) throws Exception {
        return new TemplateString(TypeRegistry.getPriceType(typeId));
    }

    public TemplateModel FM_getResellerResourcePriceType(String typeId) throws Exception {
        return new TemplateString(TypeRegistry.getRPriceType(typeId));
    }

    public TemplateModel FM_getTypeName(String typeId) throws Exception {
        return new TemplateString(TypeRegistry.getType(typeId));
    }

    public TemplateModel FM_getTypeId(String name) throws Exception {
        return new TemplateString(TypeRegistry.getTypeId(name));
    }

    public TemplateModel FM_isMonthlyResource(String name) throws Exception {
        try {
            if (TypeRegistry.isMonthly(TypeRegistry.getIntTypeId(name))) {
                return new TemplateString("1");
            }
            return null;
        } catch (NoSuchTypeException e) {
            return null;
        }
    }

    public TemplateModel FM_getTypeIdByName(String name) throws Exception {
        return new TemplateString(TypeRegistry.getTypeId(name));
    }

    public TemplateModel FM_getCurrentDate() throws Exception {
        return new TemplateString(DateFormat.getDateInstance(2).format(TimeUtils.getDate()));
    }

    public static DecimalFormat getLocalizedDecimalFormat() throws Exception {
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance();
        DecimalFormatSymbols ds = df.getDecimalFormatSymbols();
        String groupSeparator = Settings.get().getValue("groupsep");
        String decSeparator = Settings.get().getValue("decsep");
        char gSep = 0;
        char dSep = 0;
        if (groupSeparator != null) {
            gSep = groupSeparator.charAt(0);
        }
        if (decSeparator != null) {
            dSep = decSeparator.charAt(0);
        }
        if (gSep != 65535 && gSep != 0) {
            ds.setGroupingSeparator(gSep);
        }
        if (dSep != 65535 && dSep != 0) {
            ds.setDecimalSeparator(dSep);
        }
        if (dSep != 65535 && dSep != 0) {
            ds.setMonetaryDecimalSeparator(dSep);
        }
        df.setDecimalFormatSymbols(ds);
        return df;
    }

    public TemplateModel FM_numberToUSLocale(String str) throws Exception {
        try {
            DecimalFormat df = getLocalizedDecimalFormat();
            double tmpValue = df.parse(str).doubleValue();
            return new TemplateString(USFormat.format(tmpValue));
        } catch (Exception e) {
            return null;
        }
    }

    public TemplateModel FM_numberToCurLocale(String str, String useGrouping) throws Exception {
        try {
            double tmpValue = USFormat.parseDouble(str);
            DecimalFormat df = getLocalizedDecimalFormat();
            String tmp = df.format(tmpValue);
            return new TemplateString(tmp);
        } catch (Exception e) {
            return null;
        }
    }

    public TemplateModel FM_getCurrencySymbol() throws Exception {
        String result;
        String cursym = Settings.get().getValue("cursym");
        if (cursym != null) {
            result = cursym;
        } else {
            String locale = Settings.get().getValue(AccountPreferences.LANGUAGE);
            if (locale != null) {
                String[] tmpLocale = Localizer.getArrayLocale(locale);
                result = new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getCurrencySymbol();
            } else {
                result = new DecimalFormatSymbols().getCurrencySymbol();
            }
        }
        return new TemplateString(result);
    }

    public TemplateModel FM_getInternationalCurrrencySymbol() throws Exception {
        String result;
        String adm_curcode = Settings.get().getValue("curcode");
        if (adm_curcode != null) {
            return new TemplateString(adm_curcode);
        }
        String locale = Settings.get().getValue(AccountPreferences.LANGUAGE);
        if (locale != null) {
            String[] tmpLocale = Localizer.getArrayLocale(locale);
            result = new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1])).getInternationalCurrencySymbol();
        } else {
            result = new DecimalFormatSymbols().getInternationalCurrencySymbol();
        }
        return new TemplateString(result);
    }

    public TemplateModel FM_getLocalizedCurSym(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getCurrencySymbol());
    }

    public TemplateModel FM_getLocalizedInternationalCurSym(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getInternationalCurrencySymbol());
    }

    public TemplateModel FM_getLocalizedCurDecSeparator(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getMonetaryDecimalSeparator());
    }

    public TemplateModel FM_getLocalizedDecSeparator(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getDecimalSeparator());
    }

    public TemplateModel FM_getLocalizedDigit(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getDigit());
    }

    public TemplateModel FM_getLocalizedGroupingSeparator(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getGroupingSeparator());
    }

    public TemplateModel FM_getLocalizedMinusSign(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getMinusSign());
    }

    public TemplateModel FM_getLocalizedIntCurrency(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getInternationalCurrencySymbol());
    }

    public TemplateModel FM_getLocalizedNaN(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getNaN());
    }

    public TemplateModel FM_getLocalizedPercent(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getPercent());
    }

    public TemplateModel FM_getLocalizedPerMill(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getPerMill());
    }

    public TemplateModel FM_getLocalizedZero(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(new DecimalFormatSymbols(new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2])).getZeroDigit());
    }

    public TemplateModel FM_getLocalizedShortDate(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(((SimpleDateFormat) DateFormat.getDateInstance(3, new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2]))).toPattern());
    }

    public TemplateModel FM_getLocalizedMediumDate(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(((SimpleDateFormat) DateFormat.getDateInstance(2, new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2]))).toPattern());
    }

    public TemplateModel FM_getLocalizedLongDate(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(((SimpleDateFormat) DateFormat.getDateInstance(1, new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2]))).toPattern());
    }

    public TemplateModel FM_getLocalizedFullDate(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        return new TemplateString(((SimpleDateFormat) DateFormat.getDateInstance(0, new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2]))).toPattern());
    }

    public TemplateModel FM_getLocalizedPattern(String locale) throws Exception {
        String[] tmpLocale = Localizer.getArrayLocale(locale);
        Locale loc = new Locale(tmpLocale[0], tmpLocale[1], tmpLocale[2]);
        DecimalFormat tmpFormat = (DecimalFormat) NumberFormat.getCurrencyInstance(loc);
        return new TemplateString(tmpFormat.toPattern());
    }

    public static Date parseShortDate(String strDate) throws ParseException {
        DateFormat df = DateFormat.getDateInstance(3);
        try {
            return df.parse(strDate);
        } catch (ParseException e) {
            return defaultDateFormat.parse(strDate);
        }
    }

    public TemplateModel FM_getLocalTemplateName(String template_name, String template_ext) {
        String locale = "_" + Session.getCurrentLocale().toString();
        String ext = template_ext != null ? template_ext : "";
        String result = "";
        if (template_name != null && !"".equals(template_name)) {
            while (true) {
                String curTemplate = template_name + locale + ext;
                if (Session.getTemplate(curTemplate) != null) {
                    result = curTemplate;
                    break;
                } else if ("".equals(locale)) {
                    break;
                } else {
                    int index = locale.lastIndexOf(95);
                    if (index > 1) {
                        locale = locale.substring(0, index);
                    } else {
                        locale = "";
                    }
                }
            }
        }
        return new TemplateString(result);
    }

    public TemplateModel FM_includeText(String str) throws Exception {
        Template t = new Template(new ByteArrayInputStream(str.getBytes()));
        TemplateModelRoot root = Session.getModelRoot();
        StringWriter sw = new StringWriter();
        PrintWriter out = new PrintWriter(sw);
        t.process(root, out);
        out.flush();
        out.close();
        return new TemplateString(sw.toString());
    }

    public TemplateModel FM_isLServerAvailable(int groupType) {
        List servers = HostManager.getHostsByGroupType(groupType);
        for (int i = 0; i < servers.size(); i++) {
            HostEntry he = (HostEntry) servers.get(i);
            if (he.availableForSignup()) {
                return new TemplateString(true);
            }
        }
        return new TemplateString(false);
    }

    public TemplateModel FM_getHost(int hostId) throws Exception {
        HostEntry he = HostManager.getHost(hostId);
        return he;
    }

    /* loaded from: hsphere.zip:psoft/hsphere/HsphereToolbox$HSCurrency.class */
    static class HSCurrency implements TemplateMethodModel {
        HSCurrency() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            try {
                return new TemplateString(HsphereToolbox.translateCurrency(USFormat.parseDouble(HTMLEncoder.decode((String) l.get(0)))));
            } catch (Exception e) {
                Session.getLog().warn("Format currency error", e);
                return null;
            }
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/HsphereToolbox$SmartTraffic.class */
    static class SmartTraffic implements TemplateMethodModel {
        protected static String[] labels = {"KB", "MB", "GB", "TB"};

        SmartTraffic() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List list) {
            try {
                double kBytes = USFormat.parseDouble(HTMLEncoder.decode((String) list.get(0)));
                NumberFormat numberFormat = NumberFormat.getInstance();
                numberFormat.setMaximumFractionDigits(1);
                double numberUnits = kBytes;
                for (int i = 0; i < labels.length; i++) {
                    if (numberUnits < 1024.0d) {
                        return new TemplateString(numberFormat.format(numberUnits) + " " + labels[i]);
                    }
                    numberUnits /= 1024.0d;
                }
                return new TemplateString(numberFormat.format(kBytes));
            } catch (Exception e) {
                Session.getLog().warn("Invalid traffic param", e);
                return null;
            }
        }
    }

    public static String translateCurrency(double tmpValue) throws Exception {
        try {
            DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
            DecimalFormatSymbols ds = df.getDecimalFormatSymbols();
            String curcode = Settings.get().getValue("curcode");
            if (curcode != null && !"".equals(curcode)) {
                ds.setInternationalCurrencySymbol(curcode);
            }
            String cursym = Settings.get().getValue("cursym");
            if (cursym != null) {
                ds.setCurrencySymbol(cursym);
            }
            String groupSeparator = Settings.get().getValue("groupsep");
            String decSeparator = Settings.get().getValue("decsep");
            char gSep = 0;
            char dSep = 0;
            if (groupSeparator != null) {
                gSep = groupSeparator.charAt(0);
            }
            if (decSeparator != null) {
                dSep = decSeparator.charAt(0);
            }
            if (gSep != 65535 && gSep != 0) {
                ds.setGroupingSeparator(gSep);
            }
            if (dSep != 65535 && dSep != 0) {
                ds.setMonetaryDecimalSeparator(dSep);
            }
            df.setDecimalFormatSymbols(ds);
            String curformat = Settings.get().getValue("currformat");
            if (curformat != null) {
                try {
                    df.applyPattern(curformat);
                } catch (IllegalArgumentException e) {
                    try {
                        df.applyLocalizedPattern(curformat);
                    } catch (IllegalArgumentException ex1) {
                        Session.getLog().error("Exception in localized pattern", ex1);
                        throw new HSUserException("toolbox.incorrect_cur_format");
                    }
                }
            }
            String result = df.format(tmpValue);
            return result;
        } catch (IllegalArgumentException e2) {
            String.valueOf(tmpValue);
            throw new HSUserException("toolbox.incorrect_cur_format");
        }
    }

    public static double parseLocalizedNumber(String tmpValue) throws Exception {
        try {
            Session.getLog().debug("Localized value:" + tmpValue);
            DecimalFormat df = getLocalizedDecimalFormat();
            double result = df.parse(tmpValue).doubleValue();
            Session.getLog().debug("Localized US value:" + result);
            return result;
        } catch (IllegalArgumentException e) {
            throw new HSUserException("toolbox.incorrect_cur_format");
        }
    }

    public TemplateModel FM_displayBalance(String str) throws Exception {
        String key;
        if (str == null || "".equals(str)) {
            return new TemplateString("");
        }
        double balance = USFormat.parseDouble(str);
        if (balance >= 0.0d) {
            key = "bill.positive_balance";
        } else {
            key = "bill.negative_balance";
            balance = -balance;
        }
        return new TemplateString(Localizer.translateMessage(key, new Object[]{translateCurrency(balance)}));
    }

    public TemplateModel FM_getCurrencies() throws Exception {
        TemplateList l = new TemplateList();
        String[][] array = ISOCodes.currenciesArray;
        for (int i = 0; i < array.length; i++) {
            TemplateMap map = new TemplateMap();
            map.put("name", array[i][0]);
            map.put("short", array[i][1]);
            map.put("code", array[i][2]);
            l.add((TemplateModel) map);
        }
        return l;
    }

    public TemplateModel FM_getActiveCCbrands() throws Exception {
        TemplateList l = new TemplateList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT id,name_sh,name FROM cc_brands WHERE (id < 100 OR reseller_id = ?) AND name_sh IN (SELECT type FROM active_merch_gateway WHERE reseller_id = ?)");
            ps.setLong(1, Session.getResellerId());
            ps.setLong(2, Session.getResellerId());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TemplateMap map = new TemplateMap();
                map.put("id", rs.getString(1));
                map.put("name_sh", rs.getString(2).trim());
                map.put("name", rs.getString(3).trim());
                l.add((TemplateModel) map);
            }
            Session.closeStatement(ps);
            con.close();
            return l;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_isDeletedBrand(String name_sh) throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) from cc_brands WHERE (name_sh = ? AND id<100) OR (name_sh = ? AND reseller_id = ?)");
            ps2.setString(1, name_sh);
            ps2.setString(2, name_sh);
            ps2.setLong(3, Session.getResellerId());
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0 ? null : this;
            }
            Session.closeStatement(ps2);
            con.close();
            return null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_canUseCC() throws Exception {
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            PreparedStatement ps2 = con.prepareStatement("SELECT count(*) from active_merch_gateway WHERE reseller_id = ?");
            ps2.setLong(1, Session.getResellerId());
            ResultSet rs = ps2.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0 ? this : null;
            }
            Session.closeStatement(ps2);
            con.close();
            return null;
        } finally {
            Session.closeStatement(ps);
            con.close();
        }
    }

    public TemplateModel FM_setBillingNote(String str) throws Exception {
        Session.getLog().info("XXXXXXSetting billing note to: " + str);
        Session.setBillingNote(str);
        return null;
    }

    public TemplateModel FM_getActiveTransferPricedTLDs(int planId) throws Exception {
        return new TemplateList(DomainRegistrar.getActiveDomainTransferTLDs(Plan.getPlan(planId).getResourceType(TypeRegistry.getTypeId("domain_transfer"))));
    }

    public TemplateModel FM_getActiveTransferTLDs() throws Exception {
        return new TemplateList(DomainRegistrar.getActiveDomainTransferTLDs(Session.getAccount().getPlan().getResourceType(TypeRegistry.getTypeId("domain_transfer"))));
    }

    public TemplateModel FM_getActiveTLDs() throws Exception {
        return new TemplateList(DomainRegistrar.getActiveTLDs());
    }

    public TemplateModel FM_getActivePricedTLDs(int planId) throws Exception {
        Plan p = Plan.getPlan(planId);
        ResourceType rt = p.getResourceType(TypeRegistry.getTypeId("opensrs"));
        return new TemplateList(DomainRegistrar.getActivePricedTLDs(rt));
    }

    public TemplateModel FM_getRegistrarInfo(String tld) throws Exception {
        Map result = new HashMap();
        Entity reg = DomainRegistrar.getManager().getEntity(tld);
        result.put("description", reg.getDescription());
        result.put("signature", ((Registrar) reg).getSignature());
        return new TemplateMap(result);
    }

    public TemplateModel FM_getRegistrarInfoByExt(String ext) throws Exception {
        int ptIndex = ext.indexOf(".");
        String tld = ext.substring(ptIndex + 1);
        Session.getLog().debug("TLD from extension is: " + tld);
        Map result = new HashMap();
        Entity reg = DomainRegistrar.getManager().getEntity(tld);
        result.put("description", reg.getDescription());
        result.put("signature", ((Registrar) reg).getSignature());
        return new TemplateMap(result);
    }

    public TemplateModel FM_getTLDByExt(String ext) throws Exception {
        int ptIndex = ext.indexOf(".");
        return new TemplateString(ext.substring(ptIndex + 1));
    }

    public TemplateModel FM_isTransferable(String name) throws Exception {
        try {
            return DomainRegistrar.isTransferable(name) ? new TemplateOKResult() : new TemplateErrorResult(Localizer.translateMessage("domain.not_transferable", new Object[]{name}));
        } catch (RegistrarException e) {
            throw new HSUserException(e);
        }
    }

    public TemplateModel FM_lookup(String name) throws Exception {
        try {
            return new TemplateString(DomainRegistrar.lookup(name) ? "1" : "0");
        } catch (RegistrarException ex) {
            throw new HSUserException(ex);
        }
    }

    public TemplateModel FM_getYearsByTLD(String tld) throws Exception {
        List l = new ArrayList();
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            ps = con.prepareStatement("SELECT duration FROM tld_prices WHERE reseller_id = ? AND tld = ? AND duration > 0 ORDER BY duration");
            ps.setLong(1, 1L);
            ps.setString(2, tld);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                l.add(rs.getString(1));
            }
            Session.closeStatement(ps);
            con.close();
            return new TemplateList(l);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_getYearsByTLDAndPlan(String tld, String planId) throws Exception {
        if (tld.startsWith(".")) {
            tld = tld.substring(1);
        }
        Session.getLog().debug("----> tld = " + tld + "  planId = " + planId);
        TreeSet l = new TreeSet(new Comparator() { // from class: psoft.hsphere.HsphereToolbox.1
            @Override // java.util.Comparator
            public int compare(Object o1, Object o2) {
                int i1 = Integer.parseInt((String) o1);
                int i2 = Integer.parseInt((String) o2);
                if (i1 == i2) {
                    return 0;
                }
                return i1 < i2 ? -1 : 1;
            }

            @Override // java.util.Comparator
            public boolean equals(Object obj) {
                return equals(obj);
            }
        });
        Plan tmpPlan = Plan.getPlan(planId);
        l.addAll(DomainRegistrar.getYearsTLDPerPlan(tmpPlan, tld));
        return new TemplateList(Arrays.asList(l.toArray()));
    }

    public double monetaryRound(double number) {
        return Math.round(number * 100.0d) / 100.0d;
    }

    public TemplateModel FM_calculateTaxes(String totalValue) throws Exception {
        return FM_calculateTaxes(totalValue, null);
    }

    public TemplateModel FM_calculateTaxes(String totalValue, String biId) throws Exception {
        BillingInfoObject bi = null;
        boolean skipTaxes = false;
        TemplateHash res = new TemplateHash();
        String country = null;
        String state = null;
        if (biId == null || "".equals(biId)) {
            String exemptionCode = Session.getRequest().getParameter("_bi_exemption_code");
            if (exemptionCode == null || "".equals(exemptionCode)) {
                country = Session.getRequest().getParameter("_bi_country");
                if (country == null) {
                    try {
                        bi = Session.getAccount().getBillingInfo();
                    } catch (Exception e) {
                    }
                } else {
                    state = Session.getRequest().getParameter("_bi_state");
                    if ("NA".equals(state)) {
                        state = Session.getRequest().getParameter("_bi_state2");
                    }
                }
            } else {
                skipTaxes = true;
            }
        } else {
            bi = new BillingInfoObject(Long.parseLong(biId));
        }
        if (bi != null) {
            if (bi.getBillingType() == -1) {
                country = Session.getAccount().getContactInfo().getCountry();
                state = Session.getAccount().getContactInfo().getState();
                if ("NA".equals(state)) {
                    state = Session.getAccount().getContactInfo().getState2();
                }
            } else {
                country = Session.getAccount().getBillingInfo().getCountry();
                state = Session.getAccount().getBillingInfo().getState();
                if ("NA".equals(state)) {
                    state = Session.getAccount().getBillingInfo().getState2();
                }
            }
        } else if (country == null && !skipTaxes) {
            country = Session.getRequest().getParameter("_ci_country");
            state = Session.getRequest().getParameter("_ci_state");
            if ("NA".equals(state)) {
                state = Session.getRequest().getParameter("_ci_state2");
            }
        }
        double total = 0.0d;
        try {
            total = USFormat.parseDouble(totalValue);
        } catch (Exception e2) {
        }
        Hashtable taxCur = null;
        if (!skipTaxes) {
            taxCur = TaxBillEntry.calculateTaxes(total, country, state, bi);
        }
        double taxAmount = 0.0d;
        if (taxCur != null) {
            TemplateHash taxes = new TemplateHash();
            Enumeration e3 = taxCur.keys();
            while (e3.hasMoreElements()) {
                String taxId = (String) e3.nextElement();
                double curTaxAmount = ((Double) taxCur.get(taxId)).doubleValue();
                taxAmount += curTaxAmount;
                taxes.put(taxId, curTaxAmount != 0.0d ? USFormat.format(curTaxAmount) : "0");
            }
            Session.getLog().debug("taxAmount before rounding = " + taxAmount);
            double taxAmount2 = monetaryRound(taxAmount);
            Session.getLog().debug("taxAmount after rounding = " + taxAmount2);
            res.put("tax_amount", taxAmount2 != 0.0d ? USFormat.format(taxAmount2) : "0");
            res.put("taxes", taxes);
            res.put("total", total + taxAmount2 != 0.0d ? USFormat.format(total + taxAmount2) : "0");
            res.put("sub_total", total != 0.0d ? USFormat.format(total) : "0");
            return res;
        }
        res.put("total", new TemplateString(total));
        res.put("taxes", new TemplateList());
        return res;
    }

    public TemplateModel FM_getHiddenNumber(String number) throws Exception {
        return new TemplateString(GenericCreditCard.getHiddenNumber(number));
    }

    public TemplateModel FM_int2ext(String ip) throws Exception {
        return new TemplateString(Session.int2ext(ip));
    }

    public TemplateModel FM_ext2int(String ip) throws Exception {
        return new TemplateString(Session.ext2int(ip));
    }

    public TemplateModel FM_fixDomainStatus(String ip, String flag, String l_server_id) throws Exception {
        PreparedStatement ps = null;
        Connection con = Session.getDb();
        try {
            try {
                if (Integer.toString(-1).equals(flag)) {
                    ps = con.prepareStatement("UPDATE l_server_ips SET flag = 0, r_id = 0, r_type = 0  WHERE ip = ? AND l_server_id = ?");
                    ps.setString(1, ip);
                    ps.setInt(2, Integer.parseInt(l_server_id));
                } else if (Integer.toString(HostEntry.TAKEN_VPS_IP).equals(flag)) {
                    ps = con.prepareStatement("UPDATE l_server_ips SET flag = ?, r_id = ?, r_type = ?  WHERE ip = ? AND l_server_id = ?");
                    ps.setInt(1, HostEntry.VPS_IP);
                    ps.setNull(2, 4);
                    ps.setNull(3, 4);
                    ps.setString(4, ip);
                    ps.setInt(5, Integer.parseInt(l_server_id));
                }
                ps.executeUpdate();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception ex) {
                Session.getLog().error("Error fixing IP " + ip + " with flag " + flag + " for logical server " + l_server_id, ex);
                Session.closeStatement(ps);
                con.close();
            }
            return this;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_checkCurrencyPattern(String pattern, String decSeparator, String groupSeparator, String curSymbol) throws Exception {
        String result;
        try {
            DecimalFormat df = (DecimalFormat) NumberFormat.getCurrencyInstance();
            DecimalFormatSymbols ds = df.getDecimalFormatSymbols();
            if (curSymbol != null) {
                ds.setCurrencySymbol(curSymbol);
            }
            char gSep = groupSeparator.charAt(0);
            char dSep = decSeparator.charAt(0);
            if (gSep != 65535) {
                ds.setGroupingSeparator(gSep);
            }
            if (dSep != 65535) {
                ds.setMonetaryDecimalSeparator(dSep);
            }
            df.setDecimalFormatSymbols(ds);
            DecimalFormatSymbols ds1 = df.getDecimalFormatSymbols();
            Session.getLog().debug("Set Locale Group Separator = " + ds1.getGroupingSeparator());
            Session.getLog().debug("Set Locale Decimal Separator = " + ds1.getMonetaryDecimalSeparator());
            if (pattern != null) {
                df.applyPattern(pattern);
            }
            String tstString = df.format(123456.78d);
            Session.getLog().debug("Test of pattern: " + tstString);
            result = "1";
        } catch (IllegalArgumentException e) {
            result = "0";
            Session.getLog().error("Exception", e);
        }
        return new TemplateString(result);
    }

    public TemplateModel FM_formatPayPalAmount(String amount) throws Exception {
        try {
            double tmpValue = Double.parseDouble(amount);
            DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(new Locale("en", "US"));
            df.applyPattern("#,##0.00");
            String result = df.format(tmpValue);
            Session.getLog().debug("Result amount for PayPal = " + result);
            return new TemplateString(result);
        } catch (Exception e) {
            Session.getLog().error(e, (Throwable) null);
            return new TemplateString(amount);
        }
    }

    public TemplateModel FM_checkPayPalCustom(String custom) throws Exception {
        if (custom == null || "".equals(custom)) {
            return new TemplateString("-1");
        }
        String result = "0";
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        try {
            try {
                ps = con.prepareStatement("SELECT value, reseller_id FROM settings WHERE name = ?");
                ps.setString(1, "PayPal_CUSTOM");
                ResultSet rs = ps.executeQuery();
                while (true) {
                    if (!rs.next()) {
                        break;
                    }
                    String tmp = rs.getString(1);
                    long resId = rs.getLong(2);
                    if (custom.equals(tmp) && resId != Session.getResellerId()) {
                        result = "1";
                        break;
                    }
                }
                ps.close();
                Session.closeStatement(ps);
                con.close();
            } catch (Exception e) {
                result = "1";
                Session.getLog().error("Some errors in checkPayPalCustom()", e);
                Session.closeStatement(ps);
                con.close();
            }
            return new TemplateString(result);
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public TemplateModel FM_formatForHTML(String val) {
        if (val == null) {
            return null;
        }
        StringBuffer tmp = new StringBuffer("");
        int lastIndex = val.length();
        for (int i = 0; i < lastIndex; i++) {
            char ch = val.charAt(i);
            int index = specialChars.indexOf(ch);
            if (index == -1) {
                tmp.append(ch);
            } else {
                tmp.append(htmlSymbols[index]);
            }
        }
        return new TemplateString(tmp.toString());
    }

    public TemplateModel FM_getMailRelayHosts() throws Exception {
        return new TemplateList(HostManager.getMailRelayHosts(new Integer(Session.getAccount().getPlanValue("_HOST_mail")).intValue()));
    }

    public TemplateModel FM_getRegistrars() throws Exception {
        return XMLManager.getXMLAsTemplate("REGISTRAR_CONF", "registrars");
    }

    public TemplateModel FM_getGateways() throws Exception {
        TemplateModel gateways = null;
        try {
            gateways = XMLManager.getXMLAsTemplate("MERCHANT_GATEWAYS_CONF", "gateways");
        } catch (Exception ex) {
            Session.getLog().error("FM_getGateways(): Unable to get merchant gateways list from xml", ex);
        }
        return gateways;
    }

    public TemplateModel FM_getPaymentLink(String gateway, String sbalance, String prefix, String account, String description) throws Exception {
        Session.getLog().debug("getPaymentLink  gateway:" + gateway + " balance: " + sbalance + " prefix: " + prefix + " account: " + account + " desc: " + description);
        if ("CC".equals(gateway) || "Check".equals(gateway) || "".equals(gateway) || gateway == null) {
            return new TemplateString("");
        }
        User u = Session.getUser();
        String cpUrl = u.getCpContextURL();
        HashMap values = MerchantManager.getProcessorSettings(gateway);
        String servletName = (String) values.get("servlet");
        Session.getLog().debug("******" + values);
        String result = "";
        if (!"".equals(servletName) && servletName != null) {
            result = cpUrl + ExternalPayServlet.SERVLETPATH + servletName + "?amount=" + sbalance + "&trans_id=" + prefix + account + "&cp_url=" + cpUrl + "&action=redirect&description=" + URLEncoder.encode(description);
        }
        return new TemplateString(result);
    }

    public static boolean isOnlineHelpTemplate(String tName) {
        if (tName != null) {
            int idx = tName.indexOf("online_help/");
            return idx == 0 || idx == 1;
        }
        return false;
    }

    public static ResourceTypeList getTypeListByInterface(Class i) throws Exception {
        PreparedStatement ps = null;
        if (!i.isInterface()) {
            throw new Exception("Passed parameter can be only an interface");
        }
        ResourceTypeList rtl = (ResourceTypeList) typeLists.get(i);
        if (rtl != null) {
            return rtl;
        }
        ResourceTypeList rtl2 = new ResourceTypeList();
        Connection con = Session.getDb();
        try {
            ps = con.prepareStatement("SELECT DISTINCT type_id, class_name FROM plan_resource");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Class c = Class.forName(rs.getString("class_name"));
                if (Arrays.asList(c.getInterfaces()).contains(i)) {
                    rtl2.addType(rs.getInt("type_id"));
                }
            }
            typeLists.put(i, rtl2);
            Session.closeStatement(ps);
            con.close();
            return rtl2;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    /* loaded from: hsphere.zip:psoft/hsphere/HsphereToolbox$SignupGuardParams.class */
    static class SignupGuardParams implements TemplateMethodModel {
        SignupGuardParams() {
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List args) {
            try {
                return SignupGuard.get().get(HTMLEncoder.decode((String) args.get(0)));
            } catch (Exception e) {
                Session.getLog().error("Problem getting signup guard parameters:", e);
                return null;
            }
        }
    }

    public TemplateModel FM_getRegistrarTransDetails(long transactId) throws Exception {
        String planName;
        Connection con = Session.getDb();
        PreparedStatement ps = null;
        TemplateMap map = new TemplateMap();
        try {
            ps = con.prepareStatement("SELECT signature, domain, account_id, username, planid, created, period, request, response, result, tt_type, error_message FROM registrar_log WHERE id=?");
            ps.setLong(1, transactId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                map = new TemplateMap();
                map.put("registrar", rs.getString(1));
                map.put("domain", rs.getString(2));
                map.put("account_id", Session.getClobValue(rs, 3));
                map.put("username", rs.getString(4));
                int planid = rs.getInt(5);
                try {
                    planName = Plan.getPlan(planid).getDescription();
                } catch (Exception ex) {
                    planName = Integer.toString(planid);
                    Session.getLog().error("Unable to get plan id using id: " + planid, ex);
                }
                map.put("plan", planName);
                map.put("created", rs.getTimestamp(6));
                map.put("period", Integer.toString(rs.getInt(7)));
                if (Session.getResellerId() == 1) {
                    map.put("request", Session.getClobValue(rs, 8));
                    map.put("response", Session.getClobValue(rs, 9));
                } else {
                    String blocked = Localizer.translateLabel("admin.registrarlog.unavailable_data");
                    map.put("request", blocked);
                    map.put("response", blocked);
                }
                String result = rs.getInt(10) == 1 ? "Successful" : "Failed";
                map.put("result", result);
                map.put("trtype", LoggableRegistrar.getTtType(rs.getInt(11)));
                map.put("error", Session.getClobValue(rs, 12));
            }
            Session.closeStatement(ps);
            con.close();
            return map;
        } catch (Throwable th) {
            Session.closeStatement(ps);
            con.close();
            throw th;
        }
    }

    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static String getShortDateTimeStr(Timestamp date) {
        DateFormat df = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        String pattern = Settings.get().getValue("meddate");
        if (!isEmpty(pattern)) {
            try {
                df = new SimpleDateFormat(pattern);
            } catch (Exception ex) {
                Session.getLog().warn("Unable to format date by the " + pattern + " pattern: ", ex);
            }
        }
        return df.format((Date) date);
    }

    public static List availableDSTemplates(int planId) throws Exception {
        Session.getLog().debug("Inside HsphereToolbox::availableDSTemplates");
        List<DedicatedServerTemplate> templates = DSHolder.getAccessibleDSTemplates();
        Session.getLog().debug("Got " + (templates == null ? " null " : "" + templates.size()) + " templates total.filtering...");
        Plan p = Plan.getPlan(planId);
        ResourceType rt = p.getResourceType(TypeRegistry.getTypeId("ds"));
        List result = new ArrayList();
        String prefix = Globals.getAccessor().getSet("ds_templates").getPrefix();
        Session.getLog().debug("Got global keyset prefix " + prefix);
        for (DedicatedServerTemplate dst : templates) {
            Session.getLog().debug("Got and cheking dst " + dst.getId() + " valid=" + "1".equals(rt.getValue(prefix + dst.getId())));
            if ("1".equals(rt.getValue(prefix + dst.getId()))) {
                result.add(dst);
            }
        }
        return result;
    }

    public static TemplateModel FM_availableDSTemplates(int planId) throws Exception {
        return new TemplateList(availableDSTemplates(planId));
    }

    public static TemplateModel FM_getDedicatedServerTemplate(long dstId) throws Exception {
        return DSHolder.getAccessibleDSTemplate(dstId);
    }

    public String FM_getCurrentDateFormated(String dateFormat) throws Exception {
        SimpleDateFormat df = null;
        Date currDate = TimeUtils.getDate();
        try {
            df = new SimpleDateFormat(dateFormat);
            return df.format(currDate);
        } catch (Exception e) {
            return defaultDateFormat.format(df);
        }
    }

    public TemplateModel FM_getResellerIdByAccId(long accId) throws Exception {
        return new TemplateString(Reseller.getResellerIdByAccId(accId));
    }
}
