package psoft.hsphere.resource.ODBC;

import freemarker.template.TemplateListModel;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModel;
import freemarker.template.TemplateModelException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;
import psoft.hsphere.Resource;
import psoft.hsphere.ResourceId;
import psoft.hsphere.Session;
import psoft.hsphere.TemplateErrorResult;
import psoft.hsphere.resource.HostEntry;
import psoft.hsphere.resource.HostManager;
import psoft.hsphere.resource.WinHostEntry;
import psoft.util.freemarker.ListAdapter;
import psoft.util.freemarker.TemplateHash;
import psoft.util.freemarker.TemplateList;
import psoft.util.freemarker.TemplateString;

/* loaded from: hsphere.zip:psoft/hsphere/resource/ODBC/ODBCService.class */
public class ODBCService extends Resource {
    public ODBCService(int type, Collection initValues) throws Exception {
        super(type, initValues);
    }

    public ODBCService(ResourceId rId) throws Exception {
        super(rId);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public Collection getDriversParamsList() throws Exception {
        HostEntry he = recursiveGet("host");
        return ((WinHostEntry) he).exec("odbc-getdrivers.asp", (String[][]) new String[0]);
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateListModel FM_getDriversList() throws Exception {
        HostEntry he = recursiveGet("host");
        Collection driverList = ((WinHostEntry) he).exec("odbc-getdrivers.asp", (String[][]) new String[0]);
        TemplateListModel driversList = new ListAdapter(driverList);
        return driversList;
    }

    /* JADX WARN: Multi-variable type inference failed */
    /* JADX WARN: Type inference failed for: r2v1, types: [java.lang.String[], java.lang.String[][]] */
    public TemplateModel FM_getDriversParamsList(String driverName) throws Exception {
        if (driverName == null || "".equals(driverName)) {
            return null;
        }
        HostEntry he = recursiveGet("host");
        Collection<String> paramList = ((WinHostEntry) he).exec("odbc-getparams.asp", (String[][]) new String[]{new String[]{"driver", driverName}});
        TemplateList paramsList = new TemplateList();
        for (String str : paramList) {
            Session.getLog().debug("ODBCService params:" + str);
            StringTokenizer strTk = new StringTokenizer(str, "|");
            TemplateModel templateHash = new TemplateHash();
            if (strTk.hasMoreTokens()) {
                templateHash.put("Name", new TemplateString(strTk.nextToken().trim()));
            }
            if (strTk.hasMoreTokens()) {
                templateHash.put("template", new TemplateString(strTk.nextToken().trim()));
            }
            if (strTk.hasMoreTokens()) {
                templateHash.put("Value", new TemplateString(strTk.nextToken().trim()));
            }
            if (strTk.hasMoreTokens()) {
                templateHash.put("Description", new TemplateString(strTk.nextToken()));
            }
            if (strTk.hasMoreTokens()) {
                StringTokenizer list = new StringTokenizer(strTk.nextToken().trim(), ";");
                TemplateList tList = new TemplateList();
                while (list.hasMoreTokens()) {
                    tList.add((TemplateModel) new TemplateString(list.nextToken()));
                }
                templateHash.put("Select", tList);
            }
            paramsList.add(templateHash);
        }
        return paramsList;
    }

    public void createDSN(Collection value) throws Exception {
        createDSN(value, Long.parseLong(recursiveGet("host_id").toString()));
    }

    public void createDSN(Collection value, long hostId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(hostId);
        String name = recursiveGet("login").toString();
        value.add(new String[]{"user-name", name});
        he.exec("odbc-createdatasrc.asp", value);
    }

    public void modifyDSN(Collection value) throws Exception {
        WinHostEntry he = recursiveGet("host");
        String name = recursiveGet("login").toString();
        value.add(new String[]{"user-name", name});
        he.exec("odbc-updatedatasrc.asp", value);
    }

    public void removeDSN(String driverName, String DSN) throws Exception {
        removeDSN(driverName, DSN, Long.parseLong(recursiveGet("host_id").toString()));
    }

    public void removeDSN(String driverName, String DSN, long serverId) throws Exception {
        WinHostEntry he = (WinHostEntry) HostManager.getHost(serverId);
        List value = new ArrayList();
        String name = recursiveGet("login").toString();
        value.add(new String[]{"user-name", name});
        value.add(new String[]{"driver-name", driverName});
        value.add(new String[]{"DSN", DSN});
        he.exec("odbc-deletedatasrc.asp", value);
    }

    @Override // psoft.hsphere.Resource
    public TemplateModel get(String key) throws TemplateModelException {
        if ("prefix".equals(key)) {
            try {
                return new TemplateString(Session.getUser().getUserPrefix());
            } catch (Exception ex) {
                Session.getLog().error("Error getting user prefix ", ex);
                throw new TemplateModelException("Error getting user prefix for user " + Session.getUser().getLogin());
            }
        }
        return super.get(key);
    }

    public TemplateModel FM_dsn_creater(String resourceType, String mod) throws TemplateModelException {
        return new DSNCreater(resourceType, mod);
    }

    /* loaded from: hsphere.zip:psoft/hsphere/resource/ODBC/ODBCService$DSNCreater.class */
    class DSNCreater implements TemplateMethodModel {
        List args = new ArrayList();
        String resourceType;
        String mod;

        public DSNCreater(String resourceType, String mod) {
            ODBCService.this = r5;
            this.resourceType = "dsn_record";
            this.mod = "";
            this.resourceType = resourceType;
            this.mod = mod;
        }

        public boolean isEmpty() {
            return false;
        }

        public TemplateModel exec(List l) throws TemplateModelException {
            ResourceId addChild;
            try {
                if (l.isEmpty()) {
                    Session.getLog().debug("DSNCreator:Create");
                    try {
                        synchronized (Session.getAccount()) {
                            addChild = ODBCService.this.addChild(this.resourceType, this.mod, this.args);
                        }
                        return addChild;
                    } catch (Exception e) {
                        ODBCService.getLog().warn("Error adding new child: " + l, e);
                        return new TemplateErrorResult(e);
                    }
                }
                String key = (String) l.get(0);
                String value = (String) l.get(1);
                this.args.add(key == null ? "" : key);
                this.args.add(value == null ? "" : value);
                return null;
            } catch (Exception e2) {
                Session.getLog().error("Error create dsn record", e2);
                return new TemplateErrorResult(e2);
            }
        }
    }
}
