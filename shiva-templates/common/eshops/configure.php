<?

  define('DIR_FS_ADMIN', $DOCUMENT_ROOT.'/admin/');
  define('DIR_FS_CATALOG', $DOCUMENT_ROOT.'/catalog/');
  define('DIR_WS_HTTP_CATALOG', '/catalog/');
  define('DIR_WS_ADMIN', '/admin/');
  define('DIR_WS_CATALOG', '/catalog/');
  define('HTTP_CATALOG_SERVER', 'http://${domain}');
  define('HTTP_COOKIE_DOMAIN', 'user1.test51.psoft');
  define('HTTP_COOKIE_PATH', '/catalog/');
  define('HTTP_SERVER', 'http://${domain}');
  define('HTTP_SERVER', 'http://${domain}');
  <if hosting.server == "IIS" >
  define('DIR_FS_DOCUMENT_ROOT', $DOCUMENT_ROOT . '../'); 
  define('DIR_FS_LOGS', '../admin/logs');<else>
  define('DIR_FS_DOCUMENT_ROOT', '${path}');
  define('DIR_FS_LOGS', '${log_dir}');</if>
  <if hosting.getChild("ssl")>define('HTTPS_SERVER', 'https://${domain}');
  define('ENABLE_SSL', 1);
  define('HTTPS_COOKIE_PATH', '/catalog/');
  define('DIR_WS_HTTPS_CATALOG', '/catalog/');
  <else><assign sharedssl = hosting.getChild("sharedssl")> <if sharedssl>define('HTTPS_SERVER', 'https://${sharedssl.name}');
  define('ENABLE_SSL', 1);
  define('HTTPS_COOKIE_PATH', '/catalog/');
  define('DIR_WS_HTTPS_CATALOG', '/catalog/');<else>define('ENABLE_SSL', 0);</if></if>
// define our database connection
  define('DB_SERVER', '${mysql_host}');
  define('DB_SERVER_USERNAME', '${mysql_user}');
  define('DB_SERVER_PASSWORD', '${mysql_password}');
  define('DB_DATABASE', '${mysql_db}');
  define('CONFIGURE_STATUS_COMPLETED', 1);
  define('USE_PCONNECT', 'false');
  define('STORE_SESSIONS', 'mysql');
?>
