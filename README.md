# onecli

A Clojure library that makes CLI very nice, easy, and clojure-y.

## Usage

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
      [] main-cli!
      ["do"] do-it!
      ["do" "list"] do-list-it!
     :env-vars (System/getenv)
     :args args})
)
```

## License

Copyright © 2017 Daniel Jay Haskin et. al., see the AUTHORS.md file.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
