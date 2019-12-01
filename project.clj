(defproject onecli "0.2.0"
            :description "One CLI, for all people, for all time."
            :url "https://git.sr.ht/~djhaskin987/onecli"
            :license {:name "Eclipse Public License"
                      :url "http://www.eclipse.org/legal/epl-v10.html"}
            :dependencies [
                           [org.clojure/clojure "1.10.0"]
                           [cheshire "5.9.0"]
                           [clj-http "3.10.0"]
                           ]
            ;;:main onecli.cli
            :global-vars {*warn-on-reflection* true}
            :repl-options {:init-ns onecli.core}
            :profiles {
                       :dev {
                             :dependencies [
                                            [pjstadig/humane-test-output "0.9.0"]
                                            [org.clojure/clojure "1.10.1"]
                                            [cheshire "5.9.0"]
                                            [clj-http "3.10.0"]
                                            ]
                             :plugins [
                                       [test2junit "1.3.3"]
                                       [lein-licenses "0.2.2"]
                                       [lein-print "0.1.0"]
                                       ]
                             :test2junit-output-dir "target/test-results"
                             :injections [(require 'pjstadig.humane-test-output)
                                          (pjstadig.humane-test-output/activate!)]
                             }
                       :jar {:aot [
                                   onecli.core
                                   ]}
                       :uberjar {:aot [
                                       onecli.core
                                       onecli.cli
                                       ]}}
                       :target-path "target/%s/")
