(ns onecli.cli
  (:require
      [clojure.java.io :as io]
      [onecli.core :as core])
  (:gen-class))

(defn a
  "
  This is the `a` subcommand help.
  "
  [options]
  (if (.exists (:filename options))
    {:result "a is awesome"}
    {:result "a is NOT awesome"})
  )

(defn ab
  "
  This is the `a b` subcommand help.
  "
  [options]
  {:result "ab is awesome"}
  )

(defn c
  "
  This is the `c` subcommand help.
  "
  [options]
  {:result "c is awesome"}
  )

(defn -main [& args]
  (System/exit
    (core/go! {:program-name "onecli"
               :args args
               :setup (fn [options]
                        (as-> options it
                          (assoc it :println "setup")
                          (update it :filename io/as-file)))
               :teardown (fn [options] (shutdown-agents))
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
                "-f" "--set-filename"
                "-z" "--add-zeta"
                "-E" "--bork-eta"
                }
               :env (System/getenv)})))
