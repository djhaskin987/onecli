Why OneCLI?
===========


OneCLI is Clojure library that makes CLI very nice, easy, and clojure-y.

The goal of OneCLI is to make it easy to simply write potentially pure
functions, and have OneCLI worry about the rest -- config files,
command line arguments, default arguments -- they are all "taken care of" out
of the box.

OneCLI allows the programmer to easily make some very nice command line
interface tools.

It gathers configuration from defaults provided by the caller, by the
config files (including https/remote ones), from
environment, and from the CLI. then it merges these configuration items
in an intuitive and easy-to-reason-about way. The result is a low-friction,
clear and straightforward way to write commands.
