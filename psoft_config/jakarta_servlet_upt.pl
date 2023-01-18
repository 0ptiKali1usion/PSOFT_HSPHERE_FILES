#!/usr/bin/perl
# Path to the cpanel home directory
$CP_HOME = "/hsphere/local/home/cpanel";

$HS_PROP_CONF = $CP_HOME."/shiva/psoft_config/hsphere.properties";
$MOD_JK_CONF = $CP_HOME."/apache/conf/mod_jk.conf";
$JAKARTA_SERVER_CONF = $CP_HOME."/jakarta/conf/server.xml";
$JAKARTA_WEB_CONF = $CP_HOME."/hsphere/WEB-INF/classes/psoft/hsphere/WEB-INF/web.xml";
$CP_INDEX_HTML = $CP_HOME."/shiva/shiva-templates/index.html";

$JAKARTA_CONF_CACHE = $CP_HOME."/jakarta/conf/Catalina/localhost";

$CP_URI_VAR = "CP_URI";
$UPLOADER_URI_VAR = "UPLOADER_URL";
$DOWNLOADER_URI_VAR = "DOWNLOAD_URI";

$STD_SERVLET_PATH = "/psoft/servlet";
$STD_CP_SERVLET = "psoft.hsphere.CP";
$STD_UPLOADER_SERVLET = "psoft.hsphere.Uploader";
$STD_DOWNLOADER_SERVLET = "psoft.hsphere.Downloader";

#################################### Site Studio #########################################
$SS_PROPERTIES = "/hsphere/shared/SiteStudio/psoft_config/masonry.properties";
$SS_DEMO_PROPERTIES = "/hsphere/shared/SiteStudio/demo/WEB-INF/classes/psoft_config/masonry.properties";
$MASONRY_URL = "MASONRY_URL";
$SURVEY_URL = "SURVEY_URL";
$COUNTER_SERVLET = "COWNTER_SERVLET";
$POLL_SERVLET = "POLL_SERVLET";
$GB_SERVLET = "GB_SERVLET";

$STD_SS_SERVLET_PATH = "/studio/servlet";
$STD_SS_DEMO_SERVLET_PATH = "/demo/servlet";
$STD_SS_MASONRY_SERVLET = "psoft.masonry.Builder";
$STD_SS_DEMO_MASONRY_SERVLET = "psoft.masonry.Builder";
$STD_SS_SURVEY_SERVLET = "psoft.customform.CustomForm";
$STD_SS_COUNTER_SERVLET = "psoft.counter.CounterService";
$STD_SS_POLL_SERVLET = "psoft.poll.PollServ";
$STD_SS_GB_SERVLET = "psoft.guestbook.GuestBookServ";
#################################### Site Studio #########################################

$TMP_EXT = ".tmp";
$ORG_EXT = ".ORG";

$errMes = "";

($nocheck, $forceCorrection) = 0;

foreach my $argv (@ARGV) {
  if ("--force" eq $argv) {
    $forceCorrection = 1;
  } elsif ("--nocheck" eq $argv) {
    $nocheck = 1;
  }
}

($servletPath, $cpServlet, $uploaderServlet, $downloaderServlet) = "";
parsePHConfig();
if ($nocheck) { printMessageIfError(); } else { exitIfError(2); }

($ssServletPath, $ssDemoServletPath,
	$ssMasonryServlet,
	$ssMasonryDemoServlet,
	$ssSurveyServlet,
	$ssCounterServlet,
	$ssPollServlet,
	$ssGbServlet) = "";
parseSSConfig($nocheck);
if ($nocheck) { printMessageIfError(); } else { exitIfError(3); }

$emptyValueCounter = 0;

unless ($forceCorrection) {
  resetIfStandard(\$servletPath, $STD_SERVLET_PATH) && $emptyValueCounter++ ;
  resetIfStandard(\$cpServlet, $STD_CP_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$uploaderServlet, $STD_UPLOADER_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$downloaderServlet, $STD_DOWNLOADER_SERVLET) && $emptyValueCounter++ ;
  
  resetIfStandard(\$ssServletPath, $STD_SS_SERVLET_PATH) && $emptyValueCounter++ ;
  resetIfStandard(\$ssDemoServletPath, $STD_SS_DEMO_SERVLET_PATH) && $emptyValueCounter++ ;
  resetIfStandard(\$ssMasonryServlet, $STD_SS_MASONRY_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$ssMasonryDemoServlet, $STD_SS_DEMO_MASONRY_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$ssSurveyServlet, $STD_SS_SURVEY_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$ssCounterServlet, $STD_SS_COUNTER_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$ssPollServlet, $STD_SS_POLL_SERVLET) && $emptyValueCounter++ ;
  resetIfStandard(\$ssGbServlet, $STD_SS_GB_SERVLET) && $emptyValueCounter++ ;
}

if ($emptyValueCounter < 12) {
  correctConfigs($nocheck);
  exitIfError(1);
  unlink <$JAKARTA_CONF_CACHE/*.xml>;
} else {
  print "There's nothing to re-configure. All values are standard.\n" unless ($nocheck);
}
exit 0;

sub parsePHConfig() {
  my ($uploaderPath, $downloaderPath);
  open (CFG_FILE, "< $HS_PROP_CONF");
  my @lines = <CFG_FILE>;
  close CFG_FILE;
  ($servletPath, $cpServlet) = splitUri(getCfgValue($CP_URI_VAR, @lines));
  ($uploaderPath, $uploaderServlet) = splitUri(getCfgValue($UPLOADER_URI_VAR, @lines));
  differentURLPathError($UPLOADER_URI_VAR, $uploaderPath, $CP_URI_VAR, $servletPath) if ($servletPath ne $uploaderPath);
  ($downloaderPath, $downloaderServlet) = splitUri(getCfgValue($DOWNLOADER_URI_VAR, @lines));
  differentURLPathError($DOWNLOADER_URI_VAR, $downloaderPath, $CP_URI_VAR, $servletPath) if ($servletPath ne $downloaderPath);
}

sub parseSSConfig($) {
  my $nocheck = $_[0];
  my $ssServletPath2;
  open (CFG_FILE, "< $SS_PROPERTIES");
  my @lines = <CFG_FILE>;
  close CFG_FILE;
  ($ssServletPath, $ssMasonryServlet) = splitUri(getCfgValue($MASONRY_URL, @lines));
  ($ssServletPath2, $ssSurveyServlet) = splitUri(getCfgValue($SURVEY_URL, @lines));
  differentURLPathError($SURVEY_URL, $ssServletPath2, $MASONRY_URL, $ssServletPath) if ($ssServletPath ne $ssServletPath2);
  ($ssServletPath2, $ssCounterServlet) = splitUri(getCfgValue($COUNTER_SERVLET, @lines));
  differentURLPathError($COUNTER_SERVLET, $ssServletPath2, $MASONRY_URL, $ssServletPath) if ($ssServletPath ne $ssServletPath2);
  ($ssServletPath2, $ssPollServlet) = splitUri(getCfgValue($POLL_SERVLET, @lines));
  differentURLPathError($POLL_SERVLET, $ssServletPath2, $MASONRY_URL, $ssServletPath) if ($ssServletPath ne $ssServletPath2);
  ($ssServletPath2, $ssGbServlet) = splitUri(getCfgValue($GB_SERVLET, @lines));
  differentURLPathError($GB_SERVLET, $ssServletPath2, $MASONRY_URL, $ssServletPath) if ($ssServletPath ne $ssServletPath2);

  open (CFG_FILE, "< $SS_DEMO_PROPERTIES");
  @lines = <CFG_FILE>;
  close CFG_FILE;
  ($ssDemoServletPath, $ssMasonryDemoServlet) = splitUri(getCfgValue($MASONRY_URL, @lines));

  if (($ssServletPath ne "") && ($ssServletPath eq $ssDemoServletPath)) {
    print STDERR "\nConflict: Context paths for the Site Studio and Site Studio Demo"
	." servlets are identical.\nThe default configuration for the Site"
	." Studio Demo servlet will be used.\n";
    unless ($nocheck) {
      print STDERR "Would you like to continue? [y/N] ";
      die "Process interrupted.\n" unless (readline(*STDIN) =~ m/^y(es)?$/i);
    }
    $ssDemoServletPath = $STD_SS_DEMO_SERVLET_PATH;
    $ssMasonryDemoServlet = $STD_SS_DEMO_MASONRY_SERVLET;
  }
}

sub getCfgValue() {
  my ($name, @lines) = @_;
    if (($name ne "") && ($#lines >= 0)) {
    foreach (@lines) {
      if (m/^\s*$name\s*=\s*(.*?)\s*$/) {
	return $1;
      }
    }
  }
  return "";
}

sub splitUri ($) {
  my $uri = $_[0];
  if (($uri ne "") && ($uri =~ m!^(https?:\/\/[^\/]+)?([\/\w.\-\$]+)\/([\w.\-\$]+)$!)) {
    return ($2, $3);
  }
  return ();
}

sub correctConfigs($) {
  my $nocheck = $_[0];
  my ($cfgFile, $isOrg, $isCorrected);
  
  if ($servletPath || $ssServletPath || $ssDemoServletPath) {
    ($cfgFile, $isOrg) = getOrgConfig($MOD_JK_CONF);
    $isCorrected = 0;
    open (IN_FILE, "< $cfgFile");
    open (OUT_FILE, "> $MOD_JK_CONF$TMP_EXT");
    while (<IN_FILE>) {
      if ($servletPath && s/^(\s*JkMount\s+)$STD_SERVLET_PATH(\/\*\s+testWorker)/$1$servletPath$2/
	|| $ssServletPath && s/^(\s*JkMount\s+)$STD_SS_SERVLET_PATH(\/\*\s+testWorker)/$1$ssServletPath$2/
	|| $ssDemoServletPath && s/^(\s*JkMount\s+)$STD_SS_DEMO_SERVLET_PATH(\/\*\s+testWorker)/$1$ssDemoServletPath$2/) {
	$isCorrected = 1;
      }
      print OUT_FILE $_;
    }
    close (OUT_FILE);
    close (IN_FILE);
    updateConfig($MOD_JK_CONF, $isCorrected, $isOrg, "", "", $nocheck);
    
    ($cfgFile, $isOrg) = getOrgConfig($JAKARTA_SERVER_CONF);
    $isCorrected = 0;
    open (IN_FILE, "< $cfgFile");
    open (OUT_FILE, "> $JAKARTA_SERVER_CONF$TMP_EXT");
    while (<IN_FILE>) {
      if ($servletPath && s/^(\s*<Context\s.*path=")$STD_SERVLET_PATH(\/?")/$1$servletPath$2/
	|| $ssServletPath && s/^(\s*<Context\s.*path=")$STD_SS_SERVLET_PATH(\/?")/$1$ssServletPath$2/
	|| $ssDemoServletPath && s/^(\s*<Context\s.*path=")$STD_SS_DEMO_SERVLET_PATH(\/?")/$1$ssDemoServletPath$2/) {
	$isCorrected = 1;
      }
      print OUT_FILE $_;
    }
    close (OUT_FILE);
    close (IN_FILE);
    updateConfig($JAKARTA_SERVER_CONF, $isCorrected, $isOrg, "", "", $nocheck);
  }
  
  if ($cpServlet || $uploaderServlet || $downloaderServlet
	|| $ssMasonryServlet || $ssSurveyServlet || $ssCounterServlet || $ssPollServlet
	|| $ssGbServlet || $ssMasonryDemoServlet) {
    ($cfgFile, $isOrg) = getOrgConfig($JAKARTA_WEB_CONF);
    $isCorrected = 0;
    open (IN_FILE, "< $cfgFile");
    open (OUT_FILE, "> $JAKARTA_WEB_CONF$TMP_EXT");
    while (<IN_FILE>) {
      if (m/(\s*)(<servlet-name>\s*|<url-pattern>\s*\/)($STD_CP_SERVLET|$STD_UPLOADER_SERVLET|$STD_DOWNLOADER_SERVLET|$STD_SS_MASONRY_SERVLET|$STD_SS_SURVEY_SERVLET|$STD_SS_COUNTER_SERVLET|$STD_SS_POLL_SERVLET|$STD_SS_GB_SERVLET|$STD_SS_DEMO_MASONRY_SERVLET)/) {
	my ($tag,$servlet) = ($2, $3);
	if (($cpServlet && ($STD_CP_SERVLET eq $servlet) && s/$tag$servlet/$tag$cpServlet/)
		|| ($uploaderServlet && ($STD_UPLOADER_SERVLET eq $servlet) && s/$tag$servlet/$tag$uploaderServlet/)
		|| ($downloaderServlet && ($STD_DOWNLOADER_SERVLET eq $servlet) && s/$tag$servlet/$tag$downloaderServlet/)
		|| ($ssMasonryServlet && ($STD_SS_MASONRY_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssMasonryServlet/)
		|| ($ssSurveyServlet && ($STD_SS_SURVEY_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssSurveyServlet/)
		|| ($ssCounterServlet && ($STD_SS_COUNTER_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssCounterServlet/)
		|| ($ssPollServlet && ($STD_SS_POLL_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssPollServlet/)
		|| ($ssGbServlet && ($STD_SS_GB_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssGbServlet/)
		|| ($ssMasonryDemoServlet && ($STD_SS_DEMO_MASONRY_SERVLET eq $servlet) && s/$tag$servlet/$tag$ssMasonryDemoServlet/)) {
	  $isCorrected = 1;
	}
      }
      print OUT_FILE $_;
    }
    close (OUT_FILE);
    close (IN_FILE);
    updateConfig($JAKARTA_WEB_CONF, $isCorrected, $isOrg, "", "", $nocheck);
  }

############## shiva-templates/index.html ##################
  if (($servletPath ne "") || ($cpServlet ne "")) {
    if ($servletPath eq "") {
      $servletPath = $STD_SERVLET_PATH;
    } elsif ($cpServlet eq "") {
      $cpServlet = $STD_CP_SERVLET;
    }
    ($cfgFile, $isOrg) = getOrgConfig($CP_INDEX_HTML);
    $isCorrected = 0;
    open (IN_FILE, "< $cfgFile");
    open (OUT_FILE, "> $CP_INDEX_HTML$TMP_EXT");
    while (<IN_FILE>) {
      if (s/(\sURL=)$STD_SERVLET_PATH\/$STD_CP_SERVLET/$1$servletPath\/$cpServlet/
		|| s/(<a href=")$STD_SERVLET_PATH\/$STD_CP_SERVLET(">)/$1$servletPath\/$cpServlet$2/) {
	$isCorrected = 1;
      }
      print OUT_FILE $_;
    }
    close (OUT_FILE);
    close (IN_FILE);
    updateConfig($CP_INDEX_HTML, $isCorrected, $isOrg, "cpanel:httpdcp", "640", $nocheck);
  }
}

sub getOrgConfig() {
  my ($name) = @_;
  return (-f $name.$ORG_EXT) ? ($name.$ORG_EXT, 1) : ($name, 0);
}

sub updateConfig($$$$$$) {
  my ($cfgFile, $isCorrected, $isOrg, $owner, $perm, $undying) = @_;
  if ($isCorrected) {
    unless ($owner) {
      $owner = "root:cpanel";
    }
    unless ($owner) {
      $perm = "640" 
    }
    copyFile($cfgFile, $cfgFile.$ORG_EXT, $owner, $perm, $undying) unless ($isOrg);
    copyFile($cfgFile.$TMP_EXT, $cfgFile, $owner, $perm, $undying);
  }
  unlink $cfgFile.$TMP_EXT;
}
  
sub copyFile($$$$$) {
  my ($src, $dst, $owner, $perm, $undying) = @_;
  if ($perm ne "") {
    $errMes = `install -m "$perm" "$src" "$dst"`;
  } else {
    $errMes = `cp "$src" "$dst"`;
  }
  $errMes = `chown "$owner" "$dst"` unless ($? && ($owner eq ""));
  if ($?) {
    if ($undying) { printMessageIfError(); } else { exitIfError(4); }
  } else {
    $errMes = "";
  }
}

sub printMessageIfError {
  if ($errMes ne "") {
    print $errMes."\n";
    $errMes = "";
  }
}

sub exitIfError($) {
  my $retCode = $_[0];
  if ($errMes ne "") {
    print $errMes."\n";
    exit $retCode;
  }
}

sub differentURLPathError($$$$) {
  my ($variable1, $value1, $variable2, $value2) = @_;
  $errMes .= "The URL path defined in \'$variable1\': [$value1] is different "
	."from the path which is defined in \'$variable2\': [$value2].\n";
}

sub resetIfStandard(\$$) {
  my ($newValue, $standardValue) = @_;
  if ($$newValue eq $standardValue) {
    $$newValue = "";
    return 1;
  } else {
    return 0;
  }
}
