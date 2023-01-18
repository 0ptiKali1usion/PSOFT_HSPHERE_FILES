package psoft.hsphere;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Category;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import psoft.hsphere.resource.apache.ErrorDocumentResource;
import psoft.hsphere.util.XMLManager;

/* loaded from: hsphere.zip:psoft/hsphere/MenuBuilder.class */
public class MenuBuilder {
    protected static Element menusNode;
    private static final Category log = Category.getInstance(MenuBuilder.class.getName());
    protected static Map menusHash = new HashMap();
    protected static Map menus = new HashMap();
    protected static HashMap topMenus = new HashMap();
    protected static HashMap menuHash = new HashMap();
    protected static Map resolvedMenus = new HashMap();

    public static synchronized HashMap getAllMenus(String configFile) {
        try {
            Document doc = XMLManager.getXML(configFile);
            Element root = doc.getDocumentElement();
            NodeList menus2 = root.getElementsByTagName("menus");
            menusNode = (Element) menus2.item(0);
            NodeList menuTags = menusNode.getElementsByTagName(LangBundlesCompiler.INT_MENU_BUNDLE);
            for (int i = 0; i < menuTags.getLength(); i++) {
                Element curItem = (Element) menuTags.item(i);
                String menuName = curItem.getAttribute("name");
                if (!resolvedMenus.containsKey(menuName)) {
                    HashMap th = new HashMap();
                    th.put("type", LangBundlesCompiler.INT_MENU_BUNDLE);
                    th.put("name", curItem.getAttribute("name"));
                    String tmpLabel = curItem.getAttribute("label");
                    th.put("label", tmpLabel);
                    th.put("def_item", curItem.getAttribute("defaultitem"));
                    th.put("platform_type", curItem.getAttribute("platform_type"));
                    th.put("resource", curItem.getAttribute("resource"));
                    String tmpTip = curItem.getAttribute("tip");
                    th.put("tip", tmpTip);
                    th.put("items", buildMenu(curItem));
                    menuHash.put(menuName, th);
                }
            }
            NodeList interfacesList = root.getElementsByTagName("interface");
            Element interfaceTag = (Element) interfacesList.item(0);
            NodeList menuDefs = interfaceTag.getElementsByTagName("menudef");
            for (int i2 = 0; i2 < menuDefs.getLength(); i2++) {
                Element menuDefEl = (Element) menuDefs.item(i2);
                String menuDefId = menuDefEl.getAttributes().getNamedItem("id").getNodeValue();
                topMenus.put(menuDefId, buildHolder(menuDefEl));
            }
        } catch (Exception ex) {
            Session.getLog().error("Some error happens during menu building, because ", ex);
            Session.getLog().debug(ex);
        }
        return topMenus;
    }

    protected static synchronized List buildMenu(Element menuEl) {
        ArrayList lst = new ArrayList();
        String menuName = menuEl.getAttributes().getNamedItem("name").getNodeValue();
        resolvedMenus.put(menuName, menuName);
        NodeList nodes = menuEl.getElementsByTagName("*");
        for (int i = 0; i < nodes.getLength(); i++) {
            Element currEl = (Element) nodes.item(i);
            if ("menuitem".equals(currEl.getTagName())) {
                lst.add(addItem(currEl));
            }
            if ("initmenu".equals(currEl.getTagName())) {
                lst.add(addSub(currEl));
            }
        }
        return lst;
    }

    protected static HashMap addItem(Element el) {
        HashMap hash = new HashMap();
        hash.put("type", "item");
        hash.put("name", el.getAttributes().getNamedItem("name").getNodeValue());
        hash.put("label", el.getAttributes().getNamedItem("label").getNodeValue());
        hash.put(ErrorDocumentResource.MTYPE_URL, el.getAttributes().getNamedItem(ErrorDocumentResource.MTYPE_URL).getNodeValue());
        hash.put("tip", el.getAttributes().getNamedItem("tip").getNodeValue());
        hash.put("platform_type", el.getAttribute("platform_type"));
        hash.put("resource", el.getAttribute("resource"));
        hash.put("new_window", el.getAttribute("new_window"));
        return hash;
    }

    protected static Element findMenuElement(String name) {
        NodeList list = menusNode.getElementsByTagName(LangBundlesCompiler.INT_MENU_BUNDLE);
        for (int i = 0; i < list.getLength(); i++) {
            Element elem = (Element) list.item(i);
            if (elem.getAttributes().getNamedItem("name").getNodeValue().equals(name)) {
                return (Element) list.item(i);
            }
        }
        return null;
    }

    protected static HashMap addSub(Element el) {
        HashMap hash = new HashMap();
        String menuName = el.getAttributes().getNamedItem("name").getNodeValue();
        Element menuToInit = findMenuElement(menuName);
        if (menuToInit == null) {
            System.out.println("Can not find menu for initmenu with name=" + menuName);
        }
        hash.put("type", LangBundlesCompiler.INT_MENU_BUNDLE);
        hash.put("name", menuToInit.getAttributes().getNamedItem("name").getNodeValue());
        hash.put("label", menuToInit.getAttributes().getNamedItem("label").getNodeValue());
        hash.put("def_item", menuToInit.getAttributes().getNamedItem("defaultitem").getNodeValue());
        hash.put("tip", menuToInit.getAttributes().getNamedItem("tip").getNodeValue());
        hash.put("items", buildMenu(menuToInit));
        return hash;
    }

    protected static MenuItemsHolder buildHolder(Element menuDefEl) {
        String menuDefId = menuDefEl.getAttributes().getNamedItem("id").getNodeValue();
        MenuItemsHolder top = new MenuItemsHolder(menuDefId, "TOP", "", "", "", null, "tip");
        NodeList lst = menuDefEl.getElementsByTagName("*");
        for (int i = 0; i < lst.getLength(); i++) {
            Element item = (Element) lst.item(i);
            if ("initmenu".equals(item.getTagName())) {
                String itemName = item.getAttributes().getNamedItem("name").getNodeValue();
                try {
                    top.addHolder(getItemsHolder((HashMap) menuHash.get(itemName), top));
                } catch (NullPointerException npe) {
                    log.error("Cannot find item: " + itemName);
                    throw npe;
                }
            } else if ("menuitem".equals(item.getTagName())) {
                top.addItem(getItem(addItem(item), top));
            }
        }
        return top;
    }

    protected static MenuItemsHolder getItemsHolder(HashMap itemContainer, MenuItemsHolder parent) {
        String name = (String) itemContainer.get("name");
        String label = (String) itemContainer.get("label");
        String defItemId = (String) itemContainer.get("def_item");
        String tip = (String) itemContainer.get("tip");
        String platform_type = (String) itemContainer.get("platform_type");
        String resource = (String) itemContainer.get("resource");
        List items = (List) itemContainer.get("items");
        MenuItemsHolder menu = new MenuItemsHolder(name, label, defItemId, platform_type, resource, parent, tip);
        for (int i = 0; i < items.size(); i++) {
            HashMap item = (HashMap) items.get(i);
            String itemType = (String) item.get("type");
            if ("item".equals(itemType)) {
                menu.addItem(getItem(item, menu));
            } else if (LangBundlesCompiler.INT_MENU_BUNDLE.equals(itemType)) {
                menu.addHolder(getItemsHolder(item, menu));
            }
        }
        return menu;
    }

    protected static MenuItem getItem(HashMap itemContainer, MenuItemsHolder parent) {
        String name = (String) itemContainer.get("name");
        String label = (String) itemContainer.get("label");
        String URL = (String) itemContainer.get(ErrorDocumentResource.MTYPE_URL);
        String platform_type = (String) itemContainer.get("platform_type");
        String resource = (String) itemContainer.get("resource");
        String tip = (String) itemContainer.get("tip");
        String new_window = (String) itemContainer.get("new_window");
        return new MenuItem(name, label, URL, platform_type, resource, parent, tip, new_window);
    }
}
