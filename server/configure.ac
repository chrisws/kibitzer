AC_INIT([k-server], 1.4)
AM_INIT_AUTOMAKE
AC_CONFIG_HEADERS([config.h])
AC_PROG_CXX

AC_CHECK_HEADERS(libwebsockets.h, [], [AC_MSG_ERROR([libwebsockets is not installed])])
PACKAGE_LIBS="${PACKAGE_LIBS} -lwebsockets"
CXXFLAGS="${CXXFLAGS} -Wall -Wextra -Wshadow -Wdouble-promotion -fno-rtti -fno-exceptions -std=c++14"

AC_SUBST(PACKAGE_LIBS)
AC_CONFIG_FILES(Makefile)
AC_OUTPUT

echo
echo ${TARGET}
echo "CXXFLAGS=${CXXFLAGS}"
echo "PACKAGE_LIBS=${PACKAGE_LIBS}"
echo
