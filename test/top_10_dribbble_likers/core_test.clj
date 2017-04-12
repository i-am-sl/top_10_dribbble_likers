(ns top-10-dribbble-likers.core-test
  (:require [clojure.test :refer :all]
            [top-10-dribbble-likers.core :as core :refer :all]))

(deftest a-test
  (testing "~100 http queries"
    (is
      (do
        (println "process started:" "\n")
        (let [{:keys [top10_users]} (core/top10_dribbble_likers 155719)]
          (println "result:" "\n" top10_users)
          (not (nil? top10_users)))
        ))))
