Command Procedure Call: an RFC
==============================

Overview
--------

Command Procedure Call (CPC) is a standard for command line utility
application behavior. If a command line utility is "CPC compliant",
it means that it complies with this standard.

CPC compliant programs accept *options* from a number of different sources.
These options are then aggregated into one single data structure, referred to
as the "option data structure". The user also specifies a list of commands,
each of which maps to a single procedure. This "procedure" can be thought of as
a method or funtion of some kind. The program decides which procedure to call
based on the given commands and calls the procedure, giving it the option data
structure as an argument. The schema of this option datastructure MUST be the
same for all procedures (all possible commands), though all options that are in
the structure's schema need not be used by all procedures, nor need all the
options in the structure have meaningful data (they could be set to "null" or
some other value denoting an absence of any value). The procedure then returns
to the program a data structure. This is the "return data structure".  This
return data structure MUST be serializable to valid JSON.  Then the program
serializes the structure to valid JSON and prints it to standard output. The
program MAY print arbitrary data to standard error during execution, but it
MUST print the JSON return data structure to standard output and ONLY that
structure. Then the program exits. The program MUST print a valid JSON document
to standard output every time it is called; it MUST NOT remain silent.

Program Return and Exit Codes
-----------------------------

Upon return, the program MUST return an exit code other than 0 to denote
that an error of some kind was encountered, if an error was encountered
during execution. The program MAY map different return values to different
errors. The program MUST NOT return a code other than 0 if the program ran
successfully. (Looking at YOU, robocopy.)

The Specification of Options
----------------------------

Each option found in the option data structure is logically a key-value pair,
with the key corresponding the the *option name* and the value
corresponding to the *option value*. The option name is a string of unicode
characters. The name of the option MUST be lower cased and MUST conform to this
regular expression::

    [a-z][a-z0-9-_]+

Note that the option name SHOULD be lower-cased.

The option value within an option data structure is of any type, though
the option value for any particular option name SHOULD always have the same  
concrete type. The option value type for any given option key SHOULD NOT be
polymorphic.

Each option is found by consulting specific *option sources*. Each option
source is consulted in turn. The actual value assigned to each option in the
option data structure which is ultimately given to the procedure is taken from
the last option source to contain that option ("last wins").

The option sources are listed below in the order that they are consulted:

  1. Configuration Files
  2. Environment Variables
  3. The Command Line

The last option source in the above list is also where the commands
that the program uses to determine what procedure to call are found.
It may then be thought of as the sole *command source*.

A Note on Program Names
-----------------------

The file name of the program MUST be named the same as the name of the program
itself. This *program name* will become important throughout this standard and
should be chosen carefully.

The Configuration Files
-----------------------

The first place the program will look for options will be configuration files.
The configuration files that the program will look in are:


  1. ``$APPDATA/<program-name>/config.json`` (even if on linux)
  2. ``$HOME/.<program-name>/config.json``
  3. ``$PWD/<program-name>.json``
  4. The files listed in the ``$<PROGRAM-NAME>_CONFIG_FILES`` environment
     variable, formatted in a comma separated list, if such an environment
     variable exists, examined in the order in which they appear

Each file is looked at in turn. The files are unicode text files, and are
*defined* to be encoded in UTF-8 unicode text encoding. The files are valid
JSON files that are MUST be deserializeable into valid option structures, even
if not all the options are specified in the files. The option structures are
then logically merged one on top of the other, starting with the first listed
configuration file and
merging the second and third on top of it, with the last configuration file
"winning".

The Environment Variables
-------------------------

Next, the program MUST examine the certain environment variables, logically
create an option structure based on their contents, and merge that option
structure onto the option structure (logically) that was created by examining
configuration files.

The environment variable equivalent of an option name or program name is the
same as the name in question except that all letters are capitalized and any
dashes in the name are replaced with underscores. These strings will be denoted
below as ``<OPTION_NAME>`` and ``<PROGRAM_NAME>``, respectively.

Any environment variable of the form ``<PROGRAM_NAME>_FLAG_<OPTION_NAME>`` will
be interpreted as a boolean value, and will set the option ``OPTION_NAME`` to
true in the option structure if the value of the environment variable is the
string ``true`` and false if the value of the environment variable is the
string ``false``.

Any environment variable of the form ``<PROGRAM_NAME>_ITEM_<OPTION_NAME>`` will
be interpreted in a option-specific manner, but SHOULD usually be interpreted
as an atomic value of type other than a boolean value. The string value of this
environment variable MAY be passed through some transform function which returns
a deserialized version of the value. This value may then be logically set to
as the value of the option key in the option structure.

Any environment variable of the form ``<PROGRAM_NAME>_LIST_<OPTION_NAME>`` will
be interpreted by splitting the string on some list-separating character (which
SHOULD usually be ``,``), passing each resulting string through some transform
as with the ``ITEM`` case, and using the result as the value of the option
in the option structure. The result SHOULD be roughly the same as if a JSON
list were deserialized and the resulting value used as the value of the option.

Any environment variable of the form ``<PROGRAM_NAME>_MAP_<OPTION_NAME>`` will
be interpreted by splitting the string on some list-separating character as
before, then splitting each resulting string in the list into a key-value pair
on some map-separating character (which SHOULD normally be ``=``) and storing
the list of key-value pairs as a map or dictionary. This dictionary will then
become the value associated with the option in the option structure (logically).

Even if the list-separating character for list separation is different than
``,``, it MUST be the same for the ``LIST`` case and for the ``MAP`` case,
whichever is chosen.

Finally, the string associated with any environment variable of the form
``<PROGRAM_NAME>_JSON_<OPTION_NAME>`` must be interpreted as a string of
unicode characters which constitutes valid JSON. This JSON will be deserialized
and the value of the deserialized JSON set logically as the value of the option
in the option structure.

If the option in question is in fact not of the type or schema described by
any environment variable which lists its option name, the behavior of the
program is undefined. Behavior in the face of type mismatches are beyond the
scope of this standard.

As previously stated, but to be very clear, options specified as above with
environment variables MUST override any option specified in any configuration
file.

Command Line
------------

Finally, the command line is examined. The command line is a bit of a special
case because it is examined, both for commands as an additional chance to
specify more options or override ones previously given by environment variables
or configuration files.

First, a program MAY define one or more "aliases", which are arguments given on
the command line that, if present, are logically substituted for strings.  They
may be thought of as a search-and-replace list of strings which if encountered
should be substituted for some program-defined replacement. As an example, the
string ``--help`` may be an *alias* for the argument string ``help``. If
aliases exist in the program, they MUST be applied before any other processing
of arguments, or it must be as if this is so.

After this step the arguments are processed.

Option names are to appear verbatim in arguments in place of the
``<option-name>`` string when it appears in the below description.

The order in which arguments which specify options appear matters. Arguments
occurring later in the arguments MUST override any value set for the same
option by an earlier argument.

As previously specified, any option given by command line MUST override any
value given for that option given by environment variable or configuration
file.

An argument of the form ``--enable-<option-name>`` MUST have the effect
of setting the option to the boolean value true, unless the option is not meant
to be of a boolean value type, in which case the behavior of the program is
undefined.

An argument of the form ``--disable-<option-name>`` MUST have the effect
of setting the option to the boolean value false, unless the option is not
meant to be of a boolean value type, in which case the behavior of the program
is undefined.

An argument of the form ``--reset-<option-name>`` MUST have the effect
of "resetting" the option, such that the behavior of the function was never
set at all, not even given a default value by the program. In programs with a
concept of ``null`` or ``nil``, this argument sets the option to the null
value.

A series of arguments of the form ``--set-<option-name> <value>`` MUST have the
effect of setting the value of ``<option-name>`` to ``<value>``. Any transform
functions specific to that option MAY be used by the program, resulting in the
string ``<value>`` becoming deserialized and the option set to the deserialized
value.

A series of arguments of the form ``--json-<option-name> <json-string>`` MUST
have the effect of treating the argument ``<json-string>`` as a string of
unicode characters, representing a valid JSON value, list or object. The JSON
is deserialized (or at least logically so), possibly in an option-specific
manner, and the option's value within the option data structure is set to this
result.

A series of arguments of the form ``--file-<option-name> <resource-loc>`` MUST
have the effect of opening some resource, reading UTF-8 encoded unicode characters
from that resource, deserializing them from JSON, and setting the option to the
resulting value of that JSON. The resource MUST be allowed to be a file, but
the program MAY also support single-string URLs, such as ``http``, ``ftp``, or
``ssh`` URLs.

All other arguments that are not of the forms above, after the aliases have
been applied, are assumed to be commands.

The CPC compliant program will then call the procedure that is associated
with the list of commands found on the command line. The "list of commands" here
means a list of commands with the commands found first in the arguments list
at the front of the list, moving in this order until the back of the list
which should contain the commands found last in the arguments list.
A unique procedure for each command list should be called with the option data
structure. If a list of commands was specified about which the program has no
information, an error SHOULD be returned.
