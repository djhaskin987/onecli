(ns onecli.cli
  (:gen-class)
  (:require
   [clojure.java.io :as io]
   [onecli.core :as core]))

(defn a
  "
  This is the `a` subcommand help.
  "
  [options]
  (if (.exists (:filename options))
    {:result "a is awesome"}
    {:result "a is NOT awesome"}))

(defn ab
  "
  This is the `a b` subcommand help.
  "
  [_]
  {:result "ab is awesome"})

(defn c
  "
  This is the `c` subcommand help.
  "
  [_]
  {:result "c is awesome"})

(defn -main
  [& args]
  (System/exit
   (core/go! {:program-name "onecli"
              :args args
              :setup (fn [options]
                       (as-> options it
                         (assoc it :println "setup")
                         (update it :filename io/as-file)))
              :teardown (fn [_] (shutdown-agents))
              :functions
              {["a"] 'onecli.cli/a
               ["a" "b"] 'onecli.cli/ab
               ["c"] 'onecli.cli/c}
              :cli-aliases
              {"-a" "--set-alpha"
               "-b" "--file-beta"
               "-g" "--yaml-gamma"
               "-d" "--enable-delta"
               "-D" "--disable-delta"
               "-e" "--assoc-epsilon"
               "-f" "--set-filename"
               "-z" "--add-zeta"
               "-E" "--bork-eta"}
              :env (System/getenv)})))
