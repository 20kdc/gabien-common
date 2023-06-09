# TimeLogger

`TimeLogger` exists as a way to get profiling data out of GaBIEn.

The format and API is specifically intended for working out the details of multi-threaded applications first; in particular it was introduced to debug WSI issues during the big acceleration with VOPEKS. That in mind, there is a converter to the Chrome tracing JSON format. (That format is not directly output because I'm concerned it might bias performance to have a format that's too complicated; plus the format requires proper termination, while `TimeLogger` in theory does not.)

Conceptually, `TimeLogger` is made up of individual sources, which are independent. A Source has an "amount of activity", starting at 0. Every time the source is *opened*, this goes up by 1; every time it is *closed*, it goes down by 1. The amount of activity, in non-erroneous uses of `TimeLogger`, should only be 0 or 1. But if it does exceed those bounds, the converter/viewer should accept that and signify the overlap in some way.

Each individual source is meant to represent a specific activity. Sources are not necessarily per-thread, but they are represented that way in output formats to get viewing tools to act the right way. In particular, a Source may migrate between threads if it is representing, for example, a semaphore.

Importantly, Sources are ideally created during startup, and Source creation should never be on the critical per-frame path. Source creation will tend towards being a slower process so that actual submission of events is fast.

Ultimately, `TimeLogger` is not a hyper-optimized tracer, and calls to it should be avoided when not in use.

## Format

The `TimeLogger` format is a continuous stream of individual events.

Events have up to 4 fields. Their formats are as `DataOutputStream` writes them:

* Type (Byte)

* Source ID (Int)

* Time (Long) (`System.nanoTime()`)

* Text (UTF)

Only Type 0 events have the Text field.

### Type 0: New Source

This event defines a new source. Hence, it is the only event with a Text field.

### Type 1: Open

Opens a source.

### Type 2: Close

Closes a source.
