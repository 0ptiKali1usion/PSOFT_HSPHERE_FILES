/***************************************************************************
 <copyright>
 * Copyright (c) 1999-2006 Positive Software Corporation
 </copyright>
 ***************************************************************************/

var topMenu = new Array();
var items = {};
var menuAr = new Array();
var pushImpl = propertyExists("push", menuAr);

var QM = '"';
var SQM = "'";
var IMG_PREFIX = "img_";
var TD_PREFIX = "td_";
var MSO_PREFIX = "mso_";
var LAY_PREFIX = "lay_";
var MT_PREFIX = "mt_";
var spacerImg;
var mPointerImgSrc;
var isRtlDir = false;
var aLeft = "left";
var aRight = "right";
var mnu_index = 0;
var item_index = 0;
var topMenuLayer = "topMenu";
var timeOutMil = 300;
var topTO = null;
var activeTMItem = null;
var activeMenu = null;
var actLocked = false;
var lockTimeOut = 50;
var llOffset = 10;

var inputsNeedBeHidden = (isIE || isKonqueror) ;
var inpBoxes = null;

function initMenu(menuLayer, spacerImgURL, mPointerImgAtt, rtlDirection) {
  topMenuLayer = menuLayer;
  spacerImg = spacerImgURL;
  mPointerImgSrc = mPointerImgAtt;
  if (rtlDirection != "") {
    isRtlDir = true;
    aLeft = "right";
    aRight = "left";
  }
}

function menuObj(id, label, link, targetWindow) {
  this.id = fixId(id);
  this.isMenu = true;
  this.isItem = false;
  this.isActivated = false;
  this.label = label;
  this.link = link;
  this.target = targetWindow;
  this.isTop = false;
  this.parent = null;
  this.items = new Array();
  this.to = null;
  this.layerClass = null;
}

function itemObj(id, label, link, targetWindow) {
  this.id = fixId(id);
  this.isMenu = false;
  this.isItem = true;
  this.isActivated = false;
  this.label = label;
  this.link = link;
  this.target = targetWindow;
  this.isTop = false;
  this.parent = null;
}

function addMenu(parentMenu, id, label, link, targetWindow, active) {
  var menu = new menuObj(id, label, link, targetWindow, active);
  if (parentMenu == "") {
    menu.isTop = true;
    if (active) activeTMItem = menu.id;
    menu.activate = function() {
      if (activeMenu != this.id || !this.isActivated) {
	setElementClass(TD_PREFIX + this.id, 'menuTopActiveItem');
	var mso = getElement(TD_PREFIX + this.id);
	var msoWidth = getWidth(mso);
	var mt = getElement(MT_PREFIX + this.id);
	var mtWidth = getWidth(mt);
	var lax = getPositionLeft(mso);
	var lay = getPositionTop(mso) + getHeight(mso);
	if (isRtlDir) {
	  if (lax + msoWidth - mtWidth > 0) lax += msoWidth - mtWidth;
	} else {
	  if ((lax + mtWidth > getWindowWidth()) && (mtWidth > msoWidth))
		lax += msoWidth - mtWidth;
	}
	if (inputsNeedBeHidden) hideInputsBeneath(this.id, lax, lay);
	showSubMenu(this.id, lax, lay);
	activeMenu = this.id;
	this.isActivated = true;
      }
    }
    menu.deactivate = function() {
      if (this.isActivated) {
	hideSubMenu(this.id);
	setElementClass(TD_PREFIX + this.id, 'menuTopItem');
	if (inputsNeedBeHidden) showInputsBeneath(this.id);
	this.isActivated = false;
	if (activeMenu == this.id) activeMenu = null;
      }
    }
    pushArray(topMenu, menu);
  } else {
    var parent = getItem(parentMenu);
    menu.activate = function() {
      if (activeMenu != this.id || !this.isActivated) {
	setElementClass(TD_PREFIX + this.id, 'menuSubActiveItem');
	var mso = getElement(IMG_PREFIX + this.id);
	var msoWidth = getWidth(mso);
	var mt = getElement(MT_PREFIX + this.id);
	var mtWidth = getWidth(mt);
	var lax = getPositionLeft(mso);
	var lay = getPositionTop(mso);
	var clx = 0;
	var cly = 0;
	if (isKonqueror && (brAgentVer < 3.2)) {
	  var tml = getElement(topMenuLayer);
	  clx = getPositionLeft(tml);
	  cly = getPositionTop(tml);
	  lax -= clx;
	  lay -= cly;
	}
	if (isRtlDir) {
	  if (lax < mtWidth) {
	    var md = getElement(TD_PREFIX + this.id);
	    lax = getPositionLeft(md) - clx + msoWidth + llOffset;
	    lay = getPositionTop(md) - cly + getHeight(md) - 1;
	  } else {
	    lax -= mtWidth;
	  }
	} else {
	  var pageWidth = getWindowWidth();
	  if ((lax + msoWidth + mtWidth > pageWidth) && (pageWidth > 0)) {
	    var md = getElement(TD_PREFIX + this.id);
	    lax -= llOffset;
	    if (lax + mtWidth > pageWidth) {
	      lax = getPositionLeft(md) - clx + msoWidth + llOffset;
	      if (lax + mtWidth < pageWidth) lax = pageWidth - mtWidth;
	    }
	    lay = getPositionTop(md) - cly + getHeight(md) - 1;
	  } else {
	    lax += msoWidth;
	  }
	}
	if (inputsNeedBeHidden) hideInputsBeneath(this.id, lax, lay);
	showSubMenu(this.id, lax, lay);
	activeMenu = this.id;
	this.isActivated = true;
      }
    }
    menu.deactivate = function() {
      if (this.isActivated) {
	hideSubMenu(this.id);
	setElementClass(TD_PREFIX + this.id, 'menuSubItem');
	if (inputsNeedBeHidden) showInputsBeneath(this.id);
	this.isActivated = false;
	if (activeMenu == this.id) activeMenu = null;
      }
    }
    if (parent != null) {
      menu.parent = parent;
      menu.isLayerPrepared = false;
      pushArray(parent.items, menu);
    }
  }
  menu.layerClass = "subMenuLayer";
  items[menu.id] = menu;
  pushArray(menuAr, menu);
}

function addItem(parentMenu, id, label, link, targetWindow, active) {
  var item = new itemObj(id, label, link, targetWindow, active);
  if (parentMenu == "") {
    item.isTop = true;
    if (active) activeTMItem = item.id;
    item.activate = function() {
      setElementClass(TD_PREFIX + this.id, 'menuTopActiveItem');
      this.isActivated = true;
    }
    item.deactivate = function() {
      setElementClass(TD_PREFIX + this.id, 'menuTopItem');
      this.isActivated = false;
    }
    pushArray(topMenu, item);
  } else {
    item.activate = function() {
      setElementClass(TD_PREFIX + this.id, 'menuSubActiveItem');
      this.isActivated = true;
    }
    item.deactivate = function() {
      setElementClass(TD_PREFIX + this.id, 'menuSubItem');
      this.isActivated = false;
    }
    var parent = getItem(parentMenu);
    if (parent != null) {
      item.parent = parent;
      pushArray(parent.items, item);
    }
  }
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

function getItem(id) {
  if (propertyExists(id, items)) return items[id];
  return null;
}

function activateMenu(menuId) {
  var curMenu = getItem(menuId);
  if (curMenu) {
    var parentId = curMenu.parent ? curMenu.parent.id : "";
    if (activeMenu && (activeMenu != parentId)) {
      hideActiveButMenu(menuId);
    }
    var pChain = new Array();
    while (curMenu) {
      if (curMenu.to) {
	clearTimeout(curMenu.to);
	curMenu.to = null;
      }
      pushArray(pChain, curMenu);
      curMenu = curMenu.parent;
    }
    var pi = pChain.length - 1;
    while (pi >= 0) pChain[pi--].activate();
  }
}

function deactivateMenu(menuId) {
  var menu = getItem(menuId);
  if (menu) {
    menu.to = window.setTimeout("getItem('" + menuId + "').deactivate()", timeOutMil);
    var parent = menu.parent;
    while (parent && (parent.id != activeMenu)) {
      parent.to = window.setTimeout("getItem('" + parent.id + "').deactivate()", timeOutMil);
      parent = parent.parent;
    }
  }
}

function menuOn(menuId) {
  if (!setLock()) {
    setTimeout('menuOn("' + menuId + '")', lockTimeOut);
    return false;
  }
  if (activeTMItem) hideActiveTMItem();
  activateMenu(menuId);
  unLock();
  return true;
}

function menuOut(menuId) {
  if (!setLock()) {
    setTimeout('menuOut("' + menuId + '")', lockTimeOut);
    return false;
  }
  var menu = getItem(menuId);
  if (menu.items) {
    var items = menu.items;
    var ind = items.length - 1;
    while (ind >= 0) {
      if (items[ind--].isActivated) {
	unLock();
	return true;
      }
    }
  }
  deactivateMenu(menuId);
  unLock();
  return true;
}

function itemOn(itemId) {
  if (!setLock()) {
    setTimeout('itemOn("' + itemId + '")', lockTimeOut);
    return false;
  }
  var item = getItem(itemId);
  if (item) {
    if (activeTMItem) hideActiveTMItem();
    if (item.parent) {
      var parentId = item.parent ? item.parent.id : "";
      hideActiveButMenu(parentId);
      activateMenu(parentId);
    } else {
      hideActiveButMenu("");
    }
    item.activate();
  }
  unLock();
  return true;
}

function itemOut(itemId) {
  if (!setLock()) {
    setTimeout('itemOut("' + itemId + '")', lockTimeOut);
    return false;
  }
  var item = getItem(itemId);
  if (item) {
    item.deactivate();
    if (item.parent) deactivateMenu(item.parent.id);
  }
  unLock();
  return true;
}

function hideActiveTMItem() {
  var item = getItem(activeTMItem);
  if (item) item.deactivate();
  activeTMItem = null;
}

function hideActiveButMenu(menuId) {
  var prevMenu = getItem(activeMenu);
  while(prevMenu && (prevMenu.id != menuId) ) {
    if (prevMenu.to) {
      clearTimeout(prevMenu.to);
      prevMenu.to = null;
    }
    prevMenu.deactivate();
    prevMenu = prevMenu.parent;
  }
}

function hideInputsBeneath(menuId, lax, lay) {
  var mt = getElement(MT_PREFIX + menuId);
  if (mt) {
    if (inpBoxes == null) {
      inpBoxes = new Array();
      var hSelect = isIE || isKonqueror;
      var hRadio = isKonqueror;
      var hCheckbox = isKonqueror;
      var hText = (isIE5 || isKonqueror);
      var hButton = isKonqueror;
      for (var fi = document.forms.length-1; fi >= 0 ; fi--) {
	var fElements = document.forms[fi].elements;
	for (var ei = fElements.length - 1; ei >= 0; ei--) {
	  var elem = fElements[ei];
	  if (propertyExists("type", elem) && (
	  	((elem.type == "select-one") && hSelect)
		|| ((elem.type == "radio") && hRadio)
		|| ((elem.type == "checkbox") && hCheckbox)
		|| ((elem.type == "text") && hText)
		|| ((elem.type == "textarea") && hText)
		|| ((elem.type == "password") && hText)
		|| ((elem.type == "submit") && hButton)
		|| ((elem.type == "button") && hButton))) {
	    elem.overlapped = 0;
	    elem.menus = {};
	    pushArray(inpBoxes, elem);
	  }
	}
      }
      if (inpBoxes.length < 1) {
	inputsNeedBeHidden = false;
	return;
      }
    }
    var mtLeft = lax ? lax : getPositionLeft(mt);
    var mtTop = lay ? lay : getPositionTop(mt);
    var mtRight = mtLeft + getWidth(mt);
    var mtBottom = mtTop + getHeight(mt);
    for (var ii = inpBoxes.length - 1; ii >= 0; ii--) {
      var fld = inpBoxes[ii];
      fldLeft = getPositionLeft(fld);
      fldTop = getPositionTop(fld);
      fldRight = fldLeft + getWidth(fld);
      fldBottom = fldTop + getHeight(fld);
      if (((((fldLeft < mtLeft) && (fldRight > mtLeft))
		|| ((mtLeft < fldLeft) && (mtRight > fldLeft)))
	&& (((fldTop < mtTop) && (fldBottom > mtTop))
		|| ((mtTop < fldTop) && (mtBottom > fldTop))))
	&& (!propertyExists(menuId, fld.menus) || !fld.menus[menuId])) {
	fld.overlapped++;
	fld.menus[menuId] = true;
	fld.style.visibility = 'hidden';
      }
    }
  }
}

function showInputsBeneath(menuId) {
  if (getItem(menuId) && (inpBoxes != null)) {
    for (var ii = inpBoxes.length - 1; ii >= 0; ii--) {
      var fld = inpBoxes[ii];
      if ((fld.overlapped > 0) && propertyExists(menuId, fld.menus) && (fld.menus[menuId])) {
	fld.menus[menuId] = null;
	if (--fld.overlapped < 1) {
	  fld.style.visibility = 'visible';
	}
      }
    }
  }
}

function navigate(itemId) {
  hideActiveButMenu("");
  var item = getItem(itemId);
  if (item) {
    if (item.target)
      window.open(item.link, item.target);
    else
      document.location = item.link;
  }
}

function setElementClass(id, className) {
  var obj = getElement(id);
  if (obj != null) obj.className = className;
}

var wsl_tm = null;

function setLock() {
  if (actLocked) {
    if (!wsl_tm) wsl_tm = setTimeout("unLock()", lockTimeOut);
    return false;
  }
  if (wsl_tm) {
    clearTimeout(wsl_tm);
    wsl_tm = null;
  }
  actLocked = true;
  return true;
}

function unLock() {
  if (actLocked) {
    actLocked = false;
  }
}

function showTopMenu() {
  if (!isRtlDir || isGecko || isIE) {
    for (var i = 0; i < menuAr.length; i++) {
      var menu = menuAr[i];
      document.write('<div id="' + LAY_PREFIX + menu.id + '" class="' + menu.layerClass + '">'
	+ subMenuTable(menu.id) + '</div>');
    }
    showContent(topMenuLayer, 1, "", "", topMenuTable());
  }
}

function showSubMenu(menuId, lax, lay) {
  var menu = getItem(menuId);
  if (menu.to == null) {
    menu.to = subMenuTable(menuId);
    showContent(LAY_PREFIX + menuId, 1, lax, lay);
  } else {
    showContent(LAY_PREFIX + menuId, 1, lax, lay);
  }
}

function hideSubMenu(menuId) {
  showContent(LAY_PREFIX + menuId, 0, 0, 0);
}

function topMenuTable() {
  var datecolwidth;
  var output = '<TABLE WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING=0><TR><TD>' // Main table
    + '<TABLE WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0" ALIGN="left"><TR>'
    + '<TD class="menuTopLeftEdge"><IMG SRC="' + spacerImg + '"></TD>'; // Left edge
  for (var i = 0; i < topMenu.length; i++) {
    var item = topMenu[i];
    var fPrefix = item.isMenu ? "menu" : "item";
    var cPrefix = 'menuTopItem';
    if (activeTMItem && (activeTMItem == item.id)) {
      cPrefix = 'menuTopActiveItem';
      item.isActivated = true;
    }
    output += '<TD nowrap class="' + cPrefix + '" id="' + TD_PREFIX + item.id
	+ '" onMouseOver="' + fPrefix + "On('" + item.id + "') "
	+ '" onMouseOut="' + fPrefix + "Out('" + item.id + "')"
	+ '" onClick="navigate(' + SQM + item.id + SQM + ')"'
	+ '>&nbsp;' + item.label + '&nbsp;</TD>'
	+ '</TD><TD class="menuTopSeparator"><IMG SRC="' + spacerImg + '"></TD>';
  }
  output += '<TD class="menuTopBlank"><IMG SRC="' + spacerImg + '"></TD><TD class="menuTopRightEdge"><IMG SRC="'
    + spacerImg + '"></TD></TR></TABLE></TD></TR>'
    + '<TR><TD><TABLE WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0" ALIGN="left"><TR><TD class="menuTopBotLeftCorner"><IMG SRC="'
    + spacerImg + '"></TD><TD class="menuTopBotTile"><IMG SRC="' + spacerImg + '"></TD><TD class="menuTopBotRightCorner"><IMG SRC="'
    + spacerImg + '"></TD></TR></TABLE></TD></TR></TABLE>';
  return output;
}

function subMenuTable(menuId) {
  var datecolwidth;
  var menu = getItem(menuId);
  var output = "";
  if (menu) {
    var items = menu.items;
    output += '<TABLE id="' + MT_PREFIX + menuId + '" WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0"><TR>' // Main table
	+ '<TD class="menuSubTopLeftCorner"><IMG SRC="' + spacerImg + '"></TD>'
	+ '<TD class="menuSubTopTile"><IMG SRC="' + spacerImg + '"></TD>'
	+ '<TD class="menuSubTopRightCorner"><IMG SRC="' + spacerImg + '"></TD></TR>';
    for (var i = 0; i < items.length; i++) {
      var item = items[i];
	  output += '<TR><TD class="menuSubLeftEdge"><IMG SRC="' + spacerImg
	    + '"></TD><TD nowrap class="menuSubItem" id="' + TD_PREFIX + item.id + '" ';
      if (item.isMenu) {
	    output += 'onMouseOver="menuOn(' + SQM + item.id + SQM
			+ ')" onMouseOut="menuOut(' +SQM + item.id + SQM
			+ ')" onClick="navigate(' + SQM + item.id + SQM + ')">'
			+ '<TABLE WIDTH="100%" BORDER="0" CELLPADDING="0" CELLSPACING="0" ALIGN="left">'
			+ '<TR><TD nowrap>&nbsp;' + item.label + '&nbsp;</TD><TD align="' + aRight
			+  '" valign="middle"><IMG ' + mPointerImgSrc + ' id="' + IMG_PREFIX 
			+ item.id + '" BORDER="0"></TD></TR></TABLE>';
	  } else {
	    output += 'onMouseOver="itemOn(' + SQM + item.id + SQM
			+ ')" onMouseOut="itemOut(' +SQM + item.id + SQM
			+ ')" onClick="navigate(' + SQM + item.id + SQM + ')">'
			+ '&nbsp;' + item.label + '&nbsp;';
	  }
	  output += '</TD><TD class="menuSubRightEdge"><IMG SRC="' + spacerImg
	  	+ '"></TD></TR>';
	}
	output += '<TR><TD class="menuSubBottomLeftCorner"><IMG SRC="' + spacerImg + '"></TD>'
	+ '<TD class="menuSubBottomTile"><IMG SRC="' + spacerImg + '"></TD>'
	+ '<TD class="menuSubBottomRightCorner"><IMG SRC="' + spacerImg + '"></TD></TR>'
	+ '</TABLE>';
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

function getPositionLeft (el) {
  var ol = el.offsetLeft;
  while ((el = el.offsetParent) != null)
	ol += el.offsetLeft;
  return ol;
}

function getPositionTop (el) {
  var ot = el.offsetTop;
  while((el = el.offsetParent) != null)
	ot += el.offsetTop;
  return ot;
}

function getHeight (el) {
  if (el) return el.offsetHeight;
  return 0;
}

function getWidth (el) {
  if (el) return el.offsetWidth;
  return 0;
}

function getWindowWidth() {
  if (getElementMethod == DOC_EL) return window.innerWidth;
  else if (getElementMethod == DOC_ALL) return document.body.clientWidth;
  else if (getElementMethod == DOC_LAY) return window.outerWidth;
  return null;
}

function showContent(id, state, lax, lay, content, newLayerClass) {
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
	if (lax) lElement.style.left = lax + "px";
	if (lay) lElement.style.top = lay + "px";
	lElement.style.visibility = (state) ? "visible" : "hidden";
    } else if (getElementMethod == DOC_ALL) {
	if (content) {
	    lElement.innerHTML = content;
	}
	if (lax) lElement.style.left = lax;
	if (lay) lElement.style.top = lay;
	lElement.style.visibility = (state) ? "visible" : "hidden";
    }
  }
}

function getElement(id) {
  if (getElementMethod == DOC_EL) return document.getElementById(id);
  else if (getElementMethod == DOC_ALL) return document.all[''+id];
  return null;
}

function pushArray(arObj, newElem) {
  if (pushImpl) {
    arObj.push(newElem);
  } else {
    arObj[arObj.length] = newElem;
  }
}
