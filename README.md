# Praxis LIVE

This is the official source code repository for Praxis LIVE, an open-source hybrid visual environment for live creative coding. Praxis LIVE mixes intuitive real-time visual node editing, with a range of built-in components for audio, visual & data processing, together with an embedded compiler and editor for live-coding [Processing](https://processing.org/), Java and GLSL.

While including specific support for audio and video processing, Praxis LIVE is designed to support other forms of [cyber-physical coding](https://praxisintermedia.wordpress.com/2016/11/03/cyber-physical-coding-on-the-jvm/).

## Website & Downloads

See [www.praxislive.org](http://www.praxislive.org) for more information and downloads. There is an [online manual](http://praxis-live.readthedocs.io), as well as a range of [examples](https://github.com/praxis-live/examples) and [additional components](https://github.com/praxis-live/pxg).

## Support, bugs and feature requests

For general support or to discuss usage and development of Praxis LIVE, please sign up to the [mailing list](http://groups.google.com/d/forum/praxis-live). Please report bugs or make specfic feature requests on the [issue queue](https://github.com/praxis-live/support/issues) if possible.

## License

Praxis LIVE v3.x is released under the terms of the GPLv3 - see [license](LICENSE.md) for more details.

Praxis CORE v3.x (runtime) is also released under the terms of the GPLv3. The currently in development v4.x will see Praxis CORE relicensed under LGPL.

## Contribute

Non-code contributions are always welcomed - make example projects, help with documentation, assist new users, talk about Praxis LIVE, or just make something great with it!

An easy way to contribute to Praxis LIVE code is by making custom components for the [additional components](https://github.com/praxis-live/pxg) repository. Pull requests gratefully received! Eventually, these components may make it into the core distribution.

You can also get involved with building the main project itself ...

## Building the source code

To build Praxis LIVE you'll need to clone the two repositories that make up the overall project - the Praxis CORE runtime and the Praxis LIVE editor.

```
git clone https://github.com/praxis-live/praxis.git
git clone https://github.com/praxis-live/praxis-live.git

```

Make sure the two repository directories are in the same parent directory. There are two main branches in both repositories - `master` is for the current release and urgent bug fixes, `develop` is for development of the next release.

You'll also need the correct version of the NetBeans IDE - Praxis LIVE is built on top of the NetBeans platform. It is possible to build the project from another version of NetBeans or another IDE, but it must be built against the right version of the NetBeans platform - building inside that version of the IDE is recommended.

- NetBeans 8.2 : https://netbeans.org/downloads/

Open the Praxis (runtime) and Praxis LIVE (editor) projects in the IDE. The Praxis LIVE project should find the Praxis project dependency automatically. Build the Praxis project before the Praxis LIVE project.
