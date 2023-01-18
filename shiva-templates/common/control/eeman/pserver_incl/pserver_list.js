<!--  Begin ./control/eeman/pserver_list.js  -->
<script language="javascript">
function trim(value) {
  var temp = value;
  var obj = /^(\s*)([\W\w]*)(\b\s*$)/;
  if (obj.test(temp)) { 
    temp = temp.replace(obj, '$2'); 
  }
  var obj = / +/g;
  temp = temp.replace(obj, " ");
  if (temp == " ") { temp = ""; }  
  return temp;
}
function selectItem(controlName) {
  if (curForm != null) { 
    var index = curForm.elements[controlName].selectedIndex; 
    var val = curForm.elements[controlName].options[index].text;
    curForm.elements[controlName].options[index].value = escape(val); 
    curForm.elements[controlName+'_val'].value = val;
  }
}
function checkNotValid(controlName, controlType, paramSpaces) {
  var found = false; 
  var exist = '${lang.eeman.params.exist}';  
  var contr = curForm.elements[controlName];
  var valuetocheck = trim(curForm.elements[controlName + "_val"].value); 
  for(i = 0; i < contr.length; i++ ) {
    var contrvalue = contr.item(i).value;
    if (contrvalue == valuetocheck) {
      found = true;
    }
  }
  if (found) {
    alert(exist + controlName);
    return true;
  } else {
    var empty = checkEmptyOrSpace(controlName, paramSpaces);
    if (empty) {
      return true; 
    } else {
      var valid = true; 
      if ((controlType == "emailslist") || (controlType == "domainslist") || 
          (controlType == "iplist")) {   
        eval("valid =" + controlType + "_validate('" + valuetocheck + "')" );
      }
      return !valid;
    }
  }
}
function checkEmptyOrSpace(controlName, paramSpaces) {
  var valuetocheck = trim(curForm.elements[controlName + "_val"].value);
  var emptyvalue = '${lang.eeman.params.emptyvalue}';
  var valuefor = '${lang.eeman.params.valuefor}';
  var nospaces = '${lang.eeman.params.nospaces}';
  if (!paramSpaces) {
    var index = valuetocheck.indexOf(" ");
    if ((index > 0) && (index < valuetocheck.length-1)) {
      alert(valuefor + controlName + " " + nospaces);
      return true;
    }
  }
  if (valuetocheck == '') {
    alert(emptyvalue + controlName);
    return true;
  } else {
    return false;
  }  
}
</script>
<!--  END ./control/eeman/pserver_list.js  -->