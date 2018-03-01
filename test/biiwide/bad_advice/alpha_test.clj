(ns biiwide.bad-advice.alpha-test
  (:require [clojure.test :refer :all]
            [biiwide.bad-advice.alpha :as bad]))

(deftest basic-test
  (let [f (bad/fn foo [a b]
                  [:assert/before (even? a)
                   :assert/after  (odd? b)
                   :unk abc
                   :before (printf "before (foo %s %s)\n" a b)
                   :after  (printf "after (foo %s %s) => %s\n" a b $)]
                  (/ a b))]
    (is (thrown? AssertionError (f 2 2)))
    (is (thrown? AssertionError (f 3 3)))
    (is (= 2/3 (f 2 3)))
    ))
