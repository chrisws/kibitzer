#/bin/bash

rm -f acinclude.m4
rm -rf autom4te.cache aclocal.m4

aclocal
autoheader
autoconf
automake --add-missing --foreign
