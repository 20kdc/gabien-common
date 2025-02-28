# The History (aka _why_)

This has kind of just been dumped here because I keep ominously referring to bits of it and I should probably just put it here.

So the thing to keep in mind about GaBIEn is that it didn't used to be as formalized as it is now (and given how co-developed it is with R48 it still isn't that formal). It used to be a set of IntelliJ IDEA projects, and before that it was a set of NetBeans projects, running what I _guess_ you could call a predecessor to R48. The trope name for it would be Early Installment Weirdness, because this version of GaBIEn had *touchscreen buttons.*

Android made things weird, though. Circa 2017, the original plan to make everything work together nicely for the GitHub release (GaBIEn was _open-source_ in some form or another since its inception, as part of the application it was originally developed for) was essentially that you'd just... make `gabien-android` work properly in your IDE when you wanted to do a build and do it all from there.

Now, frankly, this model sucked, but adding a ton of per-project boilerplate to satisfy Android that would break whenever Google _or_ Gradle found a new shiny to chase _or_ when the JDK was updated... this was not ideal either.

_Apparently,_ I introduced a shell script in `gabien-android` (They used to be separate repos. This was a bad idea.) commit `a700281e490ba07cb79edbb9cf9d3fba3795a149` (November 15th, 2017).

_The core idea behind GaBIEn, then and now, is that you merge the application JAR (common + app code) with the backend JAR and package the resulting codebase._ So the shell script's job was to do that bundling & packaging, and it removed a lot of hassle.

Anyway, as for what happened to Gradle. In `gabien-app-r48` commit `e64cf5c165ce456f3754cd0145d1cc6e89ef121b` (`Wed Jun 23 01:28:11 2021 +0100`), the project was Gradle-based.

In R48 commit `a214be8b11e47359a733e39b6459fd08b8f18031` (`Sat Feb 12 15:07:10 2022 +0000`), it was Maven-based.

What happened? Well, I go on a hiatus, I update a few things, maybe I upgrade a distro or a JDK version or Gradle or whatever, and I come back to find my build broken. _And, let me be clear, the best way to drive me absolutely nuts is to break the build this way. If it's painful for me, and I know the codebase, then it's ten times as painful for anyone else who has no clue how stuff works around here. I've been on the receiving end of that often enough to know this for a fact._

(Keep in mind that not upgrading Gradle causes it to break with JDK versions due to ObjectWeb ASM being unable to read the newer classfiles.)

So while I don't remember exactly what triggered the change; I'm pretty sure I remember I Mavenized the entire project _immediately._

Ok, so that's explained the `pom.xml` files everywhere. But where did the Android release shell script go and how'd the Android SDK leave the equation? What's microMVN about?

New verse, same as the first. Someone broke the build _again._ This time it was because I flip-flopped on using `maven-toolchains-plugin`; I should have used `maven.compiler.executable` from the start but didn't know about it. I ultimately didn't use either and paid the price when I found I needed to keep my devices free of OpenJDK 21, lest it infect my build with errors like 'not compiling to Android period because of a build-tools bug.'

This lead to something of a chain reaction of version conflicts, because I try to look ahead on these things. Maven was going to require OpenJDK 21 eventually. _Maybe_ Android build-tools would update in time, but what if they invented some new Dalvik opcodes and dropped support for Android 2.1, or whatever? OpenJDK 21 has deprecated the Java 8 target. Where's that going? Why does it look increasingly as if the project is going to require a _separate_ JDK and JRE?

I needed to lock down as much of the build dependency versions onto OpenJDK 8 as possible. In theory it could have been another JDK version, but in practice everything worth using supports JDK 8 anyway.

If I can be busy for 5 years and reliably build the project when I get back, I win.