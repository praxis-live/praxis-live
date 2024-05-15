# PraxisLIVE

![PraxisLIVE v5 screenshot][screenshot]

[PraxisLIVE](http://www.praxislive.org) - a hybrid visual live programming IDE, rethinking general
purpose and creative coding.

PraxisLIVE is built around [PraxisCORE](https://www.praxislive.org/core/), a modular JVM runtime
for cyberphysical programming, supporting real-time coding of real-time systems. With a distributed
forest-of-actors architecture, runtime code changes and comprehensive introspection, PraxisCORE brings
aspects of Erlang, Smalltalk and Extempore into the Java world ... a powerful platform for media
processing, data visualisation, sensors, robotics, IoT, and lots more!

PraxisLIVE and PraxisCORE are open-source projects developed by Neil C Smith, and now supported
by [Codelerity Ltd.](https://www.codelerity.com).

## Website & Downloads

See [www.praxislive.org](http://www.praxislive.org) for more information and downloads.

There is also an online manual at [https://docs.praxislive.org](https://docs.praxislive.org)

## Support, bugs and feature requests

For general support or to discuss usage and development of PraxisLIVE, please check out the
[Community](https://www.praxislive.org/community/) page for links to our mailing list, online chat, etc.

Please report bugs or make specific feature requests on the
[issue queue](https://github.com/praxis-live/support/issues) if possible.

## License

PraxisCORE v6.x (runtime) is released under the terms of the LGPLv3, and free to use in open-source
and commercial projects.

The PraxisLIVE IDE is released under the terms of the GPLv3 - see [license](LICENSE.md) for
more details.


## Development & Contributions

Contributions are always welcome.

An easy way to contribute to PraxisLIVE code is by making custom components for the
[additional components](https://github.com/praxis-live/components) repository. Pull requests
gratefully received! Eventually, these components may make it into the core distribution.

You can also get involved with building the main projects themselves - see information below.

**However, you don't have to contribute code to make a huge difference.** Make examples,
help with documentation, assist new users, spread the word, make amazing things!

## Building the source code

To build PraxisLIVE you'll need a JDK (21+). The build uses Maven. The source includes the Maven
wrapper script, which will automatically download and execute the right version of Maven for you.

If you need to checkout the PraxisLIVE source code, use -

```
git clone https://github.com/praxis-live/praxis-live.git
```

If you're building a released version of PraxisLIVE, the build will automatically download the
PraxisCORE dependencies. You can skip ahead to the PraxisLIVE build. If you're building a
development (snapshot) version you will need to download, build and install the matching version
of PraxisCORE.

### Building PraxisCORE

To build PraxisCORE, first checkout the PraxisCORE sources.

```
git clone https://github.com/praxis-live/praxiscore.git
```

Then from inside the `praxiscore` directory, execute -

```
./mvnw clean install
```

Installation of snapshot versions of PraxisCORE is required for them to be picked up by the
PraxisLIVE IDE build.

### Building PraxisLIVE IDE

To build the PraxisLIVE IDE, execute the build from within the `praxis-live` directory.

```
./mvnw clean package
```

A zip of the IDE will be created inside `application/target/`. You can also test the application
via Maven using -

```
./mvnw -f application/ nbm:run-platform
```

PraxisLIVE is built on top of the [Apache NetBeans](https://netbeans.apache.org/) platform and IDE.
The build scripts will download Apache NetBeans dependencies. The first build might take more time
as it downloads and caches the necessary dependencies.

[screenshot]: https://www.praxislive.org/assets/PraxisLIVEv5-sm.jpg
