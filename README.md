# Kibitzer

Web cards game for playing "Warlords and Scumbags" and other games.

Kibitzer is made with svelte and libwebsockets

![kibitzer](https://user-images.githubusercontent.com/785121/87738018-c7367e00-c81f-11ea-87e2-50e8feb7cd66.png)


## User interface

Install the dependencies...

```bash
cd svelte-app
npm install
```

...then start [Rollup](https://rollupjs.org):

```bash
npm run dev
```

Navigate to [localhost:5000](http://localhost:5000). You should see kibitzer running. Edit a component file in `src`, save it, and reload the page to see your changes.

## Building and running in production mode

To create an optimised version of the app:

```bash
npm run build
```

You can run the newly built app with `npm run start`. This uses [sirv](https://github.com/lukeed/sirv), which is included in your package.json's `dependencies` so that the app will work when you deploy to platforms like [Heroku](https://heroku.com).

# Backend

## install dependencies

```
$ sudo apt install npm libssl-dev autogen automake g++ libwebsockets
```
## build

```
$ sh autogen.sh
$ ./configure
$ make
```

## run

```
$ ./run.sh
```

## Docker

```
$ docker build --tag kserver .
$ docker run kserver
$ docker exec -it 2839f2808486 /bin/bash
```

## systemd

```
$ sudo systemctl stop kibitzer
$ sudo systemctl start kibitzer
$ sudo systemctl status kibitzer
```
