# SuzumeChunChun Web Chat App

**This app is not available anymore and is no longer maintained**.

---

**Totally Anonymous and Private Web Chat App**

You can use this app at ~~https://suzumechunchun.com~~

SuzumeChunChun has mainly these features below:


- No user account. No personal information. No stored messages. No history. In-browser message encryption!
- Without creating an account, you can start a chat room with anyone you want!
- No personal info is necessary to create a chat channel(chat room).
- No message is stored on the server. Messages are displayed only in your browser.
- Messages are encrypted in your browser. (If you delete your chat secret key, we even can't recover your message!
- Messages will vaporize approximately every 20 seconds. And Only latest 10 messages will be displayed.
- All data on the server is also encrypted or hashed. (All data except tables' primary keys and timestamps. But primary keys and timestamps have nothing to do with identifying who used or with messages you sent).
- When you end a chat channel, it's impossible to recover the channel or messages.
- It is open source, and the code is public.
- If you find any problem, please let me know on Twitter or GitHub.


General support, news, announcement: ~~https://twitter.com/sccwebchat~~


You can post general questions on the Twitter account.
But if you find any bugs or issues about the code, please post it on this GitHub issues page.


It is still in development, and there are bugs, issues and unfinished implementations.
If you think this app helps people to have better privacy online,
and if you could spare some of your valuable time,
please help me by telling me flaws, bugs and vulnerabilities in this app.


I'm still learning Scala, Akka, Play Framework, concurrent programming, well English, and many other things related to Scala.
That's mainly why right now the code may look rough and strange in many ways.
I'm very very sorry about that.


---


## How to Set Up Development Environment

This project is built on these frameworks:

- Play Framework2.8 (Scala)
- Vue2 (TypeScript)
- Jest (TypeScript Testing)
- Tailwind CSS (Styling)


This project is mostly built on Play Framework 2.8.
I didn't add anything special on the framework.
If you know how to use Play Framework, then I guess you shouldn't have much problem with the code.

Here is the Play's document: https://www.playframework.com/documentation/2.8.x/Home
(But I'm not sure Play's document is really helpful...)

All the TypeScript and CSS code is in `resources` directory.

If you would like to test and reuse the code, here is how to set up dev environment.


### system requirements

Please use IntelliJ for this project IDE.

It works both on Windows, Mac.

sbt >= 1.4.9.


#### tools

First, you need to install these tools below.


1. Yarn (npm): To build TypeScript and CSS.
1. Google Tinkey: To create encryption keyset.
1. Docker: For database.


##### Yarn

This project uses yarn1. 

https://classic.yarnpkg.com/en/docs/install/


##### Tinkey

https://github.com/google/tink/blob/master/docs/TINKEY.md

Homebrew install

```bash
brew tap google/tink https://github.com/google/tink
brew install tinkey
```


##### Docker

This project uses Docker for Postgres.

https://www.docker.com/get-started


---


### Building Dev Environment


#### Install all JavaScript/TypeScript modules

To install all modules for JavaScript/Typescript, please execute this on the terminal.

```
yarn install
```


#### Creating Tink keyset


Google Tinkey creates encryption key which is used to encrypt data in the database.

The command below will create `plaintext-keyset.json` in the project directory.

```bash
tinkey create-keyset --key-template AES128_GCM --out plaintext-keyset.json
```

This keyset should be used only in development. Plase do not use plaintext keyset in a production environment.


---

### Build and Run


#### Start Postgres

Before building and running the project, you should start database by

```shell
docker-compose up -d
```


Shutting down docker


```shell
docker-compose down
```

#### Build and Run Scala code

start **sbt shell** (not Terminal), and then

```shell
run
```

This will also watch the file changes.


#### Transpile TypeScript code

Start **Terminal**, and then

```shell
yarn dev
```

You can also use `yarn watch` to transpile and watch the file changes.


---


### Testing


#### Play Testing

In **sbt shell**, the command below will run Scala tests.

```
test
```

#### TypeScript Testing

In **Terminal**, the command below will run JavaScript/TypeScript tests.

```
yarn test
```


