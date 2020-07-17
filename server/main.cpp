//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#include <libwebsockets.h>
#include <string.h>
#include <signal.h>
#include "controller.h"
#include "message.h"

static int interrupted;

void sigint_handler(int /*sig*/) {
  interrupted = 1;
}

// one of these is created for each client connecting to us
struct Session {
  Session *_sess;
  Message _msg;
  lws *_wsi;
  int _last; /* the last message number we sent */
  int _id;
};

// one of these is created for each vhost our protocol is used with
struct HostContext {
  const lws_context *_context;
  const lws_vhost *_vhost;
  const lws_protocols *_protocol;
  Session *_sess; /* linked-list of live pss*/
  int _current; /* the current message number we are caching */
  Controller *_controller;
};

static const lws_http_mount mount = {
  /* .mount_next */   nullptr,   /* linked-list "next" */
  /* .mountpoint */   "/",    /* mountpoint URL */
  /* .origin */     "./public",  /* serve from dir */
  /* .def */      "index.html", /* default filename */
  /* .protocol */     nullptr,
  /* .cgienv */     nullptr,
  /* .extra_mimetypes */    nullptr,
  /* .interpret */    nullptr,
  /* .cgi_timeout */    0,
  /* .cache_max_age */    0,
  /* .auth_mask */    0,
  /* .cache_reusable */   0,
  /* .cache_revalidate */   0,
  /* .cache_intermediaries */ 0,
  /* .origin_protocol */    LWSMPRO_FILE, /* files in a dir */
  /* .mountpoint_len */   1,    /* char count */
  /* .basic_auth_login_file */  nullptr,
  /* unused */ nullptr
};

static void session_closed(Session *sess, HostContext *vhd) {
  sess->_msg.destroy();

  const Message message = vhd->_controller->destroySession(sess->_id);

  // remove our closing pss from the list of live pss
  lws_ll_fwd_remove(Session, _sess, sess, vhd->_sess);

  // tell the other players this one has left
  lws_start_foreach_llp(Session **, psess, vhd->_sess) {
    (*psess)->_msg = message;
    lws_callback_on_writable((*psess)->_wsi);
  }
  lws_end_foreach_llp(psess, _sess);
  vhd->_current++;
}

static int callback(lws *wsi, lws_callback_reasons reason, void *user, void *in, size_t len) {
  Session *sess = (Session *)user;
  HostContext *vhd = (HostContext *)
    lws_protocol_vh_priv_get(lws_get_vhost(wsi), lws_get_protocol(wsi));

  switch (reason) {
  case LWS_CALLBACK_PROTOCOL_INIT:
    vhd = (HostContext *)
      lws_protocol_vh_priv_zalloc(lws_get_vhost(wsi), lws_get_protocol(wsi), sizeof(HostContext));
    vhd->_context = lws_get_context(wsi);
    vhd->_protocol = lws_get_protocol(wsi);
    vhd->_vhost = lws_get_vhost(wsi);
    vhd->_controller = new Controller();
    break;

  case LWS_CALLBACK_PROTOCOL_DESTROY:
    delete vhd->_controller;
    vhd->_controller = nullptr;
    break;

  case LWS_CALLBACK_ESTABLISHED:
    // add ourselves to the list of live pss held in the vhd
    lws_ll_fwd_insert(sess, _sess, vhd->_sess);
    sess->_msg.create();
    sess->_wsi = wsi;
    sess->_last = vhd->_current;
    sess->_id = vhd->_controller->createSession();
    break;

  case LWS_CALLBACK_CLOSED:
    session_closed(sess, vhd);
    break;

  case LWS_CALLBACK_SERVER_WRITEABLE:
    if (sess->_msg._data != nullptr && sess->_last != vhd->_current) {
      // notice we allowed for LWS_PRE in the payload already
      int m = lws_write(wsi, sess->_msg._data + LWS_PRE, sess->_msg._len, LWS_WRITE_TEXT);
      if (m != sess->_msg._len) {
        lwsl_err("ERROR %d writing to ws\n", m);
        return -1;
      }
      sess->_last = vhd->_current;
    }
    break;

  case LWS_CALLBACK_RECEIVE:
    if (vhd->_controller->handle((unsigned char *)in, len, sess->_msg, sess->_id)) {
      if (sess->_msg.isBroadcast()) {
        // let everybody know we want to write something on them as soon as they are ready
        lws_start_foreach_llp(Session **, psess, vhd->_sess) {
          if (vhd->_controller->isSameRoom((*psess)->_id, sess->_id)) {
            if (*psess != sess) {
              (*psess)->_msg = vhd->_controller->redact((*psess)->_id, sess->_msg);
            }
            lws_callback_on_writable((*psess)->_wsi);
          }
        }
        lws_end_foreach_llp(psess, _sess);
      } else {
        lws_callback_on_writable(wsi);
      }
      vhd->_current++;
    } else {
      lwsl_user("OOM: dropping\n");
    }
    break;

  default:
    break;
  }

  return 0;
}

static struct lws_protocols protocols[] = {
  { "http", lws_callback_http_dummy, 0, 0, 0, 0, 0 },
  { "was-ws",
    callback,
    sizeof(Session),
    128, // rx buffer size
    0, nullptr, 0
  },
  { nullptr, nullptr, 0, 0, 0, 0, 0 } /* terminator */
};

static const lws_retry_bo_t retry = {
  .retry_ms_table = nullptr,
  .retry_ms_table_count = 0,
  .conceal_count = 0,
  .secs_since_valid_ping = 3,
  .secs_since_valid_hangup = 10,
  .jitter_percent = 0
};

int main(int argc, const char **argv) {
  lws_context_creation_info info;
  const char *p;
  int n = 0;
  int logs = LLL_USER | LLL_ERR | LLL_WARN | LLL_NOTICE;

  signal(SIGINT, sigint_handler);

  if ((p = lws_cmdline_option(argc, argv, "-d"))) {
    logs = atoi(p);
  }

  lws_set_log_level(logs, nullptr);
  lwsl_user("Kibitzer | visit http://localhost:7681 (-s = use TLS / https)\n");

  memset(&info, 0, sizeof(info));
  info.port = 7681;
  info.mounts = &mount;
  info.protocols = protocols;
  info.vhost_name = "localhost";
  info.options = LWS_SERVER_OPTION_HTTP_HEADERS_SECURITY_BEST_PRACTICES_ENFORCE;

  if (lws_cmdline_option(argc, argv, "-s")) {
    lwsl_user("Server using TLS\n");
    info.options |= LWS_SERVER_OPTION_DO_SSL_GLOBAL_INIT;
    info.ssl_cert_filepath = "/config/localhost-100y.cert";
    info.ssl_private_key_filepath = "/config/localhost-100y.key";
  }

  if (lws_cmdline_option(argc, argv, "-h")) {
    info.options |= LWS_SERVER_OPTION_VHOST_UPG_STRICT_HOST_CHECK;
  }

  if (lws_cmdline_option(argc, argv, "-v")) {
    info.retry_and_idle_policy = &retry;
  }

  lws_context *context = lws_create_context(&info);
  if (!context) {
    lwsl_err("lws init failed\n");
    return 1;
  }

  while (n >= 0 && !interrupted) {
    n = lws_service(context, 0);
  }

  lws_context_destroy(context);
  return 0;
}
