bin_PROGRAMS = k_server
k_server_SOURCES = \
	main.cpp \
	cards.cpp cards.h \
	rules.cpp rules.h \
	controller.cpp controller.h \
	message.cpp message.h

k_server_LDADD = @PACKAGE_LIBS@

test:
	clear && g++ -g -O0 -D_TEST=1 rules.cpp message.cpp cards.cpp controller.cpp && valgrind --leak-check=full ./a.out

check:
	clang-check *.cpp && cppcheck *.cpp

