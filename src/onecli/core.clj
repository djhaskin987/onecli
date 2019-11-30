(ns onecli.core
  (:require
    [cheshire.core :as json]
    [clj-http.client :as client]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    ))

(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     (flush)
     x#))

(defn exit-error [status msg]
  (.println ^java.io.PrintWriter *err* msg)
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

(def transforms {
                 :int
                 #(java.lang.Long/parseLong %)
                 :float
                 #(java.lang.Double/parseDouble %)
                 :json
                 json/parse-string
                 :file
                 default-slurp})

(defn parse-args
  [
   {
    :keys
    [
     args
     aliases
     transforms
     map-sep
     ]
    :or
    {
     aliases
     {}
     transforms
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
                     #"--(disable|enable|reset|assoc|add|set|json)-(.+)" arg)]
            (let [kopt (keyword (string/lower-case clean-opt))
                  kact (keyword action)
                  t (kopt transforms)]
              (cond
                (= kact :disable)
                (recur (conj m [kopt false]) rargs)
                (= kact :enable)
                (recur (conj m [kopt true]) rargs)
                (= kact :reset)
                (recur (conj m [kopt nil]) rargs)
                (or
                  (= kact :json)
                  (= kact :set)
                  (= kact :add)
                  (= kact :assoc))
                (if (empty? rargs)
                  (throw (ex-info "not enough arguments supplied"
                                  {:option kopt
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
                                    (if (nil? t)
                                      v
                                      (t v)))
                          (ex-info "Argument not recognized as a key/value pair"
                                   {:option kopt
                                    :action kact
                                    :argument arg}))
                        (let [groomed-val
                              (if (= kact :json)
                                (json/parse-string i true)
                                (if (nil? t)
                                  i
                                  (t i)))]
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
     transforms
     ]
    :or
    {
     aliases
     {}
     transforms
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
          (string/replace program-name #"\W" "_"))
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
                      kabel (keyword (string/lower-case label))
                      t (kopt transforms)]
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
                           (if (nil? t) v (t v))
                           (= kabel :json)
                           (json/parse-string v true)
                           (= kabel :map)
                           (into {}
                                 (map
                                   (fn please-work [i]
                                     (if-let [[_ k v] (re-matches map-sep-pat i)]
                                       [(keyword k) (if (nil? t) v (t v))]
                                       (throw (ex-info "no key value pairs detected"
                                                       {:item i}))))
                                   (string/split v list-sep-pat)))
                           (= kabel :list)
                           (map (fn [x] (if (nil? t) x (t x)))
                                (string/split v list-sep-pat))
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
  (dbg (as-> (:option-packs options) it
        (mapv available-option-packs it)
        (into {} it)
        (merge it options)
        (dissoc it :option-packs))))

(defn display-config!
  [options]
  (println (json/generate-string options))
  0)

(defn try-file [fparts]
  (let [f (io/file
            (string/join
              java.io.File/separator
              fparts))]
    (when (.exists f)
      [f])))

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
           transforms
           ]
    :as params
    :or {
         list-sep ","
         map-sep "="
         cli-aliases
         {}
         env-aliases
         {}
         transforms
         {}
         available-option-packs
         {}
         defaults
         {}
         functions {}
         }
    }]
  (let [base-functions
        {
         ["options" "show"] display-config!
         ["options"] display-config!
         }
        effective-functions
        (merge base-functions functions)
        cli-options
        (parse-args (into params [
                                  [:args args]
                                  [:aliases cli-aliases]
                                  ]))
        env-options
        (parse-env-vars (into params [
                                    [:env-vars env]
                                    [:aliases env-aliases]
                                    ]))
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
           ])
        expanded-cfg
        (reduce merge
                (map (fn [file]
                       (dbg (expand-option-packs
                                available-option-packs
                                (json/parse-string
                                  (default-slurp (dbg file)) true))
                              ))
                     config-files))
        expanded-env (expand-option-packs available-option-packs env-options)
        expanded-cli (expand-option-packs available-option-packs cli-options)
        effective-options
        (as-> [(dbg defaults)
               (dbg expanded-cfg)
               (dbg (dissoc expanded-env :config-files))
               (dbg (dissoc expanded-cli :config-files))
               ] it
              (mapv (fn [x] (into {}
                                  (filter
                                    #(not (nil? (second %)))
                                    x))) it)
              (reduce merge (hash-map) it))
        ]
    ;; Subproc package system forks processess,
    ;; which causes the VM to hang unless this is called
    ;; https://dev.clojure.org/jira/browse/CLJ-959
    (if-let [func (get effective-functions (:commands effective-options))]
        (func effective-options)
      (throw (ex-info (string/join
                    \newline
                    (into
                        ["Unknown command."
                         "Command given:"
                         (str "  - `"
                              (string/join " " (:commands effective-options))
                              "`")
                         "Available commands:"]
                      (map (fn [commands]
                             (str
                               "  - `"
                               (string/join " " commands)
                               "`"))
                           (keys effective-functions))))
                      {:options effective-options})))))

(defn default-spit [loc stuff]
  (clojure.core/spit loc (pr-str stuff) :encoding "UTF-8"))

(defn pretty-spit [loc stuff]
  (with-open
    [ow (io/writer loc :encoding "UTF-8")]
    (pprint/pprint stuff ow)))
