(ns onecli.cli
  (:require
      [onecli.core :as core])
  (:gen-class))

(defn -main [& args]
  (core/run {:program-name "onecli"
             :args args
             :env (System/getenv)}))
