# gabien-common

Firstly.

    gabien-common - Cross-platform game and UI framework
    Written starting in 2016 by contributors (see CREDITS.txt)
    To the extent possible under law, the author(s) have dedicated all copyright and related and neighboring rights to this software to the public domain worldwide. This software is distributed without any warranty.
    You should have received a copy of the CC0 Public Domain Dedication along with this software. If not, see <http://creativecommons.org/publicdomain/zero/1.0/>.

Secondly, this is not very good. Any of it.

"gabien" was a framework I built in 2014 to make writing Java 
 applications a bit less painful.

Why Java? Portability, and a lack of reliance on external libraries.

Why? Because external libraries mean dealing with licenses.

(If C# had the APIs I needed portably and without external libraries, I would switch.
 For now, though, Java's the only one-file no legal madness solution I have.
 There's also the accelerated Java2D thing, which is useful.)

Instructions:

    1. Clone the common, JavaSE and target application repositories.
    2. Build the common, JavaSE and application repositories in that order.
    3. Run the application repository.

If using IDEA, run the idea generation on all three repositories seperately,
 then open IDEA on the application repository and fix up the dependencies.

Do something similar with your Eclipse workspace if you use Eclipse,
 only now you don't have to be careful as to open the application first,
 or repeat some of the steps for every application being edited.

(This is as simple a system as I could work out.)

## Notes on Code Style

The code style isn't really documented as such, but it's basically the IDEA default with two changes.

Firstly, `new int[]{` or such is wrong, and should be `new int[] {`.

Secondly, annotation wrapping is a per-class matter of stylistic choice - some annotations are deliberately grouped together.

Pedantic, I know.

Otherwise, 4-spaces indentation, and do NOT use any "rearrange definitions" feature.

Sometimes there's a method to the definition order. (Sometimes there isn't, but still.)