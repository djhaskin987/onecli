(ns onecli.core-test
  (:require [clojure.test :refer :all]
            [onecli.core :refer :all]))



(deftest test-arg-parser
         (testing "that no arguments doesn't fail."
                  (is (empty? (parse-args
                                []))))
         (testing "that enable and disable works, and one overrides the other"
                  (is (=
                        {:garden true}
                        (parse-args ["--enable-garden"])))
                  (is (=
                        {:garden false}
                        (parse-args ["--disable-garden"])))
                  (is (=
                        {:garden false}
                        (parse-args ["--enable-garden" "--disable-garden"]))))
         (testing "normal case"
                  (is (=
                        {:money-bags {
                                      "money" 38
                                      }
                         :gnomes 17
                         :garden true
                         :hedge :tall
                         :pets ["horse" "dog"]
                         :commands ["henceforth"
                                    "and"
                                    "forever"]}
                         (parse-args ["--json-abc"
                                      "{\"rags\": [{\"to\": \"riches\", \"years\": 11},{\"to\": \"ashes\", \"years\": 12}]}"
                                      "--assoc-money-bags"
                                      "money=38.3"
                                      "-g"
                                      "17"
                                      "--enable-garden"
                                      "--add-pets"
                                      "horse"
                                      "--add-pets"
                                      "dog"
                                      "--set-hedge"
                                      "tall"
                                      "henceforth"
                                      "and"
                                      "forever"]
                                     :aliases
                                     {"-g" "--set-gnomes"}
                                     :transforms {
                                                  :gnomes
                                                  (:int transforms)
                                                  :hedge keyword
                                                  :money-bags
                                                  (:float transforms)
                                                  }))))
         (testing "reset add"
                  (is (=
                        {:pets ["cat"]}
                        (parse-args ["--add-pets"
                                     "horse"
                                     "--add-pets"
                                     "dog"
                                     "--reset-pets"
                                     "--add-pets"
                                     "cat"])))))

(defn default-spit [loc stuff]
  (clojure.core/spit loc (pr-str stuff) :encoding "UTF-8"))

(defn pretty-spit [loc stuff]
  (with-open
    [ow (io/writer loc :encoding "UTF-8")]
    (pprint/pprint stuff ow)))
