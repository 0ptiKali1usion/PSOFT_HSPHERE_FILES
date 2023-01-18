/***************************************************************************
 <copyright>
 * Copyright (c) 1999-2005 Positive Software Corporation
 </copyright>
 ***************************************************************************/

var global_state = new Array('','');
var global_val_message = " ('+'-valid, '-'-not valid yet, '!'-invalid/not validated) "; 
var val_err_message = "This field can't be validated dynamically !";
var ALERT_PREFIX = "Error(s): \n\n";
var curForm = null;
var __spCheck;

var V_ERROR = -1;
var V_REJECT = 0;
var V_ACCEPT = 1;
var V_INCOMPLETE = 2;

var IMG_VALID = 0;
var IMG_INCOMPETE = 1;
var IMG_ATTENTION = 2;

var MES_VALID = 0;
var MES_FAIL = 1;

var brAgent = navigator.userAgent.toLowerCase();
var brAppName = navigator.appName.toLowerCase();
var brMajorVer = parseInt(navigator.appVersion);
var brMinorVer = parseFloat(navigator.appVersion);
var isWinPlatform = (navigator.platform.indexOf("Win") == 0);
var brAgentVer = 0;

function setBrAgentVersion(keyWord) {
    var i = brAgent.indexOf(keyWord);
    if (i >= 0) brAgentVer = parseFloat(brAgent.substring(i + keyWord.length + 1));
}

var isNN	= (brAppName.indexOf("netscape")!=-1);
if (isNN) setBrAgentVersion("mozilla");
var isNN4	= (isNN && (brMajorVer == 4));
var isNN4up	= (isNN && (brMajorVer >= 4));
var isNN405next	= (isNN && (brMinorVer > 4.05));
var isNN47	= (isNN && (brMinorVer >= 4.7) && (brMinorVer < 4.8));
var isNN4Old	= (isNN && (brMajorVer < 5));
var isNN6up	= (isNN && (brMajorVer >= 5));
var isGecko	= (brAgent.indexOf("gecko") != -1);

var isIE	= ((brAgent.indexOf("msie") != -1));
if (isIE) setBrAgentVersion("msie");
var isIE3	= (isIE && (brMajorVer < 4));
var isIE4	= (isIE && (brMajorVer == 4));
var isIE4up	= (isIE && (brMajorVer >= 4));
var isIE5	= (isIE && (brMajorVer == 4) && (brAgentVer >= 5) && (brAgentVer < 5.5));
var isIE55	= (isIE && (brMajorVer == 4) && (brAgentVer >= 5.5) && (brAgentVer < 6));
var isIE5up	= (isIE && (brAgentVer >= 5));
var isIE55up	= (isIE && (brAgentVer >= 5.5));
var isIE6up	= (isIE && (brAgentVer >= 6));

var isOpera 	= (brAgent.indexOf("opera") != -1);
if (isOpera) setBrAgentVersion("opera");
var isOpera5up	= (isOpera && (brAgentVer >= 5));

var isKonqueror = (brAgent.indexOf("konqueror") != -1);
if (isKonqueror) setBrAgentVersion("konqueror");

var isHotJava = (brAgent.indexOf("hotjava") != -1);
var isHotJava3 = (isHotJava && (brMajorVer == 3));
var isHotJava3up = (isHotJava && (brMajorVer >= 3));

var jsVersion = 1.0;
if (isNN6up || isGecko) jsVersion = 1.5;
else if (isHotJava3up) jsVersion = 1.4;
else if (isIE5up || isNN405next || isOpera5up) jsVersion = 1.3;
else if (isNN4 || isIE4) jsVersion = 1.2;
else if (isNN || isOpera) jsVersion = 1.1;
var isJS14up = (jsVersion >= 1.4);

var propertyExists = (isJS14up) 
	? new Function ("property", "object", "return ((object != null) && (property in object));")
	: new Function("property", "object", "return ((object != null) && (object[property]));");

var browserDetectionComplete = true;

function checkRegexp(pattern_, str) {
  if (str == null)
      return false;
  var pattern = new RegExp(pattern_);
  return (pattern.test(str));
}

function checkOperand(operand) {
  return ((operand == V_REJECT) || (operand == V_ACCEPT) || (operand == V_INCOMPLETE));
}

function OR_(operand1, operand2) {
  if (!checkOperand(operand1) || !checkOperand(operand1)) return V_ERROR;
  if (operand1 == V_REJECT) {
    return operand2;
  } else if (operand1 == V_INCOMPLETE) {
    return (operand2 == V_ACCEPT) ? V_ACCEPT : V_INCOMPLETE;
  } else {
    return V_ACCEPT;
  }
}

function AND_(operand1, operand2) {
  if (!checkOperand(operand1) || !checkOperand(operand1)) return V_ERROR;
  if (operand1 == V_REJECT) {
    return V_REJECT;
  } else if (operand1 == V_INCOMPLETE) {
    return (operand2 == V_REJECT) ? V_REJECT : V_INCOMPLETE;
  } else {
    return operand2;
  }
}

function EQ_(operand1, operand2) {
  if ((operand1 == null) || (operand2 == null)) return V_INCOMPLETE;
  if (operand1 == operand2) return V_ACCEPT;
  var l1 = operand1.length;
  var l2 = operand2.length;
  if ((l1 == 0) || (l2 == 0)) return V_INCOMPLETE;
  if (l1 > l2) return (operand1.substr(0, l2) == operand2) ? V_INCOMPLETE : V_REJECT;
  if (l1 < l2) return (operand1 == operand2.substr(0, l1)) ? V_INCOMPLETE : V_REJECT;
  return V_REJECT;
}

function NOT_(operand) {
  return ((operand == V_INCOMPLETE) || (operand == V_ERROR)) ? operand
	: ((operand == V_REJECT) ? V_ACCEPT : V_REJECT);
}

function LT_(operand1, operand2) {
  return (operand1 < operand2) ? V_ACCEPT 
	: ((operand1 < 0) && (operand2 < 0) ? V_INCOMPLETE : V_REJECT);
}

function LE_(operand1, operand2) {
  return (operand1 <= operand2) ? V_ACCEPT 
	: ((operand1 < 0) && (operand2 < 0) ? V_INCOMPLETE : V_REJECT);
}

function GT_(operand1, operand2) {
  return (operand1 > operand2) ? V_ACCEPT 
	: ((operand1 >= 0) && (operand2 >= 0) ? V_INCOMPLETE : V_REJECT);
}

function GE_(operand1, operand2) {
  return (operand1 >= operand2) ? V_ACCEPT
	: ((operand1 >= 0) && (operand2 >= 0) ? V_INCOMPLETE : V_REJECT);
}

var __INTEGER_PATTERN = new Array(new Array('0','0','9','2'),new Array('0','+','','0','-','','0','0','9','2'),new Array('1','0','9','2'),new Array('fa',2,1));
var __FLOAT_PATTERN = new Array(new Array('0','+','','7','-','','7','0','9','3'),new Array('0','0','9','4'),new Array('0','0','9','6'),new Array('1',',','','2','.','','2','0','9','3','E','','5','e','','5'),new Array('1','0','9','4'),new Array('0','+','','1','-','','1','0','9','4'),new Array('1','0','9','6','E','','5','e','','5'),new Array('0','0','9','3'),new Array('fa',7,0));

function checkInteger_(operand) {
  return validate(__INTEGER_PATTERN, operand);
}

function checkFloat_(operand) {
  return validate(__FLOAT_PATTERN, operand);
}

function checkPattern(pattern) {
  if (pattern == null) return false;
  if (!propertyExists("valid_", pattern)) {
    var states = pattern.length - 1;
    var infoState = pattern[states];
    if (infoState.length == 3) {
      var initState = infoState[2];
      var finalState = infoState[1];
      if ((infoState[0] == 'fa') && (finalState < states) 
		&& (initState < states)) {
	pattern.valid_ = true;
	pattern.init_state = initState;
	pattern.final_state = finalState;
	return true;
      }
    }
    pattern.valid_ = false;
    return false;
  }
  return pattern.valid_;
}

function validate(pattern, input) {
  if (!checkPattern(pattern)) return V_ERROR;
  if (input == null) input = '';
  var state = pattern.init_state;
  var finalState = pattern.final_state;
  for (var i=0; i<input.length; i++) {
    var curChar = input.charAt(i);
    var stateTriads = pattern[state];
    var stLastIndex = stateTriads.length - 3;
    var sti = 1;
    state = -1;
    while(sti <= stLastIndex) {
      var lmChar = stateTriads[sti++];
      var rmChar = stateTriads[sti++];
      if (rmChar == '') {
	if (curChar == lmChar) {
	  state = stateTriads[sti];
	  break;
	}
      } else if ((curChar >= lmChar) && (curChar <= rmChar)) {
	  state = stateTriads[sti];
	  break;
      }
      sti++;
    }
    if (state < 0) return V_REJECT;
    if (state > finalState) return V_ERROR;
  }
  return (pattern[state][0] == V_ACCEPT) ? V_ACCEPT : V_INCOMPLETE;
}

function checkAnyField(field) {
  var fieldFunction = field.vf;
  var functionName;
  if ((fieldFunction != null) && (window.document.VFScriptExist)) {
    if (fieldFunction == '') {
      if (checkFN == null) return V_ERROR;
      fieldFunction = checkFN[field.name];
      if(fieldFunction == null) return V_ERROR;
      functionName = fieldFunction;
      fieldFunction += '(ThisField)';
    } else {
	var ind = fieldFunction.indexOf('(');
	if (ind > 0) {
	  functionName = fieldFunction.substr(0, ind);
	} else {
	  functionName = fieldFunction;
	}
    }
    if ((functionName != null) && (existingVF[functionName])) {
	ThisField = field;
	return eval(fieldFunction);
    }
  }
  return V_ERROR;
}                                                                    

function setStates(valid, fail) {
  global_state[MES_VALID] = valid;
  global_state[MES_FAIL] = fail;
}

function changeWndStatus(statusMessage) {
  if (!propertyExists("status", window) || (window.status != statusMessage)) {
    window.status = statusMessage;
  }
}

function changeImage(imgName, newImgId) {
  var img = window.document.images[imgName];
  if ((img != null) && (!propertyExists("state", img) || (img.state != newImgId))) {
    if (isIE && isWinPlatform
	&& (img.src.toLowerCase().lastIndexOf("spacer.gif") >= 0)) {
      img.style.filter = "progid:DXImageTransform.Microsoft.AlphaImageLoader(src='"
		+ status_images[newImgId] + "', sizingMethod='scale')";
      img.style.visibility = "visible";
      img.state = newImgId;
    } else {
    img.src = status_images[newImgId];
    img.state = newImgId;
  }
}
}

function showState(validity, imgName, insideField) {
  if (validity != V_ERROR) {
    var imgId = IMG_VALID;
    var messageId = MES_VALID;
    if (validity != V_ACCEPT) {
      imgId = insideField 
		? ((validity == V_REJECT) ? IMG_ATTENTION : IMG_INCOMPETE)
		: IMG_ATTENTION;
      messageId = MES_FAIL;
    }
    changeImage(imgName, imgId);
    changeWndStatus(global_state[messageId] + global_val_message);
  } else {
    changeImage(imgName, IMG_ATTENTION);
    changeWndStatus(val_err_message);
  }
}

function showOnFocus(field) {
  showState(checkAnyField(field), field.img_name, true);
}

function showOnBlur(field) {
  showState(checkAnyField(field), field.img_name, false);
}

function showOnKeyUp(field) {
  if (isNN47)
    showOnKeyN(field);
  else
    showState(checkAnyField(field), field.img_name, true);
}

function showOnKeyN(field) {
  field.blur();
  field.focus();
}

function nameToLabel(fieldName) {
  for (var i = 0; i < fieldName.length; i++) {
    if (fieldName.charAt(i) != '_')
      return fieldName.substr(i).toUpperCase();
  }
  return fieldName;
}

function checkForm(form, withAlert) {
  if (form == null) return false;
  return checkFForm(form, withAlert, true);
}

function checkFForm(form, withAlert, setFocus) {
  var isFormValid = true;
  var messages = "";
  var fields = form.elements;
  var failfield = null;
  var firstVF = true;
  if (fields != null) {     
    for (var i=0; i < fields.length; i++) {
      var cfield = fields[i];
      if (propertyExists("vf", cfield)) {
	var fieldValidity = checkAnyField(cfield);
	if (fieldValidity != V_ERROR) {
	  if (fieldValidity == V_ACCEPT) {
	    changeImage(cfield.img_name, IMG_VALID);
	  } else {
	    changeImage(cfield.img_name, IMG_ATTENTION);
	    isFormValid = false;
	    if (withAlert) {
	      var Label = cfield.label;
	      if ((Label == null) || (Label == '')) {
		Label = nameToLabel(cfield.name);
	      }
	      messages += Label + ": " + global_state[MES_FAIL] + "\n";
	      if (setFocus) failfield = cfield;
	    } else {
	      if (setFocus && firstVF && (getStringValue(cfield) == ""))
		failfield = cfield;
	    }
	    setFocus = false;
	  }
	}
	firstVF = false;
      }
    }
    if (messages) alert(ALERT_PREFIX + messages);
    if (failfield) failfield.focus();
  }
  return isFormValid;
}

function checkAllForms() {
  var isValid = true;
  for (var i = 0; i < document.forms.length; i++) {
    var cform = document.forms[i];
    if (propertyExists("wbv", cform)) {
      if (!checkFForm(cform, false, isValid)) isValid = false;
    }
  }
  return isValid;
}

function getStringValue(field) {
  if (field != null) {
    var fType = typeof (field);
    if (fType == 'string') return field;
    if (fType != 'undefined') {
      if (propertyExists("type", field) && (field.type == "select-one") && (field.selectedIndex >= 0)) {
	return field.options[field.selectedIndex].value;
      } else if ((propertyExists("length", field) || ((field.toString() != null) && (field.toString().indexOf("NodesCollection") >= 0)))
		&& (field[0] != null) && ((field[0].type == "radio") || isGecko)) {
	for (var i = 0; i < field.length; i++) {
	  if (field[i].checked == "1") return field[i].value;
	}
      } else if (propertyExists("value", field) && (field.value != null)) {
        return field.value;
      }
    }
  }
  return "";
}

function getBooleanValue(field) {
  return (field.type == "checkbox") ? (field.checked ? true : false)
		: ((getStringValue(field) != "") ? true : false );
}

function getAsString(value) {
  return value != null ? value : "";
}

function getAsInt(value) {
  return checkInteger_(value) ? parseInt(value) : Number.NaN;
}

function getAsFloat(value) {
  return checkFloat_(value) ? parseFloat(value) : Number.NaN;
}

function getAsBoolean(value) {
  return (value != null) && (value != "")
}

