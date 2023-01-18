/* The part to detect browsers correctly */

function createItem(frmName,fromlist,tolist,valchosen,recalc) {
var boxLength = document.forms[frmName].elements[tolist].length;
var selectedItem = document.forms[frmName].elements[fromlist].selectedIndex;
var selectedText = document.forms[frmName].elements[fromlist].options[selectedItem].text;
var selectedValue = document.forms[frmName].elements[fromlist].options[selectedItem].value;
var i;
var newoption
var isNew = true;
if (boxLength != 0) {
  if (document.forms[frmName].elements[tolist].options[0].value=="_NO_CHOSEN_") {
    document.forms[frmName].elements[tolist].options[0] = null;
    boxLength-=1;
  } else {
    for (i = 0; i < boxLength; i++) {
      thisitem = document.forms[frmName].elements[tolist].options[i].text;
      if (thisitem == selectedText) {
        isNew = false;
        break;
      }
    }
  }
}
if (isNew) {
  newoption = new Option(selectedText, selectedValue, true, true);
  document.forms[frmName].elements[tolist].options[boxLength] = newoption;
}
if (selectedItem != document.forms[frmName].elements[fromlist].length-1) {
  document.forms[frmName].elements[fromlist].selectedIndex = selectedItem+1;
} else {
  document.forms[frmName].elements[fromlist].selectedIndex = 0;
}
if (recalc) {
  getSelectedItems(frmName,tolist,valchosen);
}
}

function deleteItem(frmName,tolist,valchosen,tolist_empty_value,recalc) {
var boxLength = document.forms[frmName].elements[tolist].length;
for (i = 0; i < boxLength; i++) {
  if (document.forms[frmName].elements[tolist].options[i].selected) {
    document.forms[frmName].elements[tolist].options[i] = null;
    if (boxLength==1) {    //the last was removed
      newoption = new Option(tolist_empty_value, "_NO_CHOSEN_", true, true);
      document.forms[frmName].elements[tolist].options[0] = newoption;
    }
    break;
  }
}
document.forms[frmName].elements[tolist].selectedIndex=0;
if (recalc) {
  getSelectedItems(frmName,tolist,valchosen);
}
}

function getSelectedItems(frmName,tolist,valchosen) {
var selectedValues="";
var boxLength = document.forms[frmName].elements[tolist].length;

if (boxLength > 0 && document.forms[frmName].elements[tolist].options[0].value != "_NO_CHOSEN_") {
  for(var i = 0; i < document.forms[frmName].elements[tolist].length; i++) {
    selectedValues += (document.forms[frmName].elements[tolist].options[i].value + " ");
  }
  if (selectedValues!="") {
    selectedValues = selectedValues.substring(0, selectedValues.length-1);
  }
}
document.forms[frmName].elements[valchosen].value = selectedValues;
}

