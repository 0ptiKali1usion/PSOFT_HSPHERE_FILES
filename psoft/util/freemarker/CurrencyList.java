package psoft.util.freemarker;

import freemarker.template.SimpleHash;
import freemarker.template.TemplateHashModel;
import freemarker.template.TemplateListModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;

/* loaded from: hsphere.zip:psoft/util/freemarker/CurrencyList.class */
public class CurrencyList implements TemplateListModel, TemplateHashModel {
    protected int current = 0;
    public static final CurrencyList list = new CurrencyList();
    public static final String[][] currenciesArray = {new String[]{"AFA", "Afghani"}, new String[]{"ALL", "Lek"}, new String[]{"DZD", "Algerian Dinar"}, new String[]{"AON", "New Kwanza"}, new String[]{"ARS", "Argentine Peso"}, new String[]{"AWG", "Aruban Guilder"}, new String[]{"AUD", "Australian Dollar"}, new String[]{"BSD", "Bahamian Dollar"}, new String[]{"BHD", "Bahraini Dinar"}, new String[]{"BDT", "Taka"}, new String[]{"BBD", "Barbados Dollar"}, new String[]{"BZD", "Belize Dollar"}, new String[]{"BMD", "Bermudian Dollar"}, new String[]{"BOB", "Boliviano"}, new String[]{"BAD", "Bosnian Dinar"}, new String[]{"BWP", "Pula"}, new String[]{"BRL", "Real"}, new String[]{"BND", "Brunei Dollar"}, new String[]{"BGL", "Lev"}, new String[]{"XOF", "CFA Franc BCEAO"}, new String[]{"BIF", "Burundi Franc"}, new String[]{"KHR", "Cambodia Riel"}, new String[]{"XAF", "CFA Franc BEAC"}, new String[]{"CAD", "Canadian Dollar"}, new String[]{"CVE", "Cape Verde Escudo"}, new String[]{"KYD", "Cayman Islands Dollar"}, new String[]{"CLP", "Chilean Peso"}, new String[]{"CNY", "Yuan Renminbi"}, new String[]{"COP", "Colombian Peso"}, new String[]{"KMF", "Comoro Franc"}, new String[]{"CRC", "Costa Rican Colon"}, new String[]{"HRK", "Croatian Kuna"}, new String[]{"CUP", "Cuban Peso"}, new String[]{"CYP", "Cyprus Pound"}, new String[]{"CZK", "Czech Koruna"}, new String[]{"DKK", "Danish Krone"}, new String[]{"DJF", "Djibouti Franc"}, new String[]{"XCD", "East Caribbean Dollar"}, new String[]{"DOP", "Dominican Peso"}, new String[]{"TPE", "Timor Escudo"}, new String[]{"ECS", "Ecuador Sucre"}, new String[]{"EGP", "Egyptian Pound"}, new String[]{"SVC", "El Salvador Colon"}, new String[]{"EEK", "Kroon"}, new String[]{"ETB", "Ethiopian Birr"}, new String[]{"EUR", "Euro"}, new String[]{"FKP", "Falkland Islands Pound"}, new String[]{"FJD", "Fiji Dollar"}, new String[]{"XPF", "CFP Franc"}, new String[]{"GMD", "Dalasi"}, new String[]{"GHC", "Cedi"}, new String[]{"GIP", "Gibraltar Pound"}, new String[]{"GTQ", "Quetzal"}, new String[]{"GNF", "Guinea Franc"}, new String[]{"GWP", "Guinea - Bissau Peso"}, new String[]{"GYD", "Guyana Dollar"}, new String[]{"HTG", "Gourde"}, new String[]{"HNL", "Lempira"}, new String[]{"HKD", "Hong Kong Dollar"}, new String[]{"HUF", "Forint"}, new String[]{"ISK", "Iceland Krona"}, new String[]{"INR", "Indian Rupee"}, new String[]{"IDR", "Rupiah"}, new String[]{"IRR", "Iranian Rial"}, new String[]{"IQD", "Iraqi Dinar"}, new String[]{"ILS", "Shekel"}, new String[]{"JMD", "Jamaican Dollar"}, new String[]{"JPY", "Yen"}, new String[]{"JOD", "Jordanian Dinar"}, new String[]{"KZT", "Tenge"}, new String[]{"KES", "Kenyan Shilling"}, new String[]{"KRW", "Won"}, new String[]{"KPW", "North Korean Won"}, new String[]{"KWD", "Kuwaiti Dinar"}, new String[]{"KGS", "Som"}, new String[]{"LAK", "Kip"}, new String[]{"LVL", "Latvian Lats"}, new String[]{"LBP", "Lebanese Pound"}, new String[]{"LSL", "Loti"}, new String[]{"LRD", "Liberian Dollar"}, new String[]{"LYD", "Libyan Dinar"}, new String[]{"LTL", "Lithuanian Litas"}, new String[]{"MOP", "Pataca"}, new String[]{"MKD", "Denar"}, new String[]{"MGF", "Malagasy Franc"}, new String[]{"MWK", "Kwacha"}, new String[]{"MYR", "Malaysian Ringitt"}, new String[]{"MVR", "Rufiyaa"}, new String[]{"MTL", "Maltese Lira"}, new String[]{"MRO", "Ouguiya"}, new String[]{"MUR", "Mauritius Rupee"}, new String[]{"MXN", "Mexico Peso"}, new String[]{"MNT", "Mongolia Tugrik"}, new String[]{"MAD", "Moroccan Dirham"}, new String[]{"MZM", "Metical"}, new String[]{"MMK", "Myanmar Kyat"}, new String[]{"NAD", "Namibian Dollar"}, new String[]{"NPR", "Nepalese Rupee"}, new String[]{"ANG", "Netherlands Antilles Guilder"}, new String[]{"NZD", "New Zealand Dollar"}, new String[]{"NIO", "Cordoba Oro"}, new String[]{"NGN", "Naira"}, new String[]{"NOK", "Norwegian Krone"}, new String[]{"OMR", "Rial Omani"}, new String[]{"PKR", "Pakistan Rupee"}, new String[]{"PAB", "Balboa"}, new String[]{"PGK", "New Guinea Kina"}, new String[]{"PYG", "Guarani"}, new String[]{"PEN", "Nuevo Sol"}, new String[]{"PHP", "Philippine Peso"}, new String[]{"PLN", "New Zloty"}, new String[]{"QAR", "Qatari Rial"}, new String[]{"ROL", "Leu"}, new String[]{"RUR", "Russian Ruble"}, new String[]{"RWF", "Rwanda Franc"}, new String[]{"WST", "Tala"}, new String[]{"STD", "Dobra"}, new String[]{"SAR", "Saudi Riyal"}, new String[]{"SCR", "Seychelles Rupee"}, new String[]{"SLL", "Leone"}, new String[]{"SGD", "Singapore Dollar"}, new String[]{"SKK", "Slovak Koruna"}, new String[]{"SIT", "Tolar"}, new String[]{"SBD", "Solomon Islands Dollar"}, new String[]{"SOS", "Somalia Shilling"}, new String[]{"ZAR", "Rand"}, new String[]{"LKR", "Sri Lanka Rupee"}, new String[]{"SHP", "St Helena Pound"}, new String[]{"SDP", "Sudanese Pound"}, new String[]{"SRG", "Suriname Guilder"}, new String[]{"SZL", "Swaziland Lilangeni"}, new String[]{"SEK", "Sweden Krona"}, new String[]{"CHF", "Swiss Franc"}, new String[]{"SYP", "Syrian Pound"}, new String[]{"TWD", "New Taiwan Dollar"}, new String[]{"TJR", "Tajik Ruble"}, new String[]{"TZS", "Tanzanian Shilling"}, new String[]{"THB", "Baht"}, new String[]{"TOP", "Tonga Pa'anga"}, new String[]{"TTD", "Trinidad & Tobago Dollar"}, new String[]{"TND", "Tunisian Dinar"}, new String[]{"TRL", "Turkish Lira"}, new String[]{"UGX", "Uganda Shilling"}, new String[]{"UAH", "Ukrainian Hryvnia"}, new String[]{"AED", "United Arab Emirates Dirham"}, new String[]{"GBP", "Pounds Sterling"}, new String[]{"USD", "US Dollar"}, new String[]{"UYU", "Uruguayan Peso"}, new String[]{"VUV", "Vanuatu Vatu"}, new String[]{"VEB", "Venezuela Bolivar"}, new String[]{"VND", "Viet Nam Dong"}, new String[]{"YER", "Yemeni Rial"}, new String[]{"YUM", "Yugoslavian New Dinar"}, new String[]{"ZRN", "New Zaire"}, new String[]{"ZMK", "Zambian Kwacha"}, new String[]{"ZWD", "Zimbabwe Dollar"}};
    public static final SimpleHash[] currencies = new SimpleHash[currenciesArray.length];

    /* JADX WARN: Type inference failed for: r0v2, types: [java.lang.String[], java.lang.String[][]] */
    static {
        for (int i = 0; i < currenciesArray.length; i++) {
            SimpleHash hash = new SimpleHash();
            hash.put("name", currenciesArray[i][1]);
            hash.put("code", currenciesArray[i][0]);
            currencies[i] = hash;
        }
    }

    public boolean isEmpty() {
        return false;
    }

    public boolean hasNext() {
        return this.current < currencies.length;
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

    public String find(String key) {
        for (int i = 0; i < currenciesArray.length; i++) {
            if (currenciesArray[i][0].equals(key)) {
                return currenciesArray[i][1];
            }
        }
        return null;
    }

    public TemplateModel get(int index) {
        return currencies[index];
    }

    public TemplateModel next() {
        TemplateModel[] templateModelArr = currencies;
        int i = this.current;
        this.current = i + 1;
        return templateModelArr[i];
    }

    public void rewind() {
        this.current = 0;
    }
}
