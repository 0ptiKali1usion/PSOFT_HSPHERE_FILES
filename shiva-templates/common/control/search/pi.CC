<!-- BEGIN ./search/pi.CC  -->
<tr>
   <td colspan="2" bgcolor="${HEADER_COLOR}"><call draw_header(lang.cc.info)></td>
</tr>
<tr>
   <td align="left" ><call draw_label(lang.cc.type)></td>
   <td align="left" ><call draw_label(pi.type)></td>
</tr>
<tr>
   <td align="left" ><call draw_label(lang.cc.name)></td>
   <td align="left" ><call draw_label(pi.name)></td>
</tr>
<tr>
   <td align="left" ><call draw_label(lang.cc.number)></td>
   <td align="left" ><call draw_label(pi.hNumber)></td>
</tr>
<tr>
   <td align="left" ><call draw_label(lang.cc.exp)></td>
   <td align="left" ><call draw_label(pi.exp)></td>
</tr>
<assign ccb_name_sh = pi.type>
<tr>
   <td align="left" ><call draw_label(lang.cc.cvv)></td>
   <td align="left" >
     <if pi.cvv_checked=="checked">
       <call draw_label(lang.cc.cvv_verified)>
     <else>
       <call draw_label(lang.cc.cvv_notverified)>
     </if>
   </td>
</tr>
<!--  END ./search/pi.CC  -->
