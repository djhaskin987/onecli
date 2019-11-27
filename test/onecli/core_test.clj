(ns onecli.core-test
  (:require [clojure.test :refer :all]
            [onecli.core :refer :all]))

(deftest test-empty-parser
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
                        {:gnomes 17
                         :garden true
                         :hedge :tall
                         :pets ["horse" "dog"]
                         :unparsed ["henceforth"
                                    "and"
                                    "forever"]}
                         (parse-args ["-g"
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
                                     :transforms {:gnomes
                                                  #(java.lang.Integer/parseInt %)
                                                  :hedge keyword}))))
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
