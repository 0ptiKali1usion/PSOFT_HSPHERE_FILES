/* The part to detect browsers correctly */

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

var isNN        = (brAppName.indexOf("netscape")!=-1);
if (isNN) setBrAgentVersion("mozilla");
var isNN4       = (isNN && (brMajorVer == 4));
var isNN4up     = (isNN && (brMajorVer >= 4));
var isNN405next = (isNN && (brMinorVer > 4.05));
var isNN47      = (isNN && (brMinorVer >= 4.7) && (brMinorVer < 4.8));
var isNN4Old	= (isNN && (brMajorVer < 5));
var isNN6up     = (isNN && (brMajorVer >= 5));
var isGecko     = (brAgent.indexOf("gecko") != -1);

var isIE        = ((brAgent.indexOf("msie") != -1));
if (isIE) setBrAgentVersion("msie");
var isIE3       = (isIE && (brMajorVer < 4));
var isIE4       = (isIE && (brMajorVer == 4));
var isIE4up     = (isIE && (brMajorVer >= 4));
var isIE5       = (isIE && (brMajorVer == 4) && (brAgentVer >= 5) && (brAgentVer < 5.5));
var isIE55      = (isIE && (brMajorVer == 4) && (brAgentVer >= 5.5) && (brAgentVer < 6));
var isIE5up     = (isIE && (brAgentVer >= 5));
var isIE55up    = (isIE && (brAgentVer >= 5.5));
var isIE6up     = (isIE && (brAgentVer >= 6));

var isOpera     = (brAgent.indexOf("opera") != -1);
if (isOpera) setBrAgentVersion("opera");
var isOpera5up  = (isOpera && (brAgentVer >= 5));

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
