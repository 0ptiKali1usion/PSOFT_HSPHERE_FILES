/*  Function for adding */
function addOption(formname, attslistname, text, value) {
var nop = new Option();
nop.text = text;
nop.value = value;
nop.selected = true;
window.document.forms[formname].elements[attslistname].options[window.document.forms[formname].elements[attslistname].length] = nop;
}

function addAttachments(formname,attslistname,action,title,selectlabel) {
  var newatt=window.open("","newatt","resizable=no,scrollbars=yes,width=600,height=120,left=200,top=100,screenX=200,screenY=100");
  with (newatt.document) {
    open("text/html");
    write("<html><body onLoad='window.focus();'><title>"+title+"</title>");
    write("<script language='Javascript'>");
    write("function uploadFile() {");
    write("uploader=window.open('','uploader','resizable=yes,scrollbars=yes,width=120,height=120,left=420,top=150,screenX=420,screenY=150');");
    write("uploader.document.open(\"text/html\");");
    write("uploader.document.write(\"<html><\"+\"body onLoad='window.focus();opener.doUpload();'>Wait please ...</body></html>\");");
    write("uploader.document.close();");
    write("}");
    write("function doUpload() {");
    write("document.upload.submit();");
    write("}");
    write("function addOption( text, value) {");
    write("window.opener.addOption('"+formname+"','"+attslistname+"',value,text);");
    write("window.close();");
    write("}");
    write("</script>");
    write("<form name=\"upload\" action=\""+action+"\" method=\"POST\" ENCTYPE=\"multipart/form-data\" target=\"uploader\" accept-charset=\"UTF-8\">");
    write("<tr>");
    write("<td>"+selectlabel+"</td>");
    write("<td><input name=\"att\" type=\"file\" size=\"25\"></td>");
    write("<td><input type=\"hidden\" name=\"template_name\" value=\"attachmets_uploader.html\"></td>");
    write("<td><input type=\"submit\" name=\"upload\" value=\"Upload\" onClick=\"uploadFile();return true;\"></td>");
    write("</tr>");
    write("</form>");
    write("</body></html>");
    close();
  }
}
