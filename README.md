# gabien-common

## License

    gabien-common - Cross-platform game and UI framework
    Written starting in 2016 by contributors (see CREDITS.txt)
    To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
    You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

## Build Instructions (Command-Line)

Ensure Maven is installed and functional. At the present time, I'm using Apache Maven 3.8.4.

Run `mvn install` from this repository directory.

To avoid running unit tests, run `mvn install -DskipTests`.

You may need a `toolchains.xml` file setup in your Maven configuration directory - see `example-toolchains.xml`.

All projects will be automatically compiled (and possibly unit-tested).

## Build Instructions (Eclipse IDE)

Ensure Maven support is available. Use `File > Import...`.

Select `Maven > Existing Maven Projects`.

Select this repository directory.

When choosing which sub-projects to import, you may wish to deselect the parent project.

*You may need to get Eclipse to ignore the toolchain plugin.*

## History

"gabien" (Graphics And Basic Input ENgine was at least one of the acronyms) was originally developed as a part of an unofficial map editor for an old freeware game.

Mainly, it served as a cross-platform rendering backend, running on both JavaSE and Android.

It was later refactored into a separate library to be used across my personal projects.

Parts of that map editor were eventually integrated into R48 (`gabien-app-r48`), but they were different projects.

Since then, it's had peaks and declines, the worst relating to build system issues.

As Java continues to decline, I consider the Godot 3.x series to be the best target for further development, but I have severe hardware compatibility concerns about Godot 4.

With this in mind, I intend to keep this around, just in case.

## Notes on Code Style

The code style isn't really documented as such, but it's basically the IDEA default with two changes.

Firstly, `new int[]{` or such is wrong, and should be `new int[] {`.

Secondly, annotation wrapping is a per-class matter of stylistic choice - some annotations are deliberately grouped together.

Pedantic, I know.

Otherwise, 4-spaces indentation, and do NOT use any "rearrange definitions" feature.

Sometimes there's a method to the definition order. (Sometimes there isn't, but still.)

