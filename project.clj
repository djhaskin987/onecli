(defproject
  onecli
  "0.7.0-SNAPSHOT"
  :description "One CLI, for all people, for all time."
  :url "https://git.sr.ht/~djhaskin987/onecli"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :deploy-repositories [["clojars" {:url "https://clojars.org/repo"
                                    :username :env/CLOJARS_USERNAME
                                    :password :env/CLOJARS_PASSWORD
                                    :sign-releases true}]]

  :dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                 [cheshire "5.9.0"]
                 [org.martinklepsch/clj-http-lite "0.4.3"]]

  :global-vars {*warn-on-reflection* true}
  ;;:repl-options {
  ;;               :init-ns onecli.core
  ;;               }
  :profiles {:uberjar {:dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                                      [cheshire "5.9.0"]
                                      [org.martinklepsch/clj-http-lite "0.4.3"]
                                      [borkdude/clj-reflector-graal-java11-fix "0.0.1-graalvm-20.1.0"]]
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]
                       :main onecli.cli
                       :aot :all}
             :deploy {:dependencies [[org.clojure/clojure "1.10.2-alpha1"]
                                     [cheshire "5.9.0"]
                                     [org.martinklepsch/clj-http-lite "0.4.3"]]}
             :dev {:dependencies [[pjstadig/humane-test-output "0.9.0"]
                                  [org.clojure/clojure "1.10.2-alpha1"]
                                  [cheshire "5.9.0"]
                                  [org.martinklepsch/clj-http-lite "0.4.3"]]
                   :plugins [[test2junit "1.3.3"]
                             [lein-licenses "0.2.2"]
                             [lein-print "0.1.0"]]
                   :test2junit-output-dir "target/test-results"
                   :injections [(require 'pjstadig.humane-test-output)
                                (pjstadig.humane-test-output/activate!)]}}
  :target-path "target/%s/")
