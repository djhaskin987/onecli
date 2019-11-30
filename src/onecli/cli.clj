(ns onecli.cli
  (:require
      [onecli.core :as core])
  (:gen-class))

(defn -main [& args]
  (System/exit
    (core/go! {:program-name "onecli"
               :args args
               :env (System/getenv)})))
