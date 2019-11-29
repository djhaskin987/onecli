(ns onecli.core
  (:require
    [clojure.string :as string]))


(defmacro dbg [body]
  `(let [x# ~body]
     (println "dbg:" '~body "=" x#)
     (flush)
     x#))

;;  (:require
;;    [cheshire.core :as json]
;;    [clj-http.client :as client]))

;;; UTF-8 by default :)
;;(defn base-slurp [loc]
;;  (let [input (if (= loc "-")
;;                *in*
;;                loc)]
;;    (clojure.core/slurp input :encoding "UTF-8")))
;;
;;(defn default-slurp [resource]
;;  (if (re-matches #"https?://.*" (str resource))
;;    (:body
;;      (if-let [[whole-thing protocol auth-stuff rest-of-it]
;;               (re-matches #"(https?://)([^@]+)@(.+)" resource)]
;;        (if-let [[_ username password]
;;                 (re-matches #"([^:]+):([^:]+)" auth-stuff)]
;;          (client/get
;;            (str
;;              protocol
;;              rest-of-it)
;;            {
;;             :basic-auth [(java.net.URLDecoder/decode username)
;;                          (java.net.URLDecoder/decode password)]})
;;          (if-let [[_ headerkey headerval]
;;                   (re-matches #"([^=]+)=([^=]+)" auth-stuff)]
;;            (client/get (str
;;                          protocol
;;                          rest-of-it)
;;                        {
;;                         :headers
;;                         {
;;                          (keyword (java.net.URLDecoder/decode headerkey))
;;                          (java.net.URLDecoder/decode headerval)}})
;;            (client/get (str
;;                          protocol
;;                          rest-of-it)
;;                        {
;;                         :oauth-token (java.net.URLDecoder/decode auth-stuff)
;;                         })))
;;        (client/get resource)))
;;    (base-slurp resource)))

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
                     #"--(disable|enable|reset|assoc|add|set)-(\w+)" arg)]
            (let [kopt (keyword clean-opt)
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
                              (if (nil? t)
                                i
                                (t i))]
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
;;(defn
;;  get-env-vars
;;  [{:keys
;;    [
;;     boxes
;;     env-vars
;;     flags
;;     list-sep-pat
;;     lists
;;     map-sep-pat
;;     maps
;;     program-name
;;     transforms
;;     ]
;;    }
;;   ]
;;  (letfn [(map-transform [t v]
;;            (->> (string/split v list-sep-pat)
;;              (map #(into [] (string/split % map-sep-pat)))
;;              (map [[k v]] [(keyword k) (t v)])
;;              (into {})))
;;          (list-transform [t v]
;;            (->>
;;              (string/split v list-sep-pat)
;;              (map t)))
;;          (box-transform [t v]
;;            (t v))
;;          (boolean-transform [v]
;;            (cond (= v "true")
;;                  true
;;                  (= v "false")
;;                  false
;;                  :else
;;                  (throw
;;                    (ex-info
;;                      (str
;;                        "Boolean options require `true` or `false` to be"
;;                        "set in environment variables")
;;                      {:value-given v}))))]
;;    (let [
;;          transform-functions
;;          (reduce
;;            into
;;            {}
;;            (map
;;              (fn [m] [m (if-let [t (get transforms m)]
;;                           (partial map-transform t)
;;                           (partial map-transform identity))])
;;              maps)
;;            (map
;;              (fn [l] [l (if-let [t (get transforms l)]
;;                           (partial list-transform t)
;;                           (partial list-transform identity))])
;;              lists)
;;            (map
;;              (fn [x] [x (if-let [t (get transforms x)]
;;                           (partial box-transform t)
;;                           (partial box-transform identity))])
;;              boxes)
;;            (map
;;              (fn [b] [b boolean-transform])
;;              flags))
;;          clean-name
;;          (upper-case
;;            (string/replace program-name #"\W" "_"))]
;;      (as-> env-vars it
;;            (filter (fn [[k v]]
;;                      (re-matches (re-pattern
;;                                    (str
;;                                      "\\Q"
;;                                      clean-name
;;                                      "\\E"
;;                                      "_"
;;                                      "[A-Z_]+")) k))
;;                    it)
;;            (map
;;              (fn [[k v]]
;;                (let [option-key (as-> k it
;;                                       (subs it (+ 1 (.length clean-name)))
;;                                       (string/lower-case it)
;;                                       (string/replace it #"_" "-")
;;                                       (keyword it))]
;;                  (if-let [tf (transforms option-key)]
;;                    [option-key (tf v)]
;;                    [option-key v]))) it)
;;            (into {} it)))))
;;
;;(defn- expand-option-packs
;;  [available-option-packs options]
;;  (as-> (:option-packs options) it
;;        (mapv available-option-packs it)
;;        (into {} it)
;;        (merge it options)
;;        (dissoc it :option-packs)))
;;
;;(defn- get-config [configs]
;;  (as-> configs it
;;        (map (fn [{:keys [file read-fn]}]
;;               (try
;;                 (read-fn (default-slurp file))
;;                 (catch Exception e
;;                   (hash-map))))
;;             it)
;;        (reduce merge it)))
;;
;;;; aliases are search/replacements
;;;; boxes are values
;;;; flags are booleans
;;;; lists are lists
;;;; maps are maps
;;;; transforms are simple filters
;;(defn run
;;  [
;;   program-name
;;   description &
;;   {
;;    :keys [
;;           aliases
;;           boxes
;;           defaults
;;           flags
;;           kv-sep
;;           list-sep
;;           lists
;;           maps
;;           transforms
;;           ]
;;    :or {
;;         list-sep ","
;;         kv-sep "="
;;         read-fns {}
;;         maps #{}
;;         lists #{}
;;         boxes #{}
;;         flags #{}
;;         }
;;    }]
;;
;;  (let [list-sep-pat
;;        (re-pattern
;;          (str
;;            "\\Q"
;;            list-sep
;;            "\\E"))
;;        map-sep-pat
;;        (re-pattern
;;          (str
;;            "\\Q"
;;            map-sep
;;            "\\E"))
;;        cli-options
;;        (parse-args *command-line-args*)
;;        env-options
;;        (get-env-vars
;;          {
;;           :boxes boxes
;;           :env-vars (System/getenv)
;;           :lists lists
;;           :maps maps
;;           :flags flags
;;           :map-sep-pat map-sep-pat
;;           :list-sep-pat list-sep-pat
;;           }
;;          )
;;        config-files
;;        (reduce
;;          into
;;          []
;;          [
;;           (if-let [app-data (System/getenv "AppData")]
;;             [{:file (io/file
;;                       (string/join
;;                         java.io.File/separator
;;                         [app-data
;;                          program-name
;;                          (str
;;                            "config."
;;                            k)]))
;;               :read-fn #(json/parse-string % true)}]
;;             [])
;;           (if-let [home (System/getenv "HOME")]
;;             [{:file (io/file
;;                       (string/join
;;                         java.io.File/separator
;;                         [home
;;                          (str
;;                            "."
;;                            program-name
;;                            ".json")]))
;;               :read-fn #(json/parse-string % true)}]
;;             [])
;;           [{:file (io/file (string/join java.io.File/separator
;;                                         ["." (str
;;                                                program-name
;;                                                ".json")]))
;;             :read-fn #(json/parse-string % true)}]
;;           (:config-files cli-options)])
;;        config
;;        (get-config config-files)
;;        expanded-cfg (expand-option-packs config)
;;        expanded-env (expand-option-packs env-vars)
;;        expanded-cli (expand-option-packs global-options)
;;        effective-options
;;          (as->
;;            [defaults
;;             expanded-cfg
;;             (reduce dissoc
;;                     expanded-env
;;                     [:config-files
;;                      :json-config-files
;;                      :edn-config-files
;;                      ])
;;             (reduce dissoc
;;                     expanded-cli
;;                     [:config-files
;;                      :json-config-files
;;                      :edn-config-files
;;                      ])
;;             ] it
;;            (mapv (fn [x] (into {}
;;                                (filter
;;                             #(not (nil? (second %)))
;;                           x))) it)
;;            (reduce merge (hash-map) it))]
;;    (check-required! effective-options subcmd-cli)
;;    ((get-in functions
;;             (:commands effective-options))
;;       effective-options)
;;;; Subproc package system forks processess,
;;;; which causes the VM to hang unless this is called
;;;; https://dev.clojure.org/jira/browse/CLJ-959
;;(System/exit 0))
