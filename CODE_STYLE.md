# Code Style

Most of the code style amounts to:

1. _In Java code,_ 4-spaces. I prefer tabs, but I wrote the code like this a decade ago and changing it now would be highly messy. (We're lucky functions don't still use TitleCase. Recovering C# programmer.)
	* Conversely, in all other files, use tabs.
2. If using Eclipse, use a dedicated workspace for GaBIEn. This is important! If you don't, it will become hell to manage your configuration.
3. The `eclipse-style.xml` file is the canonical style. It is **not** strictly followed, as it makes some poor decisions.

## Eclipse Warning List

Here's a (not accounting for anything they added since last check...) warning list.

You should set this on **a dedicated workspace for GaBIEn projects, and all projects should use workspace settings.**

This warning list may be subject to change as the codebase becomes better-regulated.

* Code style: Ignore, except:
	* Non-static access to static member: Warning
	* Method with a constructor name: Warning
* Potential programming problems: Warning, except:
	* Possible accidental boolean assignment: Ignore
	* Boxing and unboxing conversions: Ignore
	* Unlikely argument type for method `equals()`: Info
	* Empty statement: Ignore
	* Unused object allocation: Ignore
	* 'switch' is missing 'default' case: Ignore
	* 'switch' case fall-through: Ignore
	* Potential resource leak: Ignore
	* Serializable class without serialVersionUID: Ignore
* Name shadowing and conflicts: Warning, except:
	* Local variable declaration hides another field or variable: Ignore
* Deprecated and restricted API: Warning, except:
	* (The two deprecated-in-deprecated signal checkboxes are off.)
	* Forbidden reference: Error
* Modules: Out of scope, but they're both Warning
* Unnecessary code: (This is a mess and so these instructions are a mess. If in doubt, start with everything on Warning and use whatever settings make the existing warnings go away)
	* Anything with the word 'Unnecessary' or 'Redundant' is Ignore
	* Anything with the word 'Unused' is Warning, except Unused type paramter which is Ignore
	* Value of local variable / lambda parameter is not used: Warning
	* Anything else is Ignore
* Generic types: Warning, except:
	* Redundant type arguments: Ignore
	* _ENABLE_ 'Ignore unavoidable generic type problems due to raw APIs'
* Annotations: Warnings, except:
	* 'Unused' status is not fully known (...): Ignore.
	* Enable 'Suppress optional errors with `@SuppressWarnings`'.
* Null analysis: Ignore, except:
	* Null pointer access: Error
	* Potential null pointer access: Warning
	* Redundant null check: Ignore
	* _ENABLE_ 'Include `assert` in null analysis'
	* _ENABLE_ 'Enable annotation-based null analysis'
	* Violation of null specification: Warning
	* Conflict between null annotations and null inference: Error
	* Unsafe `@NonNull` interpretation of free type variable from library: Warning
	* _ENABLE_ 'Use default annotations for null specifications'
	* _ENABLE_ 'Inherit null annotations'
	* _ENABLE_ 'Enable syntactic null analysis for fields'

