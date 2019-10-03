# LinuxProcessSizeDetector
This class fixes a bug in Linux's process size reporting when using Java's ZGC collector.

# Usage

First, make sure you are on 64 Bit Linux ([function for that](http://code.botcompany.de/1025550)).

Then, invoke `de.botcompany.LinuxProcessSizeDetector.rssFixedForZGC()` which gives you the estimated RSS value in bytes. You can also supply a PID to check another process.

Shared memory is not counted.
