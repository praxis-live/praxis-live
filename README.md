# PraxisLIVE

This is the official source code repository for PraxisLIVE - a hybrid visual live programming environment for creatives, programmers, students and tinkerers.

PraxisLIVE mixes intuitive real-time visual node editing, with a range of built-in components for audio, visual & data processing, together with an embedded compiler and editor for live-coding Java, [Processing](https://processing.org/) and GLSL.

PraxisLIVE is built around [PraxisCORE](https://www.praxislive.org/core/), a modular JVM runtime for cyberphysical programming, supporting real-time coding of real-time systems. With a distributed forest-of-actors architecture, runtime code changes and comprehensive introspection, PraxisCORE brings aspects of Erlang, Smalltalk and Extempore into the Java world ... a powerful platform for media processing, data visualisation, sensors, robotics, IoT, and lots more! 

## Website & Downloads

See [www.praxislive.org](http://www.praxislive.org) for more information and downloads. There is also an online manual at [https://docs.praxislive.org](https://docs.praxislive.org)

## Donations & Subscriptions

PraxisLIVE is free and open-source. Donations and subscriptions help keep the project going. If PraxisLIVE is useful to you, please consider starting a support subscription. We currently use DonorBox, supporting PayPal and card payments - [https://donorbox.org/praxislive](https://donorbox.org/praxislive)

## Support, bugs and feature requests

For general support or to discuss usage and development of PraxisLIVE, please check out the [Community](https://www.praxislive.org/community/) page for links to our mailing list, online chat, etc.

Please report bugs or make specific feature requests on the [issue queue](https://github.com/praxis-live/support/issues) if possible.

## License

PraxisLIVE v4.x is released under the terms of the GPLv3 - see [license](LICENSE.md) for more details.

PraxisCORE v4.x (runtime) is released under the terms of the LGPLv3, giving you control over how you license and distribute the projects you build.

## Development & Contributions

PraxisCORE and PraxisLIVE development is lead by [Neil C Smith](https://www.neilcsmith.net/). Contributions are always welcome.

An easy way to contribute to PraxisLIVE code is by making custom components for the [additional components](https://github.com/praxis-live/pxg) repository. Pull requests gratefully received! Eventually, these components may make it into the core distribution.

You can also get involved with building the main projects themselves - see information below.

**However, you don't have to contribute code to make a huge difference!** Make example projects, help with documentation, assist new users, talk about PraxisLIVE, or just make something amazing with it.

## Building the source code

To build PraxisLIVE you'll need to clone the two repositories that make up the overall project - the PraxisCORE runtime and the PraxisLIVE IDE.

```
git clone https://github.com/praxis-live/praxis.git
git clone https://github.com/praxis-live/praxis-live.git

```

Make sure the two repository directories are in the same parent directory. There are two main branches in both repositories - `master` is for the current release and urgent bug fixes, `develop` is for development of the next release.

PraxisLIVE is built on top of the [Apache NetBeans](https://netbeans.apache.org/) platform and IDE. As of v4.3 the build scripts will download Apache NetBeans dependencies, so it is possible to run the build from another IDE or the command line.

Before the first build, you must run the `init-platform` target on the PraxisLIVE build script. This requires an internet connection and may take a few minutes. The Apache NetBeans dependencies will be downloaded to `nbplatform` alongside the `praxis` and `praxis-live` folders. Subsequent builds will not require additional downloads unless you delete the `nbplatform` folder.

The PraxisCORE project must be built before the PraxisLIVE project.

If you have Ant installed you can run the following commands to build and run PraxisLIVE. From an IDE follow the equivalent steps. (NB. `clean` will also trigger `init-platform`).

```
ant -f praxis-live/build.xml init-platform
ant -f praxis/build.xml build
ant -f praxis-live/build.xml build
ant -f praxis-live/build.xml run
```

Ant targets also exist to build a Zip distribution, as well as a macOS application bundle and Windows installer with optional bundled JDK. See the properties documented in `nbproject/project.properties` which will need configuring locally.