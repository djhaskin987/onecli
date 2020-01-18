Quickstart
==========

If you wrote some clojure code that looked like this::

  (ns onecli.cli
    (:require
        [onecli.core :as core])
    (:gen-class))

  (defn summon
    "

  (defn print-help-screen
    "
    Welcome to the main help screen!

    All CLI commands in OneCLI are bound to a specific clojure function.

    All help screens in OneCLI commands are taken from the doc strings of
    their respective functions.

    This is the main function for the root command.

    Available subcommands:

    - `banish`
    - `summon`
    - `disappear`

    Global options include:

    - `name`: name of item, a string value
    - `age`: age of item, an integer

    This command uses OneCLI. All options can be set via environment
    variable, config file, or command line. See the OneCLI docs for
    more information:

    https://onecli.readthedocs.io

    See the docs of this command for more information:

    https://packrat.readthedocs.io

    Exit codes:
    13    Haunted House
    "
    [options]
    (throw
      (ex-info
        {
         :problem :no-subcommand-specified
         :error "no subcommand specified; run `wizard help` for more information"
         ;; OneCLI catches exceptions for you and stops the program with
         ;; a default exit code, but you can specify exit codes when an
         ;; exception is caught
         :exit-code 13
         }
        )
      )
    )

  (defn -main [& args]
    (System/exit
      (core/go! {:program-name "wizard"
                 :args args
                 :env (System/getenv)})))

And you compiled it into an uberjar, like this::

    lein uberjar

Then you could call your function from the CLI, like this::



