(ns onecli.core
  (:require
    [cheshire.core :as json]
    [clj-http.client :as client]
    [clojure.java.io :as io]
    [clojure.pprint :as pprint]
    [clojure.string :as string]
    ))

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
   args &
   {
    :keys
    [
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
    (loop [m {}
           arguments expanded-args]
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
  get-env-vars
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
        expanded-env
        (into (hash-map)
              (map (fn [[k v]]
                     (if-let [other (get aliases k)]
                       [other v]
                       [k v]))
                   env-vars))
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
    (reduce (fn [m [k v]]
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
                                   {:function :get-env-vars
                                    :option kopt
                                    :label kabel
                                    :var k
                                    :val v}))))
                           (= kabel :item)
                           (if (nil? t) v (t v))
                           (= kabel :json)
                           (json/parse-string v true)
                           (= kabel :map)
                           (->> (string/split v list-sep-pat)
                             (map #(if-let [[_ k v] (re-matches map-sep-pat %)] [k v] [% nil]))
                             (map [[k v]] [(keyword k) (if (nil? t) v (t v))])
                             (into {}))
                           (= kabel :list)
                           (map (fn [x] (if (nil? t) x (t x)))
                                (string/split v list-sep-pat))
                           :else
                           (throw
                             (ex-info
                               "nothing makes sense in this world"
                               {:function :get-env-vars
                                :option kopt
                                :label kabel
                                :var k
                                :val v})))))
                m))
            (hash-map)
            expanded-env)))

(defn- expand-option-packs
  [available-option-packs options]
  (as-> (:option-packs options) it
        (mapv available-option-packs it)
        (into {} it)
        (merge it options)
        (dissoc it :option-packs)))

(defn display-config!
  [options]
  (println (json/generate-string options)))

(defn run
  [
   {
    :keys [
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
                                  [:arguments *command-line-args*]
                                  [:aliases cli-aliases]
                                  ]))
        env-options
        (get-env-vars (into params [
                                    [:env-vars (System/getenv)]
                                    [:aliases env-aliases]
                                    ]))
        config-files
        (reduce
          into
          []
          [
           (if-let [app-data (System/getenv "AppData")]
             [(io/file
                (string/join
                  java.io.File/separator
                  [app-data
                   program-name
                   "config.json"]))]
             [])
           (if-let [home (System/getenv "HOME")]
             [(io/file
                (string/join
                  java.io.File/separator
                  [home
                   (str
                     "."
                     program-name
                     ".json")]))]
             [])
           [(io/file (string/join java.io.File/separator
                                  ["." (str
                                         program-name
                                         ".json")]))]
           (if-let [cf (:config-files cli-options)]
             cf
             [])
           (if-let [cf (:config-files env-options)]
             cf
             [])
           ])
        expanded-cfg
        (reduce merge
                (map (fn [file]
                       (try
                         (expand-option-packs
                           available-option-packs
                           (json/parse-string
                             (default-slurp file) true))
                         (catch Exception e
                           (hash-map))))
                     config-files))
        expanded-env (expand-option-packs available-option-packs env-options)
        expanded-cli (expand-option-packs available-option-packs cli-options)
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
    ;; Subproc package system forks processess,
    ;; which causes the VM to hang unless this is called
    ;; https://dev.clojure.org/jira/browse/CLJ-959
    (System/exit
      ((get effective-functions (:commands effective-options))
       effective-options))))
