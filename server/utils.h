//
// Kibitzer web-sockets server
//
// Copyright(C) 2020 Chris Warren-Smith.
//

#pragma once

#if defined(_TEST)
 #include <stdio.h>
 #define log printf
#else
 #include <libwebsockets.h>
 #define log lwsl_user
#endif
