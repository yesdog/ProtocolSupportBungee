ProtocolSupportBungee
================

[![Build Status](https://travis-ci.org/yesdog/ProtocolSupportBungee.svg?branch=master)](https://travis-ci.org/yesdog/ProtocolSupportBungee)
[![Known Vulnerabilities](https://snyk.io/test/github/yesdog/ProtocolSupportBungee/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/yesdog/ProtocolSupportBungee?targetFile=build.gradle)
[![Chat](https://img.shields.io/badge/chat-on%20discord-7289da.svg)](https://discord.gg/x935y8p)

Support for 1.6, 1.5, 1.4.7, pe clients on BungeeCord<br>
This plugin is under development

Important notes:
* Only latest version of this plugin is supported
* This plugin can't be reloaded or loaded not at BungeeCord startup

__Yesdog Extras__
* New RakNet optimized for low quality connections
* Internal backpressure support
* RakNet metrics logging with [Prometheus](https://github.com/prometheus)

---

ProtocolSupportBungee is a passthrough protocol plugin, not a converter, so servers behind BungeeCord should also support those versions

Also servers behind Bungeecord should support https://github.com/ProtocolSupport/ProtocolSupport/wiki/Encapsulation-Protocol

The preferred setup is to put ProtocolSupport to all servers behind BungeeCord

---

Jenkins: http://build.true-games.org/job/ProtocolSupportBungee/

---

Licensed under the terms of GNU AGPLv3
