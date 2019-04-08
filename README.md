ProtocolSupportBungee
================

[![Build Status](https://ci.codemc.org/job/yesdog/job/ProtocolSupportBungee/badge/icon)](https://ci.codemc.org/job/yesdog/job/ProtocolSupportBungee/)
[![Known Vulnerabilities](https://snyk.io/test/github/yesdog/ProtocolSupportBungee/badge.svg)](https://snyk.io/test/github/yesdog/ProtocolSupportBungee)
[![Chat](https://img.shields.io/badge/chat-on%20discord-7289da.svg)](https://discord.gg/x935y8p)

Support for PE clients on BungeeCord.

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

Download builds here: https://ci.codemc.org/job/yesdog/job/ProtocolSupportBungee/

---

Licensed under the terms of GNU AGPLv3
