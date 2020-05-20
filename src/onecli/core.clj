(ns onecli.core
  (:require
    [cheshire.core :as json]
    [cheshire.generate :as generate]
    [clj-http.client :as client]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    )
  )

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     (flush)
     x#))

(generate/add-encoder
  java.lang.Object
  (fn
    [obj jsonGenerator]
    (generate/write-string jsonGenerator (str obj))))

(defn exit-error [status msg]
  ;; I've decided to put resulting errors to stdout,
  ;; leaving the stderr clean for the caller
  ;; EXCEPT when printing help screens
  (println msg)
  (System/exit status))

(defn exit-out [status msg]
  (println msg)
  (System/exit status))

;; UTF-8 by default :)
(defn base-slurp [loc]
  (let [input (if (= loc "-")
                *in*
                loc)]
    (clojure.core/slurp input :encoding "UTF-8")))

(defn default-slurp [resource]
  (if (re-matches #"https?://.*" (str resource))
    (:body
      (if-let [[whole-thing protocol auth-stuff rest-of-it]
               (re-matches #"(https?://)([^@]+)@(.+)" resource)]
        (if-let [[_ username password]
                 (re-matches #"([^:]+):([^:]+)" auth-stuff)]
          (client/get
            (str
              protocol
              rest-of-it)
            {
             :basic-auth [(java.net.URLDecoder/decode username)
                          (java.net.URLDecoder/decode password)]})
          (if-let [[_ headerkey headerval]
                   (re-matches #"([^=]+)=([^=]+)" auth-stuff)]
            (client/get (str
                          protocol
                          rest-of-it)
                        {
                         :headers
                         {
                          (keyword (java.net.URLDecoder/decode headerkey))
                          (java.net.URLDecoder/decode headerval)}})
            (client/get (str
                          protocol
                          rest-of-it)
                        {
                         :oauth-token (java.net.URLDecoder/decode auth-stuff)
                         })))
        (client/get resource)))
    (base-slurp resource)))

(defn parse-args
  [
   {
    :keys
    [
     args
     aliases
     map-sep
     ]
    :or
    {
     aliases
     {}
     map-sep
     "="
     }
    }
   ]
  (let [
        map-sep-pat
        (re-pattern
          (str
            "^([^\\Q"
            map-sep
            "\\E]+)\\Q"
            map-sep
            "\\E(.*)$"))
        expanded-args
        (map (fn [arg]
               (if-let [other (get aliases arg)]
                 other
                 arg))
             args)
        ]
    (loop [
           m {}
           arguments expanded-args
           ]
      (if (empty? arguments)
        m
        (let [arg (first arguments)
              rargs (rest arguments)]

          (if-let [[_ action clean-opt]
                   (re-matches
                     #"--(disable|enable|reset|assoc|add|set|json|file)-(.+)" arg)]
            (let [kopt (keyword (string/lower-case clean-opt))
                  kact (keyword action)]
              (cond
                (= kact :disable)
                (recur (conj m [kopt false]) rargs)
                (= kact :enable)
                (recur (conj m [kopt true]) rargs)
                (= kact :reset)
                (recur (conj m [kopt nil]) rargs)
                (or
                  (= kact :json)
                  (= kact :file)
                  (= kact :set)
                  (= kact :add)
                  (= kact :assoc))
                (if (empty? rargs)
                  (throw (ex-info "not enough arguments supplied"
                                  {
                                   :problem :onecli/not-enough-args
                                   :option kopt
                                   :exit-code 1
                                   :action kact
                                   :argument arg}))
                  (let [
                        i (first rargs)
                        rrargs (rest rargs)
                        ]
                    (recur
                      (if (= kact :assoc)
                        (if-let [[_ k v] (re-matches map-sep-pat i)]
                          (assoc-in m
                                    [kopt k]
                                    v)
                          (ex-info "argument not recognized as a key/value pair"
                                   {:exit-code 1
                                    :problem :onecli/bad-kv-pair
                                    :option kopt
                                    :action kact
                                    :argument arg}))
                        (let [groomed-val
                              (cond
                                (= kact :json)
                                (json/parse-string i true)
                                (= kact :file)
                                (json/parse-string (default-slurp i) true)
                                :else
                                i)
                                ]
                          (if (= kact :add)
                            (update-in m [kopt] #(if
                                                   (empty? %)
                                                   [groomed-val]
                                                   (conj % groomed-val)))
                            (assoc m kopt groomed-val))))
                      rrargs)))))
            (recur
              (update-in m [:commands] #(if (empty? %)
                                          [arg]
                                          (conj % arg)))
              rargs)
            ))))))

(defn
  parse-env-vars
  [{
    :keys
    [
     aliases
     env-vars
     list-sep
     map-sep
     program-name
     ]
    :or
    {
     aliases
     {}
     map-sep
     "="
     list-sep
     ","
     }
    }]
  (let [
        clean-name
        (string/upper-case
          (string/replace
            program-name #"\W" "_"))
        var-pattern
        (re-pattern
          (str
            "\\Q"
            clean-name
            "\\E"
            "_"
            "(LIST|MAP|JSON|ITEM|FLAG)"
            "_"
            "(\\w+)"))
        expanded-alias-env
        (into (hash-map)
              (map (fn [[k v]]
                     (if-let [other (get aliases k)]
                       [other v]
                       [k v]))
                   (filter (fn [[k v]]
                             (not (nil? (get aliases k))))
                           env-vars)))
        expanded-explicit-env
        (filter (fn [[k v]] (nil? (get aliases k))) env-vars)
        list-sep-pat
        (re-pattern
          (str
            "\\Q"
            list-sep
            "\\E"
            ))
        map-sep-pat
        (re-pattern
          (str
            "^([^\\Q"
            map-sep
            "\\E]+)\\Q"
            map-sep
            "\\E(.*)$"))
        ]
    (letfn [(process [e] (reduce (fn [m [k v]]
                                   (if-let [[_ label clean-opt]
                                            (re-matches
                                              var-pattern
                                              k)]
                                     (let [kopt (keyword (string/lower-case (string/replace clean-opt #"_" "-")))
                                           kabel (keyword (string/lower-case label))]
                                       (assoc m kopt
                                              (cond
                                                (= kabel :flag)
                                                (let [ins (string/lower-case v)]
                                                  (cond
                                                    (or (= ins "1")
                                                        (= ins "yes")
                                                        (= ins "true"))
                                                    true
                                                    (or (= ins "0")
                                                        (= ins "no")
                                                        (= ins "false"))
                                                    false
                                                    :else
                                                    (throw
                                                      (ex-info
                                                        "environment variable value not recognized as a boolean value"
                                                        {:function :parse-env-vars
                                                         :option kopt
                                                         :label kabel
                                                         :var k
                                                         :val v}))))
                                                (= kabel :item)
                                                v
                                                (= kabel :json)
                                                (json/parse-string v true)
                                                (= kabel :map)
                                                (into
                                                  {}
                                                  (map
                                                    (fn [i]
                                                      (if-let [[_ k v] (re-matches map-sep-pat i)]
                                                        [(keyword k) v]
                                                        (throw (ex-info "no key value pairs detected"
                                                                        {:item i}))))
                                                    (string/split v list-sep-pat)))
                                                (= kabel :list)
                                                (string/split v list-sep-pat)
                                                :else
                                                (throw
                                                  (ex-info
                                                    "nothing makes sense in this world"
                                                    {:function ::parse-env-vars
                                                     :option kopt
                                                     :label kabel
                                                     :var k
                                                     :val v})))))
                                     m))
                                 (hash-map)
                                 e))]
      (merge
        (process expanded-alias-env)
        (process expanded-explicit-env)))))

(defn- expand-option-packs
  [available-option-packs options]
  (as-> (:option-packs options) it
        (mapv available-option-packs it)
        (into {} it)
        (merge it options)
        (dissoc it :option-packs)))

(defn display-config!
  "
  Displays the effective configuration as provided either by environment
  variables, by CLI arguments, or by configuration files.
  "
  [options]
  options)

(defn try-file [fparts]
  (let [f (io/file
            (string/join
              java.io.File/separator
              fparts))]
    (when (.exists f)
      [f])))

(defn- stacktrace-string
  "Gives back the string of the stacktrace of the given exception."
  [ ^Exception e]
  (let [s (java.io.StringWriter. )
        p (java.io.PrintWriter. s)]
    (.printStackTrace e p)
    (str s)))

(defn help-meta-var [arg]
  (if-let [[_ action clean-opt]
                        (re-matches
                          #"--(assoc|add|set|json|file)-(.+)"
                          arg)]
                 (let [kopt (keyword (string/lower-case clean-opt))
                       kact (keyword action)]
                   (cond
                     (= kact :assoc)
                     "<k>=<v>"
                     (or
                       (= kact :set)
                       (= kact :add))
                     "<value>"
                     (= kact :json)
                     "<inline-json-blob>"
                     (= kact :file)
                     "<json-file>"))))

(defn print-help-screen
  [program-name
   subcommands-list
   aliases
   defaults
   options]
  (println
    (string/join \newline
      (reduce
        into
        [
         [
          (str "Welcome to `" program-name "` !")
          ""
          "A subcommand MUST be given."
          ""
          "Available subcommands:"
          ""
          ]
         (map (fn [cmds] (str "  - `" (string/join \space cmds) "`"))
              (sort-by
                (fn [lst] (string/join \space lst))
                subcommands-list))
         [
          ""
          "All options can be used by all subcommands,"
          "Though any given subcommand may not use all the options."
          "Options can be given in any order, before, interspersed with or after"
          "the subcommands."
          ""
          "Options:"
          ""
          ]
         (map (fn [[small normal]]
                (if-let [metavar (help-meta-var normal)]
                  (format "  - %-26s or %-46s"
                          (str "`" small " " metavar "`")
                          (str "`" normal " " metavar "`"))
                  (format "  - %-26s or %-46s"
                          (str "`" small "`")
                          (str "`" normal "`"))))
              (sort-by
                (fn [[small normal]] (string/replace normal #"^--[a-z]+-" ""))
              aliases))
         (if (empty? defaults)
           []
           (into
             [
              ""
              "Default settings for some of the options:"
              ""
              ]
             (map (fn [[opt v]]
                    (str "  - `" (name opt) "` = `" v "`"))
                  defaults)))
         [
          ""
          "This command uses OneCLI. All options can be set via environment"
          "variable, config file, or command line. See the OneCLI docs for"
          "more information:"
          ""
          "https://onecli.readthedocs.io"
          ""
          "Exit codes:"
          "0     Yee-Haw"
          "1     It's Not Me, It's You"
          "2     It's Not You, It's Me"
          "3     It's Something Else, Don't Shoot The Messenger"
          "128   I Have No Idea What Happened"
          ]
         ])))
  {:suppress-return-output true})

(defn go!
  [
   {
    :keys [
           args
           env
           cli-aliases
           env-aliases
           available-option-packs
           defaults
           functions
           list-sep
           map-sep
           program-name
           setup
           teardown
           ]
    :as params
    :or {
         list-sep ","
         map-sep "="
         cli-aliases
         {}
         env-aliases
         {}
         available-option-packs
         {}
         defaults
         {}
         functions {}
         setup nil
         teardown nil
         }
    }]
  (let [
        base-functions
        {
         ["options" "show"] 'onecli.core/display-config!
         ["options"] 'onecli.core/display-config!
         }
        given-functions
        (reduce
          (fn make-help-screens [m [c f]]
            (assoc (assoc m c (resolve f))
                   (conj c "help")
                   (fn [options]
                     (println
                       (str
                         "Help page for `"
                         (string/join
                           " "
                           (into [program-name]
                                 c))
                         "`:"
                         ))
                     (println
                       (as-> f it
                             (resolve it)
                             (meta it)
                             (:doc it)
                             (string/trim-newline it)
                             (string/split it #"\n")
                             (map string/trim it)
                             (string/join \newline it)))
                     {:suppress-return-output true})))
          {}
          (merge base-functions functions))
        effective-cli-aliases
        (into
          {
           "--help" "help"
           "-?" "help"
           "-h" "help"
           }
          cli-aliases)
        help-screen-func
        (partial print-help-screen
                 program-name
                 (into
                   (keys given-functions)
                   [
                    ["help"]])
                 effective-cli-aliases
                 defaults)
        effective-functions
        (as-> given-functions fs
              (if (contains? fs [])
                fs
                (assoc fs [] help-screen-func))
              (if (contains? fs ["help"])
                fs
                (assoc fs ["help"] help-screen-func)))
        cli-options
        (parse-args (into params [
                                  [:args args]
                                  [:aliases effective-cli-aliases]
                                  ]))
        env-options
        (parse-env-vars (into params [
                                      [:env-vars env]
                                      [:aliases env-aliases]
                                      ]))
        expanded-env (expand-option-packs available-option-packs env-options)
        expanded-cli (expand-option-packs available-option-packs cli-options)
        config-files
        (reduce
          into
          []
          [
           (if-let [app-data (System/getenv "AppData")]
             (try-file [app-data
                        program-name
                        "config.json"]))
           (if-let [home (System/getenv "HOME")]
             (try-file [home
                        (str
                          "."
                          program-name
                          ".json")]))
           (try-file
             ["." (str
                    program-name
                    ".json")])
           (:config-files expanded-env)
           (:config-files expanded-cli)
           ])
        expanded-cfg
        (reduce merge
                (map (fn [file]
                       (expand-option-packs
                         available-option-packs
                         (json/parse-string
                           (default-slurp file) true))
                       )
                     config-files))
        effective-options
        (as-> [defaults
               expanded-cfg
               (dissoc expanded-env :config-files)
               (dissoc expanded-cli :config-files)
               ] it
              (mapv (fn [x] (into {}
                                  (filter
                                    #(not (nil? (second %)))
                                    x))) it)
              (reduce merge (hash-map) it))
        ]
    (if-let [func (get effective-functions
                       (if-let [commands (:commands effective-options)]
                         commands
                         []))]
      (try

        (let [ret
              (if (not (nil? setup))
                (func (setup effective-options))
                (func effective-options))]
          (when
            (not (:suppress-return-output ret))
            (println (json/generate-string (dissoc ret :onecli))))
          (when (not (nil? teardown)) (teardown effective-options))
          ;; Subproc package system forks processess,
          ;; which causes the VM to hang unless this is called
          ;; https://dev.clojure.org/jira/browse/CLJ-959
          (System/exit
            (if-let [return-code (:exit-code (:onecli ret))]
              return-code
              0)))
        (catch clojure.lang.ExceptionInfo e
          (exit-error
            (if-let [code (:exit-code (:onecli (ex-data e)))]
              code
              128)
            (json/generate-string
              (as-> (ex-data e) it
               (assoc it :error (str e))
               (assoc it :stacktrace
                      (stacktrace-string e))
               (assoc it :given-options effective-options)))))
        (catch Exception e
          (exit-error
            128
          (json/generate-string
            {:error (str e)
             :problem :unknown-problem
             :stacktrace (stacktrace-string e)
             :given-options effective-options
             })))))
      (exit-error
        1
        (json/generate-string
          {
           :error "Unknown command"
           :problem :unknown-command
           :given-options effective-options
           }))))

(defn default-spit [loc stuff]
  (clojure.core/spit loc (pr-str stuff) :encoding "UTF-8"))

(defn pretty-spit [loc stuff]
  (with-open
    [ow (io/writer loc :encoding "UTF-8")]
    (pprint/pprint stuff ow)))
