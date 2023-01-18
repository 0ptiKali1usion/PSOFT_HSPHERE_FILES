<!--  Begin ./control/eeman/pserver_iplist.js  -->
<script language="javascript">
<!-- return true; -  if value is valid -->
<!-- return false; -  if value is not valid -->
function iplist_validate(value) {
  var parts = value.split(".");
  var incorrect = '${lang.eeman.params.incorrect}';
  if (parts.length != 4) {
    alert(incorrect);
    return false;
  }
  for (var i=0; i < parts.length; i++) {
    var str = parts[i];
    var val = 0; 
    for (var j=0; j < str.length; j++) {
      if ((j == 0) && (str.length > 1) && (str.charAt(j) == '0')) {
        alert(incorrect);
        return false;      
      }
      if ((str.charAt(j) >= '0') && (str.charAt(j) <= '9')) {
        val = val*10 + parseInt(str.charAt(j)); 
      } else {
        alert(incorrect);
        return false;
      }
    }
    if ((val < 0) || (val > 255)) {
      alert(incorrect);
      return false;
    }
  }
  return true;
}
</script>
<!--  END ./control/eeman/pserver_iplist.js  -->