package psoft.util.freemarker;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/* loaded from: hsphere.zip:psoft/util/freemarker/CountryList.class */
public class CountryList implements TemplateListModel, TemplateHashModel {
    protected int current = 0;
    public static final CountryList list = new CountryList();
    public static final String[][] countriesArray = {new String[]{"AF", "country.AF"}, new String[]{"AL", "country.AL"}, new String[]{"DZ", "country.DZ"}, new String[]{"DS", "country.DS"}, new String[]{"AD", "country.AD"}, new String[]{"AO", "country.AO"}, new String[]{"AI", "country.AI"}, new String[]{"AQ", "country.AQ"}, new String[]{"AG", "country.AG"}, new String[]{"AR", "country.AR"}, new String[]{"AM", "country.AM"}, new String[]{"AW", "country.AW"}, new String[]{"AU", "country.AU"}, new String[]{"AT", "country.AT"}, new String[]{"AZ", "country.AZ"}, new String[]{"BS", "country.BS"}, new String[]{"BH", "country.BH"}, new String[]{"BD", "country.BD"}, new String[]{"BB", "country.BB"}, new String[]{"BY", "country.BY"}, new String[]{"BE", "country.BE"}, new String[]{"BZ", "country.BZ"}, new String[]{"BJ", "country.BJ"}, new String[]{"BM", "country.BM"}, new String[]{"BT", "country.BT"}, new String[]{"BO", "country.BO"}, new String[]{"BA", "country.BA"}, new String[]{"BW", "country.BW"}, new String[]{"BV", "country.BV"}, new String[]{"BR", "country.BR"}, new String[]{"IO", "country.IO"}, new String[]{"BN", "country.BN"}, new String[]{"BG", "country.BG"}, new String[]{"BF", "country.BF"}, new String[]{"BI", "country.BI"}, new String[]{"CA", "country.CA"}, new String[]{"KH", "country.KH"}, new String[]{"CM", "country.CM"}, new String[]{"CV", "country.CV"}, new String[]{"KY", "country.KY"}, new String[]{"CF", "country.CF"}, new String[]{"TD", "country.TD"}, new String[]{"CL", "country.CL"}, new String[]{"CN", "country.CN"}, new String[]{"CX", "country.CX"}, new String[]{"CC", "country.CC"}, new String[]{"CO", "country.CO"}, new String[]{"KM", "country.KM"}, new String[]{"CG", "country.CG"}, new String[]{"CK", "country.CK"}, new String[]{"CR", "country.CR"}, new String[]{"HR", "country.HR"}, new String[]{"CU", "country.CU"}, new String[]{"CY", "country.CY"}, new String[]{"CZ", "country.CZ"}, new String[]{"DK", "country.DK"}, new String[]{"DJ", "country.DJ"}, new String[]{"DM", "country.DM"}, new String[]{"DO", "country.DO"}, new String[]{"TP", "country.TP"}, new String[]{"EC", "country.EC"}, new String[]{"EG", "country.EG"}, new String[]{"SV", "country.SV"}, new String[]{"GQ", "country.GQ"}, new String[]{"ER", "country.ER"}, new String[]{"EE", "country.EE"}, new String[]{"ET", "country.ET"}, new String[]{"FK", "country.FK"}, new String[]{"FO", "country.FO"}, new String[]{"FJ", "country.FJ"}, new String[]{"FI", "country.FI"}, new String[]{"FR", "country.FR"}, new String[]{"FX", "country.FX"}, new String[]{"GF", "country.GF"}, new String[]{"PF", "country.PF"}, new String[]{"TF", "country.TF"}, new String[]{"GA", "country.GA"}, new String[]{"GM", "country.GM"}, new String[]{"GE", "country.GE"}, new String[]{"DE", "country.DE"}, new String[]{"GH", "country.GH"}, new String[]{"GI", "country.GI"}, new String[]{"GR", "country.GR"}, new String[]{"GL", "country.GL"}, new String[]{"GD", "country.GD"}, new String[]{"GP", "country.GP"}, new String[]{"GU", "country.GU"}, new String[]{"GT", "country.GT"}, new String[]{"GN", "country.GN"}, new String[]{"GW", "country.GW"}, new String[]{"GY", "country.GY"}, new String[]{"HT", "country.HT"}, new String[]{"HM", "country.HM"}, new String[]{"HN", "country.HN"}, new String[]{"HK", "country.HK"}, new String[]{"HU", "country.HU"}, new String[]{"IS", "country.IS"}, new String[]{"IN", "country.IN"}, new String[]{"ID", "country.ID"}, new String[]{"IR", "country.IR"}, new String[]{"IQ", "country.IQ"}, new String[]{"IE", "country.IE"}, new String[]{"IL", "country.IL"}, new String[]{"IT", "country.IT"}, new String[]{"CI", "country.CI"}, new String[]{"JM", "country.JM"}, new String[]{"JP", "country.JP"}, new String[]{"JO", "country.JO"}, new String[]{"KZ", "country.KZ"}, new String[]{"KE", "country.KE"}, new String[]{"KI", "country.KI"}, new String[]{"KP", "country.KP"}, new String[]{"KR", "country.KR"}, new String[]{"KW", "country.KW"}, new String[]{"KG", "country.KG"}, new String[]{"LA", "country.LA"}, new String[]{"LV", "country.LV"}, new String[]{"LB", "country.LB"}, new String[]{"LS", "country.LS"}, new String[]{"LR", "country.LR"}, new String[]{"LY", "country.LY"}, new String[]{"LI", "country.LI"}, new String[]{"LT", "country.LT"}, new String[]{"LU", "country.LU"}, new String[]{"MO", "country.MO"}, new String[]{"MK", "country.MK"}, new String[]{"MG", "country.MG"}, new String[]{"MW", "country.MW"}, new String[]{"MY", "country.MY"}, new String[]{"MV", "country.MV"}, new String[]{"ML", "country.ML"}, new String[]{"MT", "country.MT"}, new String[]{"MH", "country.MH"}, new String[]{"MQ", "country.MQ"}, new String[]{"MR", "country.MR"}, new String[]{"MU", "country.MU"}, new String[]{"TY", "country.TY"}, new String[]{"MX", "country.MX"}, new String[]{"FM", "country.FM"}, new String[]{"MD", "country.MD"}, new String[]{"MC", "country.MC"}, new String[]{"MN", "country.MN"}, new String[]{"MS", "country.MS"}, new String[]{"MA", "country.MA"}, new String[]{"MZ", "country.MZ"}, new String[]{"MM", "country.MM"}, new String[]{"NA", "country.NA"}, new String[]{"NR", "country.NR"}, new String[]{"NP", "country.NP"}, new String[]{"NL", "country.NL"}, new String[]{"AN", "country.AN"}, new String[]{"NC", "country.NC"}, new String[]{"NZ", "country.NZ"}, new String[]{"NI", "country.NI"}, new String[]{"NE", "country.NE"}, new String[]{"NG", "country.NG"}, new String[]{"NU", "country.NU"}, new String[]{"NF", "country.NF"}, new String[]{"MP", "country.MP"}, new String[]{"NO", "country.NO"}, new String[]{"OM", "country.OM"}, new String[]{"PK", "country.PK"}, new String[]{"PW", "country.PW"}, new String[]{"PA", "country.PA"}, new String[]{"PG", "country.PG"}, new String[]{"PY", "country.PY"}, new String[]{"PE", "country.PE"}, new String[]{"PH", "country.PH"}, new String[]{"PN", "country.PN"}, new String[]{"PL", "country.PL"}, new String[]{"PT", "country.PT"}, new String[]{"PR", "country.PR"}, new String[]{"QA", "country.QA"}, new String[]{"RE", "country.RE"}, new String[]{"RO", "country.RO"}, new String[]{"RU", "country.RU"}, new String[]{"RW", "country.RW"}, new String[]{"DN", "country.DN"}, new String[]{"LC", "country.LC"}, new String[]{"VC", "country.VC"}, new String[]{"WS", "country.WS"}, new String[]{"SM", "country.SM"}, new String[]{"ST", "country.ST"}, new String[]{"SA", "country.SA"}, new String[]{"SN", "country.SN"}, new String[]{"SC", "country.SC"}, new String[]{"SL", "country.SL"}, new String[]{"SG", "country.SG"}, new String[]{"SK", "country.SK"}, new String[]{"SI", "country.SI"}, new String[]{"SB", "country.SB"}, new String[]{"SO", "country.SO"}, new String[]{"ZA", "country.ZA"}, new String[]{"GS", "country.GS"}, new String[]{"ES", "country.ES"}, new String[]{"LK", "country.LK"}, new String[]{"SH", "country.SH"}, new String[]{"PM", "country.PM"}, new String[]{"SD", "country.SD"}, new String[]{"SR", "country.SR"}, new String[]{"SJ", "country.SJ"}, new String[]{"SZ", "country.SZ"}, new String[]{"SE", "country.SE"}, new String[]{"CH", "country.CH"}, new String[]{"SY", "country.SY"}, new String[]{"TW", "country.TW"}, new String[]{"TJ", "country.TJ"}, new String[]{"TZ", "country.TZ"}, new String[]{"TH", "country.TH"}, new String[]{"TG", "country.TG"}, new String[]{"TK", "country.TK"}, new String[]{"TO", "country.TO"}, new String[]{"TT", "country.TT"}, new String[]{"TN", "country.TN"}, new String[]{"TR", "country.TR"}, new String[]{"TM", "country.TM"}, new String[]{"TC", "country.TC"}, new String[]{"TV", "country.TV"}, new String[]{"UG", "country.UG"}, new String[]{"UA", "country.UA"}, new String[]{"AE", "country.AE"}, new String[]{"GB", "country.GB"}, new String[]{"US", "country.US"}, new String[]{"UM", "country.UM"}, new String[]{"UY", "country.UY"}, new String[]{"UZ", "country.UZ"}, new String[]{"VU", "country.VU"}, new String[]{"VA", "country.VA"}, new String[]{"VE", "country.VE"}, new String[]{"VN", "country.VN"}, new String[]{"VG", "country.VG"}, new String[]{"VI", "country.VI"}, new String[]{"WF", "country.WF"}, new String[]{"EH", "country.EH"}, new String[]{"YE", "country.YE"}, new String[]{"YU", "country.YU"}, new String[]{"ZR", "country.ZR"}, new String[]{"ZM", "country.ZM"}, new String[]{"ZW", "country.ZW"}, new String[]{"KN", "country.KN"}};
    public static final SimpleHash[] countries = new SimpleHash[countriesArray.length];

    /* JADX WARN: Type inference failed for: r0v2, types: [java.lang.String[], java.lang.String[][]] */
    static {
        for (int i = 0; i < countriesArray.length; i++) {
            SimpleHash hash = new SimpleHash();
            hash.put("name", countriesArray[i][1]);
            hash.put("code", countriesArray[i][0]);
            countries[i] = hash;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean hasNext() {
        return this.current < countries.length;
    }

    public boolean isRewound() {
        return this.current == 0;
    }

    public TemplateModel get(String key) throws TemplateModelException {
        if (key == null) {
            return null;
        }
        return new TemplateString(find(key.toUpperCase()));
    }

    public TemplateModel get(int index) {
        return countries[index];
    }

    public String find(String key) {
        for (int i = 0; i < countriesArray.length; i++) {
            if (countriesArray[i][0].equals(key)) {
                return countriesArray[i][1];
            }
        }
        return null;
    }

    public TemplateModel next() {
        TemplateModel[] templateModelArr = countries;
        int i = this.current;
        this.current = i + 1;
        return templateModelArr[i];
    }

    public void rewind() {
        this.current = 0;
    }
}
