/***************************************************************************
 <copyright>
 * Copyright (c) 1999-2005 Positive Software Corporation
 </copyright>
 ***************************************************************************/

var leftMenu = new Array();
var items = {};
var menuAr = new Array();
var pushImpl = propertyExists("push", menuAr);

var QM = '"';
var SQM = "'";
var IMG_PREFIX = "img_";
var TD_PREFIX = "td_";
var spacerImg;
var mPointerImg;
var actualMenuImg;
var leftMenuLayer = "leftMenu";
var activeMenu = null;
var M_COLORS = 4;
var COLOR_STYLE_PREF = "menuLeftColor";

function initMenu(menuLayer, spacerImgURL, mPointerImgURL, actualMenuImgURL, rtlDirection) {
  leftMenuLayer = menuLayer;
  spacerImg = spacerImgURL;
  mPointerImg = mPointerImgURL;
  actualMenuImg = actualMenuImgURL;
}

function menuObj(id, label, link, targetWindow) {
  this.id = fixId(id);
  this.isMenu = true;
  this.isActivated = false;
  this.isActual = false;
  this.label = label;
  this.link = link;
  this.target = null;
  this.level = 0;
  this.parent = null;
  this.items = new Array();
  this.to = null;
  this.layerClass = null;
  this.color_style = '';
}

function itemObj(id, label, link, targetWindow) {
  this.id = fixId(id);
  this.isMenu = false;
  this.isActivated = false;
  this.label = label;
  this.link = link;
  this.target = targetWindow;
  this.level = 0;
  this.parent = null;
  this.color_style = '';
}

function addMenu(parentMenu, id, label, link, targetWindow, active) {
  var menu = new menuObj(id, label, link, targetWindow);
  if (parentMenu == "") {
    pushArray(leftMenu, menu);
  } else {
    var parent = getItem(parentMenu);
    menu.level = parent.level + 1;
    if (parent != null) {
      menu.parent = parent;
      menu.isLayerPrepared = false;
      pushArray(parent.items, menu);
    }
  }
  if (active) {
    activeMenu = menu.id;
    menu.isActual = true;
  }
  menu.color_style = getColorStyle(menu.level);
  items[menu.id] = menu;
  pushArray(menuAr, menu);
}

function addItem(parentMenu, id, label, link, targetWindow, active) {
  var item = new itemObj(id, label, link, targetWindow);
  if (parentMenu == "") {
    pushArray(leftMenu, item);
  } else {
    var parent = getItem(parentMenu);
    if (parent != null) {
      item.parent = parent;
      if (active) activeMenu = parent.id;
      item.level = parent.level + 1;
      pushArray(parent.items, item);
    }
  }
  if (active) item.isActivated = true;
  item.color_style = getColorStyle(item.level);
  items[item.id] = item;
}

function fixId(id) {
  if (getItem(id) != null) {
    var ind = 0;
    while (getItem(id + ind) != null) ind++;
    id += ind;
  }
  return id;
}

function getColorStyle(level) {
  var colorId = level + 1;
  if (colorId > M_COLORS) colorId %= M_COLORS;
  if (colorId == 0) colorId = M_COLORS;
  return COLOR_STYLE_PREF + colorId;
}

function getItem(id) {
  if (propertyExists(id, items)) return items[id];
  return null;
}

function uncoverMenu(menuId) {
  activateMenu(menuId);
  showContent(leftMenuLayer, 1, -1, -1, leftMenuTable());
}

function activateMenu(menuId) {
  if (!menuId) {
    menuId = activeMenu;
    activeMenu = null;
  }
  var aMenu = getItem(menuId);
  if (aMenu) {
    var aParent = aMenu.parent;
    if (activeMenu != menuId) {
      activeMenu = menuId;
    } else {
      activeMenu = aParent ? aParent.id : null;
    }
    for (var i = 0; i < menuAr.length; i++) {
      var menu = menuAr[i];
      menu.isActivated = (activeMenu == menu.id);
    }
    while (aParent) {
      aParent.isActivated = true;
      aParent = aParent.parent;
    }
  }
}

function menuOn(menuId) {
  setElementClass(TD_PREFIX + menuId, 'menuLeftActiveItem');
  return true;
}

function menuOut(menuId) {
  var menu = getItem(menuId);
  if (menu) {
    setElementClass(TD_PREFIX + menuId, menu.color_style);
  }
  return true;
}

function setElementClass(id, className) {
  var obj = getElement(id);
  if (obj != null) obj.className = className;
}

function showLeftMenu() {
  uncoverMenu(null);
}

function leftMenuTable() {
  var datecolwidth;
  var output = '<TABLE WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING=0>';
  for (var i = 0; i < leftMenu.length; i++) {
    var item = leftMenu[i];
    output += drawItem(item);
  }
  output += "</TABLE>";
//alert(output);
  return output;
}

function drawItem(item) {
  var lTd = '';
  for (var i = item.level; i > 1; i--) lTd += '&nbsp;';
  if (item.isMenu) {
    var imgTd = '<IMG src="' + (item.isActual ? actualMenuImg : mPointerImg)
	+ '" BORDER="0">';
    if (item.level > 0) {
      lTd += imgTd + '&nbsp;';
      imgTd = '<IMG SRC="' + spacerImg + '" width="14" height="1">';
    }
    var content = '<TR><TD width="14" align="center">' + imgTd + '</TD>'
    	+ '<TD width="1" class="menuLeftSeparator"><IMG SRC="' + spacerImg
    	+ '" width="1" height="1"></TD><TD width="100%" valign="middle" id="'
    	+ TD_PREFIX + item.id + '" class="' + item.color_style
    	+ '" onMouseOver="menuOn(' + SQM + item.id + SQM
	+ ')" onMouseOut="menuOut(' +SQM + item.id + SQM
	+ ')" onClick="uncoverMenu(' + SQM + item.id + SQM
	+ ')" style="cursor:pointer">' + lTd + item.label + '</TD></TR>';
    if (item.isActivated) {
      content += drawSubMenu(item.id);
    }
    return withLineSeparator(content, item.level);
  } else {
    return withLineSeparator('<TR>'
	+ '<TD width="14"><IMG SRC="' + spacerImg + '" width="14" height="1"></TD>'
	+ '<TD width="1" class="menuLeftSeparator"><IMG SRC="' + spacerImg
	+ '" width="1" height="1"></TD><TD width="100%" valign="middle" id="'
	+ TD_PREFIX + item.id + '" class="' + item.color_style + '"><a href="'
        + item.link + '"' + (item.target ? 'target = "' + item.target + '"' : '')
	+ '><span style="outline: none' + (item.isActivated ? '; font-weight: bold"' : '"')
	+ '>&nbsp;' + lTd + item.label + '&nbsp;<span></a></TD></TR>', item.level);
  }
}

function withLineSeparator(content, level) {
  if (level == 0) {
    return content + '<TR><TD colspan="3" class="menuLeftSeparator"><IMG SRC="'
	+ spacerImg + '"></TD></TR>';
  } else {
    return '<TR><TD width="14" align="center"><IMG SRC="' + spacerImg
	+ '" width="14" height="1"></TD>'
	+ '<TD colspan="2" class="menuLeftSeparator"><IMG SRC="'
	+ spacerImg + '"></TD></TR>' + content;
  }
}

function drawSubMenu(menuId) {
  var datecolwidth;
  var menu = getItem(menuId);
  var output = "";
  if (menu) {
    var items = menu.items;
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
      output += drawItem(item);
    }
  }
  return output;
}

var DOC_EL = 1;
var DOC_ALL = 2;
var DOC_LAY = 3;
var getElementMethod = chooseGetElementMethod();

function chooseGetElementMethod() {
  if (isNN && propertyExists("layers", document) && (document.layers != null)) return DOC_LAY;
  if (propertyExists("all", document) && (document.all != null)) return DOC_ALL;
  if (propertyExists("getElementById", document) && (document.getElementById != null)) return DOC_EL;
  return 0;
}

function showContent(id, state, lax, lay, content) {
  var lElement = getElement(id);
  if (lElement) {
    if (getElementMethod == DOC_EL) {
  	if (content) {
	  var rng = document.createRange();
	  rng.setStartBefore(lElement);
	  var htmlFrag = rng.createContextualFragment(content);
	  while(lElement.hasChildNodes()) lElement.removeChild(lElement.lastChild);
	  lElement.appendChild(htmlFrag);
	}
	if (lax >= 0) lElement.style.left = lax + "px";
	if (lay >= 0) lElement.style.top = lay + "px";
	lElement.style.visibility = state ? "visible" : "hidden";
    } else if (getElementMethod == DOC_ALL) {
	if (content) {
	  lElement.innerHTML = content;
	}
	if (lax >= 0) lElement.style.left = lax;
	if (lay >= 0) lElement.style.top = lay;
	lElement.style.visibility = state ? "visible" : "hidden";
    } else if (getElementMethod == DOC_LAY) {
	if (lax >= 0) lElement.left = lax;
	if (lay >= 0) lElement.top = lay;
	if (content) {
	  lElement.document.open();
	  lElement.document.writeln(content);
	  lElement.document.close();
	}
	lElement.visibility = state ? "show" : "hide";
    }
  }
}

function getElement(id) {
  if (getElementMethod == DOC_EL) return document.getElementById(id);
  else if (getElementMethod == DOC_ALL) return document.all[''+id];
  else if (getElementMethod == DOC_LAY) return document.layers[''+id];
  return null;
}

function pushArray(arObj, newElem) {
  if (pushImpl) {
    arObj.push(newElem);
  } else {
    arObj[arObj.length] = newElem;
  }
}
