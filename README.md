# Touhou Demo

[![Build Status](https://travis-ci.org/ice1000/TouhouDemo.svg?branch=master)](https://travis-ci.org/ice1000/TouhouDemo)
[![Build status](https://ci.appveyor.com/api/projects/status/qkxsrw0c7l0fke9k/branch/master?svg=true)](https://ci.appveyor.com/project/ice1000/touhoudemo/branch/master)

This is a demo project for [FriceEngine](https://github.com/icela/FriceEngine). <br/>
The [Lice](https://github.com/lice-lang/lice-tiny) programming language is used as the scripting engine.

## Run

First clone it, and copy your favorite music (mp3/wav) and rename it as `res/bgm.mp3`.

Then you can

```shell
$ ./gradlew run
```

Or if you have a gradle installed in your classpath:

```shell
$ gradle run
```

Or run the Java class `org.frice.th.Touhou` in your favorite IDE.

## Customizing this game

You can customize this game by editing the [configuration lice file](./lice/init.lice).
