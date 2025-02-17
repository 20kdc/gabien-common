# Builder Theorycrafting

So I thought it over, and essentially, this needs to be a _library_ that lets project build code do whatever it wants.

A good bit of code in it, namely the ZIP stuff, is going to be redundant code between it and micromvn. Sucks but micromvn wouldn't be what it is if things weren't done this way.

Plus there's going to be some kind of awkward function along the lines of "run tool in this directory with these args".

Tool here can be `umvn`, `javac`, `aapt`, `d8`/`r8`...

I'll be happy if I can run `gabien-build release-dev new-world` and get everything the old scripts did but portably.

For now, Android D8 is in the classpath. I'm *hoping* this'll work the way I want it to...