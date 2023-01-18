/***************************************************************************
 <copyright>
 * Copyright (c) 1999-2005 Positive Software Corporation
 </copyright>
 ***************************************************************************/
var MS_PREFIX = "a_";
var L_PREFIX = "l_";

var noteStates = {};

function switchNote(noteId) {
  if (noteId != null) {
    if (propertyExists(noteId, noteStates) && (noteStates[noteId] == "1")) {
      N_showLayer(L_PREFIX + noteId, false, 0, 0)
      noteStates[noteId] = "";
    } else {
      var mso = N_getElement(MS_PREFIX + noteId);
      N_showLayer(L_PREFIX + noteId, true, N_getPositionLeft(mso), N_getPositionBottom(mso));
      noteStates[noteId] = "1";
    }
  }
}

function closeNote(noteId) {
  if ((noteId != null) && propertyExists(noteId, noteStates) && (noteStates[noteId] == "1")) {
      N_showLayer(L_PREFIX + noteId, false, 0, 0)
      noteStates[noteId] = "";
  }
}

function quickNoteOn(noteId) {
  if (!(propertyExists(noteId, noteStates) && (noteStates[noteId] == "1"))) {
      var mso = N_getElement(MS_PREFIX + noteId);
      N_showLayer(L_PREFIX + noteId, true, N_getPositionLeft(mso), N_getPositionBottom(mso))
  }
}

function quickNoteOff(noteId) {
  if (!(propertyExists(noteId, noteStates) && (noteStates[noteId] == "1"))) {
      N_showLayer(L_PREFIX + noteId, false, 0, 0)
  }
}

var N_DOC_EL = 1;
var N_DOC_ALL = 2;
var N_DOC_LAY = 3;
var N_GET_ELEMENT_METHOD = N_detGetElementMethod();

function N_detGetElementMethod() {
  if (isNN && propertyExists("layers", document) && (document.layers != null)) return N_DOC_LAY;
  if (propertyExists("all", document) && (document.all != null)) return N_DOC_ALL;
  if (propertyExists("getElementById", document) && (document.getElementById != null)) return N_DOC_EL;
  return 0;
}

function N_getPositionLeft(el) {
  if (el == null) return 0;
  var ol = el.offsetLeft;
  while ((el = el.offsetParent) != null)
	ol += el.offsetLeft;
  return ol;
}

function N_getPositionBottom(el) {
  if (el == null) return 0;
  var ob = el.offsetTop + el.offsetHeight;
  while((el = el.offsetParent) != null)
	ob += el.offsetTop;
  return ob;
}

function N_getElement(id) {
  if (N_GET_ELEMENT_METHOD == N_DOC_EL) return document.getElementById(id);
  else if (N_GET_ELEMENT_METHOD == N_DOC_ALL) return document.all[''+id];
  else if (N_GET_ELEMENT_METHOD == N_DOC_LAY) return document.layers[''+id];
  return null;
}

function N_showLayer(id, state, lax, lay) {
  var lElement = N_getElement(id);
  if (lElement) {
    if (N_GET_ELEMENT_METHOD == N_DOC_EL) {
	if (lax >= 0) lElement.style.left = lax + "px";
	if (lay >= 0) lElement.style.top = lay + "px";
	lElement.style.visibility = state ? "visible" : "hidden";
    } else if (N_GET_ELEMENT_METHOD == N_DOC_ALL) {
	if (lax >= 0) lElement.style.left = lax;
	if (lay >= 0) lElement.style.top = lay;
	lElement.style.visibility = state ? "visible" : "hidden";
    } else if (N_GET_ELEMENT_METHOD == N_DOC_LAY) {
	if (lax >= 0) lElement.left = lax;
	if (lay >= 0) lElement.top = lay;
	lElement.visibility = state ? "show" : "hide";
    }
  }
}
