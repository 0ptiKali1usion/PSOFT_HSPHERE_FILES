<!--  Begin ./control/eeman/pserver_domainslist.js  -->
<script language="javascript">
  <!-- return true; -  if value is valid -->
  <!-- return false; -  if value is not valid -->
function domainslist_validate(value) {
  var index = value.indexOf(".");
  var incorrect = '${lang.eeman.params.incorrect}';
  if (index == -1) {
    alert(incorrect);
    return false;
  } else {
    if ((index > 0) && (index < value.length-1)) {
      return true;
    } else {
      alert(incorrect);
      return false;
    }
  }
}
</script>
<!--  END ./control/eeman/pserver_domainslist.js  -->