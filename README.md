# gabien-common

Firstly.

This is released into the public domain.

No warranty is provided, implied or otherwise.

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

The code style isn't really documented as such, but it's basically the IDEA default with one change.

Which is that `new int[]{` or such is wrong, and should be `new int[] {`.

Pedantic, I know.

Otherwise, 4-spaces indentation, and do NOT use any "rearrange definitions" feature.

Sometimes there's a method to the definition order. (Sometimes there isn't, but still.)