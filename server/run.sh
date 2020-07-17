#!/bin/bash

export LD_LIBRARY_PATH=/lib:/usr/lib:/usr/local/lib

make -s && valgrind --leak-check=full ./k_server
