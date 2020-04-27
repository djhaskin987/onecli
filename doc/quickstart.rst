Quickstart
==========

For Callers
-----------

For users of commands who use OneCLI or which are CPC compliant.

If a command foo existed, and was CPC compliant, then the following might
be clean examples of what the caller could do with it.

Simple Command-Line Example
+++++++++++++++++++++++++++

In this example we see the basic idea::

    ./foo --enable-pearls --set-max-luster 125 polish

We see here that the ``pearls`` option is enabled (set to ``true``),
the ``max-luster`` option has been set to ``125``, and the command
``polish`` was given.

CLI with Environment Variables Example
++++++++++++++++++++++++++++++++++++++

Environment variables can also be used to set options::

    export FOO_FLAG_PEARLS=true
    export FOO_ITEM_MAX_LUSTER=125
    ./foo

This command is equivalent to the command in the above example in effect.
The same options have been set to the same values.

Options set via environment variables get overridden by command line::

    export FOO_FLAG_PEARLS=true
    ./foo --disable-pearls

In the above example, the pearls option is disabled and the procedure is
called which corresponds to having given no list of commands.

CLI, Env, and Config Files
++++++++++++++++++++++++++

TODO

For Developers
--------------

For Clojure Developers wishing to use OneCLI as a library.

The following If you wrote some clojure code that looked like this::

  (ns onecli.cli
    (:require
        [onecli.core :as core])
    (:gen-class))

  (defn -main [& args]
    (System/exit
      (core/go! {:program-name "wizard"
                 :args args
                 :env (System/getenv)})))

And you compiled it into an uberjar, like this::

    lein uberjar

Then you could call your function from the CLI, like this::

    java -jar target/uberjar/wizard-0.1.0-SNAPSHOT.jar help
