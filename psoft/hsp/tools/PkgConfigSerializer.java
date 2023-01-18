package psoft.hsp.tools;

import java.util.ArrayList;
import org.apache.xerces.dom.DocumentImpl;
import org.apache.xpath.XPathAPI;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/* loaded from: hsphere.zip:psoft/hsp/tools/PkgConfigSerializer.class */
public class PkgConfigSerializer {
    private PkgConfig conf;
    private Document confAsXML;
    private Element root;

    public PkgConfigSerializer(PkgConfig conf) {
        this.conf = conf;
        this.confAsXML = new DocumentImpl();
        this.root = this.confAsXML.createElement("pkg");
        this.root.setAttribute("name", "NameOfThePackage");
        this.root.setAttribute("description", "Description for test package");
        this.root.setAttribute("info", "Additional information");
        this.root.setAttribute("vendor", "Vendor");
        this.root.setAttribute("version", "00.00.00");
        this.root.setAttribute("build", "00");
        Element description = this.confAsXML.createElement("description");
        description.appendChild(this.confAsXML.createTextNode("Put here description of thepackage"));
        this.confAsXML = convert2XML();
    }

    public PkgConfigSerializer(Document doc, String pkgPrefix) throws Exception {
        this.confAsXML = doc;
        this.conf = convert2PkgConfig(pkgPrefix);
    }

    public Document getXMLConfig() {
        return this.confAsXML;
    }

    public PkgConfig getConfig() {
        return this.conf;
    }

    private Document convert2XML() {
        Element pkgActions = null;
        if (this.conf.getPassedParameters().contains("--with-templates")) {
            Element el = this.confAsXML.createElement("templates");
            PkgConfig pkgConfig = this.conf;
            el.setAttribute("src", PkgConfig.getPkgSrcPath("--with-templates"));
            this.root.appendChild(el);
        }
        if (this.conf.getPassedParameters().contains("--with-classes")) {
            Element el2 = this.confAsXML.createElement("classes");
            PkgConfig pkgConfig2 = this.conf;
            el2.setAttribute("src", PkgConfig.getPkgSrcPath("--with-classes"));
            this.root.appendChild(el2);
        }
        if (this.conf.getPassedParameters().contains("--with-xmls")) {
            Element el3 = this.confAsXML.createElement("xmls");
            PkgConfig pkgConfig3 = this.conf;
            el3.setAttribute("src", PkgConfig.getPkgSrcPath("--with-xmls"));
            this.root.appendChild(el3);
        }
        if (this.conf.getPassedParameters().contains("--with-jars")) {
            Element el4 = this.confAsXML.createElement("jars");
            PkgConfig pkgConfig4 = this.conf;
            el4.setAttribute("src", PkgConfig.getPkgSrcPath("--with-jars"));
            this.root.appendChild(el4);
        }
        if (this.conf.getPassedParameters().contains("--with-properties")) {
            Element el5 = this.confAsXML.createElement("config");
            PkgConfig pkgConfig5 = this.conf;
            el5.setAttribute("path", PkgConfig.getPkgSrcPath("--with-properties"));
            this.root.appendChild(el5);
        }
        if (this.conf.getPassedParameters().contains("--with-lang-bundle")) {
            Element el6 = this.confAsXML.createElement("lang_bundle");
            PkgConfig pkgConfig6 = this.conf;
            el6.setAttribute("src", PkgConfig.getPkgSrcPath("--with-lang-bundle"));
            this.root.appendChild(el6);
        }
        if (this.conf.getPassedParameters().contains("--with-images")) {
            Element el7 = this.confAsXML.createElement("images");
            PkgConfig pkgConfig7 = this.conf;
            el7.setAttribute("src", PkgConfig.getPkgSrcPath("--with-images"));
            this.root.appendChild(el7);
        }
        if (this.conf.getPassedParameters().contains("--with-scripts") || this.conf.getPassedParameters().contains("--with-scripts-advanced")) {
            PkgConfig pkgConfig8 = this.conf;
            String path = PkgConfig.getPkgSrcPath("--with-scripts-advanced");
            if (path == null) {
                PkgConfig pkgConfig9 = this.conf;
                path = PkgConfig.getPkgSrcPath("--with-scripts");
            }
            Element el8 = this.confAsXML.createElement("scripts");
            el8.setAttribute("src", path);
            this.root.appendChild(el8);
        }
        if (this.conf.getPassedParameters().contains("--with-sql")) {
            Element el9 = this.confAsXML.createElement("sql");
            PkgConfig pkgConfig10 = this.conf;
            el9.setAttribute("src", PkgConfig.getPkgSrcPath("--with-sql"));
            this.root.appendChild(el9);
        }
        if (this.conf.getPassedParameters().contains("--with-sql-uninstall")) {
            Element el10 = this.confAsXML.createElement("sql-uninstall");
            PkgConfig pkgConfig11 = this.conf;
            el10.setAttribute("src", PkgConfig.getPkgSrcPath("--with-sql-uninstall"));
            this.root.appendChild(el10);
        }
        if (this.conf.getPassedParameters().contains("--with-sql-upgrade")) {
            Element el11 = this.confAsXML.createElement("sql-upgrade");
            PkgConfig pkgConfig12 = this.conf;
            el11.setAttribute("src", PkgConfig.getPkgSrcPath("--with-sql-upgrade"));
            this.root.appendChild(el11);
        }
        if (this.conf.getPassedParameters().contains("--with-rpms")) {
            Comment c = this.confAsXML.createComment("\n    <rpms>\n\t<rpm name=\"RPM name with version and build\" server_group=\"WEB|MAIL|DNS\">\n\t\t<platform name=\"RH72|RH73|RHES2.1|RHAS2.1|FBSD4\" location=\"ftp://|http://|BUILT-IN\"/>\n\t</rpm>\n    </rpms>\n    ");
            this.root.appendChild(c);
        }
        if (this.conf.getPassedParameters().contains("--with-tarballs")) {
            Comment c2 = this.confAsXML.createComment("\n    <tarballs>\n\t<tarball name=\"tarball name\" server_group=\"WEB|MAIL|DNS\">\n\t\t<platform name=\"RH72|RH73|RHES2.1|RHAS2.1|FBSD4\" location=\"ftp://|http://|BUILT-IN\"/>\n\t</tarball>\n    </tarballs>\n    ");
            this.root.appendChild(c2);
        }
        if (this.conf.getPassedParameters().contains("--with-preinstall")) {
            if (0 == 0) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el12 = this.confAsXML.createElement("pre_install");
            Document document = this.confAsXML;
            PkgConfig pkgConfig13 = this.conf;
            el12.appendChild(document.createTextNode(PkgConfig.getPkgSrcPath("--with-preinstall")));
            pkgActions.appendChild(el12);
        }
        if (this.conf.getPassedParameters().contains("--with-postinstall")) {
            if (pkgActions == null) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el13 = this.confAsXML.createElement("post_install");
            Document document2 = this.confAsXML;
            PkgConfig pkgConfig14 = this.conf;
            el13.appendChild(document2.createTextNode(PkgConfig.getPkgSrcPath("--with-postinstall")));
            pkgActions.appendChild(el13);
        }
        if (this.conf.getPassedParameters().contains("--with-preupgrade")) {
            if (pkgActions == null) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el14 = this.confAsXML.createElement("pre_upgrade");
            Document document3 = this.confAsXML;
            PkgConfig pkgConfig15 = this.conf;
            el14.appendChild(document3.createTextNode(PkgConfig.getPkgSrcPath("--with-preupgrade")));
            pkgActions.appendChild(el14);
        }
        if (this.conf.getPassedParameters().contains("--with-postupgrade")) {
            if (pkgActions == null) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el15 = this.confAsXML.createElement("post_upgrade");
            Document document4 = this.confAsXML;
            PkgConfig pkgConfig16 = this.conf;
            el15.appendChild(document4.createTextNode(PkgConfig.getPkgSrcPath("--with-postupgrade")));
            pkgActions.appendChild(el15);
        }
        if (this.conf.getPassedParameters().contains("--with-preuninstall")) {
            if (pkgActions == null) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el16 = this.confAsXML.createElement("pre_uninstall");
            Document document5 = this.confAsXML;
            PkgConfig pkgConfig17 = this.conf;
            el16.appendChild(document5.createTextNode(PkgConfig.getPkgSrcPath("--with-preuninstall")));
            pkgActions.appendChild(el16);
        }
        if (this.conf.getPassedParameters().contains("--with-postuninstall")) {
            if (pkgActions == null) {
                pkgActions = this.confAsXML.createElement("actions");
            }
            Element el17 = this.confAsXML.createElement("post_uninstall");
            Document document6 = this.confAsXML;
            PkgConfig pkgConfig18 = this.conf;
            el17.appendChild(document6.createTextNode(PkgConfig.getPkgSrcPath("--with-postuninstall")));
            pkgActions.appendChild(el17);
        }
        if (pkgActions != null) {
            this.root.appendChild(pkgActions);
        }
        this.confAsXML.appendChild(this.root);
        return this.confAsXML;
    }

    private PkgConfig convert2PkgConfig(String pkgPrefix) throws Exception {
        ArrayList params = new ArrayList();
        if (this.confAsXML == null) {
            return null;
        }
        params.add("--with-prefix=" + pkgPrefix);
        Element root = this.confAsXML.getDocumentElement();
        NodeList list = XPathAPI.selectNodeList(root, "*");
        for (int i = 0; i < list.getLength(); i++) {
            Node n = list.item(i);
            if ("templates".equals(n.getNodeName())) {
                params.add("--with-templates=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("classes".equals(n.getNodeName())) {
                params.add("--with-classes=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("xmls".equals(n.getNodeName())) {
                params.add("--with-xmls=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("jars".equals(n.getNodeName())) {
                params.add("--with-jars=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("config".equals(n.getNodeName())) {
                params.add("--with-properties=" + n.getAttributes().getNamedItem("path").getNodeValue());
            } else if ("lang_bundle".equals(n.getNodeName())) {
                params.add("--with-lang-bundle=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("images".equals(n.getNodeName())) {
                params.add("--with-images=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("scripts".equals(n.getNodeName())) {
                params.add("--with-scripts=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("sql".equals(n.getNodeName())) {
                params.add("--with-sql=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("sql-uninstall".equals(n.getNodeName())) {
                params.add("-with-sql-uninstall=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("sql-upgrade".equals(n.getNodeName())) {
                params.add("-with-sql-upgrade=" + n.getAttributes().getNamedItem("src").getNodeValue());
            } else if ("rpms".equals(n.getNodeName())) {
                params.add("--with-rpms=" + PkgConfig.getPkgSrcPath("--with-rpms"));
            } else if ("tarballs".equals(n.getNodeName())) {
                params.add("--with-tarballs=" + PkgConfig.getPkgSrcPath("--with-tarballs"));
            } else if ("actions".equals(n.getNodeName())) {
                NodeList actList = XPathAPI.selectNodeList(n, "*");
                for (int j = 0; j < actList.getLength(); j++) {
                    Node actNode = actList.item(j);
                    if ("pre_install".equals(actNode.getNodeName())) {
                        params.add("--with-preinstall=" + actNode.getFirstChild().getNodeValue());
                    } else if ("post_install".equals(actNode.getNodeName())) {
                        params.add("--with-postinstall=" + actNode.getFirstChild().getNodeValue());
                    } else if ("pre_uninstall".equals(actNode.getNodeName())) {
                        params.add("--with-preuninstall=" + actNode.getFirstChild().getNodeValue());
                    } else if ("post_uninstall".equals(actNode.getNodeName())) {
                        params.add("--with-postuninstall=" + actNode.getFirstChild().getNodeValue());
                    } else if ("pre_upgrade".equals(actNode.getNodeName())) {
                        params.add("--with-preupgrade=" + actNode.getFirstChild().getNodeValue());
                    } else if ("post_upgrade".equals(actNode.getNodeName())) {
                        params.add("--with-postupgrade=" + actNode.getFirstChild().getNodeValue());
                    }
                }
            }
        }
        String pkgName = root.getAttribute("name");
        String pkgDescription = root.getAttribute("description");
        String pkgInfo = root.getAttribute("info");
        String pkgVendor = root.getAttribute("vendor");
        String pkgVersion = root.getAttribute("version");
        String pkgBuild = root.getAttribute("build");
        String[] a = new String[params.size()];
        for (int i2 = 0; i2 < params.size(); i2++) {
            a[i2] = (String) params.get(i2);
        }
        return new PkgConfig(a, pkgName, pkgDescription, pkgInfo, pkgVendor, pkgVersion, pkgBuild);
    }
}
