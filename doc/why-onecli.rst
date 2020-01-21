Why OneCLI?
===========

I was thinking really hard how to make the CLI more approachable and easy
to target. I wanted to find a simple way to map commands and options to
function calls, and I never wanted to write another argument parser
or configuration file parser again.

I also wanted to write a command that adhered as much as possible to the unix
philosophy of "do one thing well", a command that has maximum pipe usefulness.
It made sense to use JSON everywhere to meet this goal.

With these two ideas in my head, I came up with CPC, or Command Procedure
Call, a standard for how a CLI program interacts with the user and behaves
in a clean, standard way.

OneCLI is Clojure library that makes CLI very nice, easy, and clojure-y.

The goal of OneCLI is to make it easy to simply write potentially pure
functions, and have OneCLI worry about the rest -- config files,
command line arguments, default arguments -- they are all "taken care of" out
of the box.

OneCLI allows the programmer to easily make some very nice command line
interface tools.
