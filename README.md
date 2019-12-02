# onecli

A Clojure library that makes CLI very nice, easy, and clojure-y.

## Usage

OneCLI allows the programmer to easily make some very nice command line interface
tools.

It gathers configuration from config files (including https/remote ones),
from environment, and from the CLI. then it merges them.

Any CLI argument given as `--add-<thing> <arg>` will have its argument
added to a list inside the options map. `--set-<thing> <arg>` will set
the value of the options map at the given key (`<thing>`) will be set to a string
(unless a transform is specified.) Arguments of the form
`--[enable|disable]-<thing>` will set `<thing>` in the options map to `true` or
`false`, respectively.

Similarly, any env var read as `<PROGRAM_NAME>_ITEM_<THING_A_LING>` will be
added to the options map under the key `:thing-a-ling`. if the middle of the env
var is `_LIST_` the contents will be treated like a list separated by the
`:list-sep` character (by default this is the comma `,`)
and if the middle of the var is `_MAP_` it will be further split into key/value pairs
and put into a map as the value of `:thing-a-ling` in the options map.

Functions are specified to call when different sequences of subcommands are
given. The docstrings of those functions are used for their help screens.

Leiningen:
```
[onecli 0.1.0]
```

Usage looks like this:

```
(ns my-program
  (:require
    [onecli.core :as onecli])
  (:gen-class))

(defn -main [& args]
  (onecli/go!
    {:program-name "my-program"
     :functions {
      [] 'my-program.main-cli!
      ["do"] 'my-program.do-it!
      ["do" "list"] 'my-program.do-list-it!
     :env-vars (System/getenv)
     :args args})
)
```

## License

Copyright © 2019 Daniel Jay Haskin et. al., see the AUTHORS.md file.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
