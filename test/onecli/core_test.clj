(ns onecli.core-test
  (:require [clojure.test :refer :all]
            [onecli.core :refer :all]))

(deftest test-env-vars
         (testing "That no environment variables works."
                  (is (empty? (parse-env-vars
                                {:program-name "testing"
                                 :env-vars {}}))))
         (testing "That something should be thrown"
                  (is (thrown? Exception
                               (parse-env-vars
                                 {:program-name "testing"
                                  :env-vars {"TESTING_FLAG_PETS" "a,b"}}))))
         (testing "that aliases do not take precedence over the real thing"
                  (is (= {:type "mage"}
                         (parse-env-vars
                           {
                            :program-name "testing"
                            :env-vars {
                                       "TESTING_ITEM_TYPE" "mage"
                                       "TYPE" "warrior"
                                       }
                            :aliases {
                                      "TYPE" "TESTING_ITEM_TYPE"
                                      }
                            }))))
         (testing "a normal case"
                  (is (=
                        {:money-bags {
                                      :money 38.3
                                      }
                         :how-many {
                                    :so "so"
                                    :many "many"
                                    }
                         :gnomes 17
                         :hedge :tall
                         :garden true
                         :yard false
                         :clogged "drain"
                         :abc {:rags [{:to "riches"
                                       :years 11}
                                      {:to "ashes"
                                       :years 12.2
                                       :pleasant false}]}
                         :pets ["horse" "dog"]}
                        (parse-env-vars
                          {
                           :program-name "testing"
                           :env-vars {"TESTING_MAP_MONEY_BAGS" "money=38.3"
                                      "TESTING_MAP_HOW_MANY" "so=so,many=many"
                                      "TESTING_ITEM_GNOMES" "17"
                                      "TESTING_ITEM_HEDGE" "tall"
                                      "TESTING_FLAG_GARDEN" "true"
                                      "TESTING_FLAG_YARD" "false"

                                      "TESTING_JSON_ABC" "{\"rags\": [{\"to\": \"riches\", \"years\": 11},{\"to\": \"ashes\", \"years\": 12.2, \"pleasant\": false}]}"
                                      "CLOG" "drain"
                                      "TESTING_LIST_PETS" "horse,dog"}
                           :transforms {
                                        :gnomes (:int transforms)
                                        :money-bags (:float transforms)
                                        :hedge keyword
                                        }
                           :aliases
                           {
                            "CLOG" "TESTING_ITEM_CLOGGED"
                            }})))))

(deftest test-arg-parser
         (testing "that no arguments doesn't fail."
                  (is (empty? (parse-args
                                []))))
         (testing "that enable and disable works, and one overrides the other"
                  (is (=
                        {:garden true}
                        (parse-args {:args ["--enable-garden"]})))
                  (is (=
                        {:garden false}
                        (parse-args {:args ["--disable-garden"]})))
                  (is (=
                        {:garden false}
                        (parse-args {:args ["--enable-garden" "--disable-garden"]}))))
         (testing "normal case"
                  (is (=
                        {:money-bags {
                                      "money" 38.3
                                      }
                         :gnomes 17
                         :garden true
                         :hedge :tall
                         :pets ["horse" "dog"]
                         :commands ["henceforth"
                                    "and"
                                    "forever"]
                         :abc {:rags [{:to "riches"
                                 :years 11}
                                {:to "ashes"
                                 :years 12}]}
                         }
                         (parse-args {:args ["--json-abc"
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
                                                  }}))))
         (testing "reset add"
                  (is (=
                        {:pets ["cat"]}
                        (parse-args {:args ["--add-pets"
                                     "horse"
                                     "--add-pets"
                                     "dog"
                                     "--reset-pets"
                                     "--add-pets"
                                     "cat"]})))))
