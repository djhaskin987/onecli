(defproject onecli "0.1.0-SNAPSHOT"
            :description "One CLI, for all people, for all time."
            :url "https://git.sr.ht/~djhaskin987/onecli"
            :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
                      :url "https://www.eclipse.org/legal/epl-2.0/"}
            :dependencies [
                           [org.clojure/clojure "1.10.0"]
                           [cheshire "5.9.0"]]


            :global-vars {*warn-on-reflection* true}
            :plugins [[lein-licenses "0.2.2"]
                      [lein-print "0.1.0"]]
            :repl-options {:init-ns onecli.core}

            :profiles {
                       :dev {
                             :dependencies [
                                            [pjstadig/humane-test-output "0.9.0"]
                                            [org.clojure/clojure "1.10.1"]
                                            [cheshire "5.9.0"]
                                            ]
                             :plugins [[test2junit "1.3.3"]]
                             :test2junit-output-dir "target/test-results"
                             :injections [(require 'pjstadig.humane-test-output)
                                          (pjstadig.humane-test-output/activate!)]
                             }
                       :uberjar {:aot [
                                       onecli.core
                                       ]}}
                       :target-path "target/%s/")
