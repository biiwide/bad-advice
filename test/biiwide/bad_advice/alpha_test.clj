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


(deftest test-destructuring
  (let [vresult (volatile! nil)
        f (bad/fn foo
                  ([] "Bye")
                  ([{:keys [a b] :as m}]
                    [:after (vreset! vresult $)]
                    "Hi"))]
    (= (f {}) @vresult)))

(bad/defn defn-single
  [abc]
  [:before (println abc)
   :after  (printf "(f %s) => %s\n" abc $)])


(bad/defn defn-multi
  ([] 0)
  ([one]
    [:before (println one)]
    one)
  ([one two]
    [:before (printf "args: %s %s\n" one two)
     :after  (printf "(f %s %s) => %s\n" one two $)]
    (list one two)))

