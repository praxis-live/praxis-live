# PraxisLIVE

![PraxisLIVE v5 screenshot][screenshot]

This is the official source code repository for [PraxisLIVE](http://www.praxislive.org) -
a hybrid visual live programming IDE, rethinking general purpose and creative coding.

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

PraxisCORE v5.x (runtime) is released under the terms of the LGPLv3, and free to use in open-source
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

To build PraxisLIVE you'll need to clone the two repositories that make up the overall project -
the PraxisCORE runtime and the PraxisLIVE IDE. You will also need a JDK (11+) and both Ant and
Maven.

```
git clone https://github.com/praxis-live/praxiscore.git
git clone https://github.com/praxis-live/praxis-live.git
```

Make sure the two repository folders are in the same parent directory. If checking out a specific
version, make sure to use matching tags for both repositories - eg.

```
git -C praxiscore/ checkout v5.1.0
git -C praxis-live/ checkout v5.1.0
```

PraxisLIVE is built on top of the [Apache NetBeans](https://netbeans.apache.org/) platform and IDE.
The build scripts will download Apache NetBeans dependencies, so it is possible to run the build
from another IDE or the command line. These dependencies will be cached in an adjacent `nbplatform`
folder.

You will need both Ant and Maven available. PraxisCORE has switched across to Maven and also
publishes all modules to Maven Central. However, PraxisLIVE is still using Ant at this time due
to features required by the build. To build a zip of the IDE with embedded PraxisCORE, execute
the following steps from the parent folder (or similar steps inside your IDE).

```
mvn -f praxiscore/ package javadoc:aggregate
mvn -f praxiscore/praxiscore-bin/ package appassembler:assemble
ant -f praxis-live/build.xml build-zip
```

This will build a zip of the IDE, inside `praxis-live/dist`.

Alternatively, run directly using -

```
ant -f praxis-live/build.xml run
```

Ant targets also exist to build a macOS application bundle and Windows installer with optional
bundled JDK. See the properties documented in `nbproject/project.properties` which will need
configuring locally.

[screenshot]: https://www.praxislive.org/assets/PraxisLIVEv5-sm.jpg
