# LinuxProcessSizeDetector
This class fixes a bug in Linux's process size reporting when using Java's [ZGC](https://mail.openjdk.java.net/pipermail/zgc-dev/2019-October/000747.html) collector.

# Usage

First, make sure you are on 64 Bit Linux ([function for that](http://code.botcompany.de/1025550)).

Then, invoke `de.botcompany.LinuxProcessSizeDetector.rssFixedForZGC()` which gives you the estimated RSS value in bytes. You can also supply a PID to check another process.

Shared memory is not counted.

You can also run `de.botcompany.LinuxProcessSizeDetector` as a main program (without arguments or with a PID).

For finding process sizes on other platforms, use [OSHI](https://github.com/oshi/oshi).
