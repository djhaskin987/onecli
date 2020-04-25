(ns onecli.cli
  (:require
      [onecli.core :as core])
  (:gen-class))

(defn a
  "
  This is the `a` subcommand help.
  "
  []
  {:result "a is awesome"}
  )

(defn ab
  "
  This is the `a b` subcommand help.
  "
  []
  {:result "ab is awesome"}
  )

(defn c
  "
  This is the `c` subcommand help.
  "
  []
  {:result "c is awesome"}
  )

(defn -main [& args]
  (System/exit
    (core/go! {:program-name "onecli"
               :args args
               :functions
               {
                ["a"] 'onecli.cli/a
                ["a" "b"] 'onecli.cli/ab
                ["c"] 'onecli.cli/c
                }
               :cli-aliases
               {
                "-a" "--set-alpha"
                "-b" "--file-beta"
                "-g" "--json-gamma"
                "-d" "--enable-delta"
                "-D" "--disable-delta"
                "-e" "--assoc-epsilon"
                "-z" "--add-zeta"
                "-E" "--bork-eta"
                }
               :env (System/getenv)})))
