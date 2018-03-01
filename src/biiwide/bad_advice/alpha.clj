(ns biiwide.bad-advice.alpha
  (:refer-clojure :exclude [fn])
  (:require [clojure.core :as c]))


(defonce ^:private ADVICE-MACROS
  (atom {}))


(defn register-advice-macro
  "Registers an advice macro for use."
  [advice-key macro]
  {:pre [(keyword? advice-key)
         (fn? macro)]}
  (swap! ADVICE-MACROS assoc advice-key macro)
  advice-key)


(defn unregister-advice-macro
  "Removes advice from the registry."
  [advice-key]
  {:pre [(keyword? advice-key)]}
  (swap! ADVICE-MACROS dissoc advice-key)
  nil)


(defn describe-advice-macros
  "Returns a map of all advice keywords and their documentation."
  []
  (reduce-kv (c/fn [result k mac]
               (assoc result k (:doc (meta mac))))
             {} @ADVICE-MACROS))


(defn- advise-with
  [body [advice-type form]]
  (if-some [adv-mac (get @ADVICE-MACROS advice-type)]
    (adv-mac form body)
    (do (binding [*out* *err*]
          (printf "WARNING: Unrecognized Advice [%s %s]\n"
                  advice-type form))
        body)))


(defmacro advise
  "Wraps a form with advise"
  [advice-vec & body]
  (assert (vector? advice-vec))
  (assert (even? (count advice-vec)))
  (let [adv-pairs (reverse (partition 2 advice-vec))]
    (reduce advise-with (cons `do body) adv-pairs)))


(defmacro fn
  "Constructs an advised function.
(fn fn-name [a b]
  [:after  (printf \"After:  %s <= (fn-name %s %s)\\n\" $ a b)
   :before (println \"-------\")
   :before (printf \"Before: (fn-name %s %s)\\n\" a b)]
  (/a b))"
  [& fn-args]
  (let [exp-fn (macroexpand-1 (cons `c/fn fn-args))
        [head arities] (split-with (complement seq?) exp-fn)]
    (concat head
            (for [arity arities]
              (let [[argv & body] arity
                    [adv-vec & body] (if (and (vector? (first body))
                                              (not (empty? (next body))))
                                       body
                                       (cons [] body))]
                (list argv `(advise ~adv-vec ~@body)))))))


(defmacro defadvice
  [k doc-string binding body]
  {:pre [(= 2 (count binding))]}
  `(register-advice-macro
     ~k
     (vary-meta (c/fn ~binding ~body)
                assoc :doc ~doc-string)))


(defadvice :before
  "Evaluated before the body."
  [advice body]
  `(do ~advice ~body))


(defadvice :after
  "Evaluated after the body.
Binds the result of the body to $."
  [advice body]
  `(let [~'$ ~body]
     ~advice
     ~'$))


(defadvice :finally
  "Always evaluated after the body, even if an exception is thrown.
Because this evaluates in a (finally) block, it does not
have access to the returned value or any caught exceptions."
  [advice body]
  `(try ~body
     (finally ~advice)))


(defadvice :assert/before
  "Perform an assertion before the body is evaluated."
  [advice body]
  `(do ~(if (vector? advice)
          `(assert ~@advice)
          `(assert ~advice))
       ~body))


(defadvice :assert/after
  "Perform an assertion after the body is evaluated.
The result is bound to $."
  [advice body]
  `(let [~'$ ~body]
     ~(if (vector? advice)
        `(assert ~@advice)
        `(assert ~advice))
     ~'$))

