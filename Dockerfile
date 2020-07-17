FROM ubuntu

EXPOSE 7681

COPY libs/libwebsockets.so.16 /usr/lib
COPY libs/libssl.so.1.1 /usr/lib
COPY libs/libcrypto.so.1.1 /usr/lib
COPY server/k_server /bin/
COPY server/localhost-100y* /config/
COPY public/favicon-32x32.png /public/
COPY public/favicon.png /public/
COPY public/global.css /public/
COPY public/index.html /public/
COPY public/images /public/images/
COPY public/build /public/build/

CMD ["/bin/k_server", "-s"]

