<!-- BEGIN ./billing/pi.CC  -->
<tr>
   <td colspan="3" bgcolor="${HEADER_COLOR}"><call draw_header(lang.cc.info)></td>
</tr>
<assign ccb_name_sh = pi.type>
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.cc.type)></td>
   <td align="left" ><call draw_label(ccb_name_sh)>
     <if toolbox.isDeletedBrand(ccb_name_sh)><call draw_label(lang.cc.outofdate)></if>
   </td>
</tr>
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.cc.name)></td>
   <td align="left" ><call draw_label(pi.name)></td>
</tr>
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.cc.number)></td>
   <td align="left" ><call draw_label(pi.hNumber)></td>
</tr>
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.cc.exp)></td>
   <td align="left" ><call draw_label(pi.exp)></td>
</tr>
<if ccb_name_sh == "SOLO" || ccb_name_sh == "SWITCH">
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.signup.card_issue_num)></td>
   <td align="left" ><call draw_label(pi.issue_no)></td>
</tr>
<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.signup.card_start_date)></td>
   <td align="left" ><call draw_label(pi.start_date)></td>
</tr>
</if>

<tr>
   <td align="left" >&nbsp;</td>
   <td align="left" ><call draw_label(lang.cc.cvv)></td>
   <td align="left" >
   <if pi.cvv_checked=="checked">
     <call draw_label(lang.cc.cvv_verified)>
   <else>
     <call draw_label(lang.cc.cvv_notverified)>
   </if>
   </td>
</tr>

<!--  END ./billing/pi.CC  -->
