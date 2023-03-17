# Datum Specification

"Datum" (working name) is an S-expression format meant for quick implementation in various languages.

It's intended to be reasonably readable by R6RS readers, but not a strict subset (see reasoning below).

Datum decoding is described as a series of layers that starts with a byte stream. *However, if it is actually operating on a byte stream is left ambiguous.*

Therefore, "characters" may here refer to *bytes*, *Unicode codepoints*, or *UTF-16 elements*, and the specification is designed such that it is behaviourally identical regardless of which of these is chosen as the underlying representation for both input and output.

All references to numeric character values in this specification are unsigned. All references to specific characters are either as per ASCII or UTF-8.

The specification will never require UTF-8 to be decoded, but UTF-8 may need to be encoded, and if the result of the parsing process is not valid UTF-8, the input is formally considered invalid (but may be accepted regardless).

## Why not an R6RS subset?

*No rule in Datum will ever require the decoding of a UTF-8 sequence. Doing so adds unnecessary complexity, tends to discriminate against characters for arbitrary reasons, and above all can create Unicode-version-dependent behaviour due to how the previous two issues are caused.*

- For example, `🨂` is not a valid identifier in Java, the normal C compilers, and Python. There is no particular justifiable reason for this except that Unicode doesn't consider it a letter. If Unicode were to consider it a letter in future, the result could be effectively a version break in Java and Python (the normal C compilers do their own thing here).

- The behaviour of `gcc (Ubuntu 11.3.0-1ubuntu1~22.04) 11.3.0` appears to indicate GCC Unicode identifier compatibility operates by exclusion, i.e. `U+3FF80` is a valid identifier character. Java and Python, meanwhile, consider `U+3FF80` (Unassigned as of Unicode 15.0) invalid, but `U+10400` (Deseret: 𐐀) valid. A hypothetical future Unicode version could therefore enable valid Java and Python identifier characters that past versions refuse to accept for reasons that are, frankly, completely arbitrary. GCC's behaviour, on the other hand, could lead to code becoming invalid on a similar basis. This would be arguably worse if not for that people do not just arbitrarily use unassigned codepoints.

- GCC, Python and Java do consider the *private use areas* as invalid, for some unknown reason. (This rather defeats the purpose of the private use area.)

- ICU soversioning is a complete disaster.

## Data Model

The following kinds of values exist:

1. Symbols are arbitrary lists of characters (even empty lists).

2. Strings are arbitrary lists of characters (even empty lists).

3. The range and format of valid numbers is implementation-defined, with specific notes. What you're allowed to do here is restrained by Potential Identifier Differentiation, but the specific notes cover doubles and 64-bit integers.

4. The valid Special Identifiers and their meanings are implementation-defined, with specific notes. What you're allowed to do here is restrained by Potential Identifier Differentiation, but the specific notes cover 64-bit integers.

5. Lists can have any number of, or no, elements.

## Encoding

The encoding component converts a stream of characters to a potentially different stream of characters, tagged with *character classes*.

Later stages use character classes to control behaviour.

If, in this stage, a character is described as being written *indirectly,* then the class is always *content-class*. Otherwise, the class is determined by the Character Classes section.

The backslash, 92 `\`, begins an escape sequence.

The backslash may be followed by any character, in which case the result is that character, but made indirect. However, these specific characters have special meanings:

* 117 `x`: Can be followed by however many hexadecimal digits, of any case, terminated by a semicolon 59 `;`, which indicate a Unicode codepoint to be written indirectly into the character stream.

* 110 `n`: Newline, or 10, written indirectly.

* 114 `r`: Carriage return, or 13, written indirectly.

* 116 `t`: Tab, or 9, written indirectly.

This provides the fundamental escaping logic for the rest of Datum.

While these don't have any meaningful effect on the specification, there are two things worth noting:

1. It is impossible for the backslash to be written as a direct character under this system.

2. As UTF-8-extended bytes (128-255), UTF-16 surrogate pairs, and high codepoints (128+) are _always_ content-class, the direct and indirect distinction effectively does not apply to them. This is on purpose and is the only way the specification can remain interoperable with respect to implementations that do and do not respect UTF-8. In particular, `\` followed by a multi-byte-sequence creates a difference in handling of the direct flag, but as the results are always content-class, it does not actually matter.

## Character Classes

There are a number of character classes defined here, defined in terms of character values.

All characters within a class act behaviourally identical to each other as far as later stages are concerned.

- The characters from 0 through 32 inclusive, along with 127, but *not* 10, are *whitespace-class*.

- 10 is *newline-class*.

- 59 `;` is *line-comment-class*.

- 34 `"` is *string-class*.

- 39 `'` is *quote-class*.

- 40 `(` is *list-start-class*.

- 41 `)` is *list-end-class*.

- 41 `#` is *special-identifier-class*.

- 48 `0` through 57 `9` inclusive, or 45 `-`, are *numeric-start-class*.

- All other characters, *including all characters (UTF-8, UTF-16, Unicode, or otherwise) above 127,* are *content-class*.

There are also the following groups of classes, the contents of which may be differentiated by their actual class later:

- The union of *content-class*, *numeric-start-class*, and *special-identifier-class* make up the *potential-identifier-group*.

- The union of *whitespace-class* and *newline-class* make up the *non-printing-group*.

- The union of *quote-class*, *list-start-class*, and *list-end-class* make up the *alone-group*.

These groups only exist for ease of specification writing and their existence does not modify the character class process.

## Tokenization

### Whitespace

First, we must define *whitespace*. Whitespace is one of two sequences outside of a string:

1. Any *non-printing-group* character.

2. A *line-comment-class* character followed by an arbitrary sequence of characters ending with a *newline-class* character, which is considered included in the sequence.

Before reading a token, any whitespace is consumed.

### Token Types

There are three kinds of token at this stage:

1. *Potential identifiers,* sequences of *potential-identifier-group* characters. *The exact class of the first character is preserved for later differentiation.* Examples: `12.3`, `#t`, `hello`, `symbol->string`, `-8`.

2. *Strings,* *string-class*-delimited sequences of completely arbitrary characters. The only restriction is that in order to write characters 34 `"` or 92 `\`, they must be escaped.

3. Characters of the *alone-group* turn into specific token types for each of the group's classes.

It's worth noting that after this part of tokenization, potential identifiers are the only token to keep any trace of character classes, which are removed in the next stage.

Token types are used for similar purposes when necessary from this point forward.

### Chart

A chart labelled roughly by indirect values (be aware that these names aren't 1:1, check with the above):

```mermaid
stateDiagram-v2
    direction LR
    state whitespace_skipping {
        direction LR
        state loop <<choice>>
        [*] --> loop
        loop --> whitespace_class
        loop --> comment
        loop --> [*]
        whitespace_class --> loop
        comment --> loop
    }
    state comment {
        direction LR
        [*] --> ;
        ; --> not_newline
        not_newline --> not_newline
        not_newline --> newline
        ; --> newline
        newline --> [*]
    }
    [*] --> whitespace_skipping
    state potential_identifier {
        direction LR
        [*] --> potential_identifier_group
        potential_identifier_group --> potential_identifier_group
        potential_identifier_group --> [*]
    }
    whitespace_skipping --> potential_identifier
    potential_identifier --> [*]
    whitespace_skipping --> alone_group
    alone_group --> [*]
    whitespace_skipping --> string
    string --> [*]
    state string {
        direction LR
        [*] --> "_open
        "_open --> "_close
        "_open --> not_"
        not_" --> not_"
        not_" --> "_close
        "_close --> [*]
    }
```

## Potential Identifier Differentiation

### Basics

*Potential identifiers* as described above do not make up the basic primitive types such as booleans, integers, and floats.

As such, it falls to this part of the specification to explain how these are divided.

Thus, tokenization is extended to five kinds of token:

1. *Symbols,* such as `open`, `language_tok` -- the default handling of a *potential identifier* (see below).

2. *Numbers,* such as `123` -- a subset of potential identifiers described below.

3. *Special Identifiers,* such as `#t` and `#f` -- a subset of potential identifiers for reserved identifiers to be handled by the receiving parser.

4. The already-defined tokens, *strings* and the *alone-group* tokens.

Something to note is that the implementation of the differentiation is best placed within the tokenizer. These steps are divided here for ease of explanation.

### Differentiation

A *potential identifier's* first character's class (as preserved from earlier) completely defines the kind of potential identifier it is. This step is the last point at which character classes are seen.

Importantly, because it's preserved from earlier, escaping the first character guarantees as usual that the result will be *content-class*.

The classification of potential identifiers into their various subtypes works as follows:

* All potential identifiers that begin with a *numeric-start-class* character are *reserved number identifiers*.

* All potential identifiers that begin with a *special-identifier-class* character are *special identifiers*.

* Potential identifiers that don't fall into the above brackets are *symbols*.

Once this differentiation has been performed, *character classes no longer matter, only token types.*

This is important for various reasons, the most obvious being that `0` through `9` can't be discriminated by the character class system.

### Symbols

There is nothing particularly special to note about symbols. They can be thought of as essentially strings with a special flag set. However, it *may* be of use to optimize them in some way for fast lookup (perhaps interning them).

### Special Identifiers

Special identifiers are situational. The receiving parser must define them or forward the task in some way. Above all else, though, their purpose is to represent arbitrary singleton values that can't be arbitrarily defined by the user, so creating a mechanism to do so defeats the point.

However, the following special identifiers shall be considered *standardized* and should only be used for their intended purposes (though they need not necessarily be implemented):

* `#{}#`: This is actually converted into the empty symbol. This mainly exists to remove some of the error cases from writers.

* `#t` and `#T`: These express the boolean `true` value.

* `#f` and `#F`: These express the boolean `false` value.

* `#nil` or any case variation: This represents `null` or so forth. This may or may not be an alias for `()` depending on context.

### Numbers

The tokenization of Datum does not explicitly define the full set of *reserved number identifiers* that are considered *numbers*. However, some core principles must be followed:

* The reserved number identifier "-" alone is reclassified as a regular symbol.

* Any reserved number identifier that does not parse as a number must be considered a distinct kind of token, and preserved for potential compatibility workaround code to pick up on. That code may provide an error -- in particular it's not recommended to try and implement these as actual literals in a tree data model unless you really need to load _any_ valid file.

* Any reserved number identifier that does parse as a number can be appropriately forwarded as a *numeric token* (realistically, these would be *64-bit integer token* and *64-bit float token*). It is advised to keep the full textual content available, particularly in callback tokenizers where doing so is a zero-cost operation.

* Any contiguous sequence of the 10 ASCII decimal digits, which may or may not be preceded by `-`, that does not exceed the integer limits of the implementation, must be valid.

* Using `,`  in a "standard" (floating-point or integer) number is not valid. An implementation may choose to attempt to handle this anyway, but probably shouldn't. Using it as part of a non-standard kind of number not *expected* to be generally readable (Numeric vectors, possibly rational numbers but those would do better to use `/`) is theoretically fine, though an implementation should give some idea of the format of what such a number is, and the implementer may still wish to consider using alternative methods of expressing the number.

In addition, it is generally advised (given the repository this is in) that whatever number format is given follows the JSON specification, and can be parsed by Java `Long.parseLong` or `Double.parseDouble`.

## Grammar

The grammar of Datum is very simple.

Firstly, Datum does not follow JSON's de-facto "one file, one value" rule. Datum inherits from S-expressions the basic idea that a file is a continuous stream of S-expressions that may *choose* to have only one S-expression. This can, but does not (and ideally shouldn't) have to be, considered as a file being one big list.

At the level of this list, any token is valid except for the *end-list token* `)`, which is not valid. The "outer list" is implicit and not real, and there is no context to which list further input would append.

Most tokens are simply literal values of their respective kinds. There are two exceptions:

1. The *start-list token* `(` begins a list, identical to the outer list except for one aspect -- it accepts and ends with the *end-list token*. The entire list is considered to be a value.

2. The *quote token* `'` expects another value immediately afterwards (which may be, but doesn't have to be, a list). The transform is effectively: `'V` becomes `(quote V)`. Note that how this token is *used* is dependent on format (see the JSON Transformation below for an example of an arguably abnormal use of it that nonetheless improves the format).

*Due to this mode of operation, streaming parsers for Datum are nearly identical to callback tokenizers, with the exception that streaming parsers may report an error if an end-list token is applied when it matches to no start-list token, and that streaming parsers have to deal with the quote token by synthesizing the list.*

## Optional Implementation Requirements

An implementation may (but does not have to) make the following requirements, and all documents in the format must follow these to be valid:

1. That the input stream is valid UTF-8.

2. That all `\u`-escaped codepoints are allowed to be used in UTF-8.

3. That all strings and potential identifiers are valid UTF-8. (The first two points together guarantee this.)

An implementation may also require a lack of null (0) characters at any point, but the document is still valid in this case. This requirement upsets the ability to represent partially-ASCII byte streams as Unicode codepoints 0 through 255, but implementations in languages that would concern themselves with null bytes do not have UTF-8 decoding as a certainty in a minimalist implementation, and therefore it is up to the implementer to determine the appropriate path. *This is worth noting especially because this affects file formats layered onto Datum.*

## JSON Transformation

The full JSON transformation shows methods to fit different data models to Datum.

To transform JSON into Datum (and vice versa), the following rules should be followed:

1. Each JSON value is a single Datum value. This preserves the property that a JSON stream becomes a Datum stream and vice versa.

2. Booleans are `#f` and `#t`.

3. Numbers are represented exactly as they are in JSON.

4. Strings undergo a change in escaping, but due to the rules above this will never require _decoding UTF-8_ (as long as one is willing to allow that arbitrary UTF-8 directly into the output stream without directly escaping it).

5. When reversing the transformation, if a symbol is used where not expected, it should be converted to a string.

6. Null is translated into `#nil`.

7. Arrays are translated directly into a list, i.e. `[1, 2, 3]` becomes `(1 2 3)`.

This doesn't cover objects, as there's no reasonable *direct* translation.

A full JSON translation adds an additional rule:

Objects use the quote symbol to indicate the difference between them and lists, and use a list of key/value pairs (the pairs written without any framing). For example, `{"1a": 1, "b": "2"}` becomes `'("1a" 1 "b" "2")`.

This is not ambiguous, as symbols at the start of a list should not appear outside of objects in this transformation (and this is only to help with typing anyway). It is slightly unusual (as this expands to `(quote ("1a" 1 b "2"))`), but is easier to type than other forms.

Another way to represent this could have been, for instance, to have `(obj)` be the empty object, and `(obj "1a" 1 b 2)` be the example object given above. This wasn't chosen as using the quote character here made typing a lot easier.

## KeyValues Transformation

This will not be fully specified here, but exists more to illustrate a point about the use of the stream style.

KeyValues uses key/value pairs, but handles arrays by simply giving elements in them the same, repeated key.

This in mind, one can imagine the following KeyValues:

```
example {
    a b
}
```

becoming the following:

```
example (
    a "b"
)
```

There isn't really much more to say about this -- the translation only has the same problems as the JSON translation above.
