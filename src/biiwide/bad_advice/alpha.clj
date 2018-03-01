(ns biiwide.bad-advice.alpha
  (:refer-clojure :exclude [fn])
  (:require [clojure.core :as c]
            [clojure.tools.logging :as log]))


(defonce ^:private ADVICE
  (atom {}))


(defn register-advice-macro
  [advice-key macro]
  {:pre [(keyword? advice-key)
         (fn? macro)]}
  (swap! ADVICE assoc advice-key macro)
  advice-key)


(defn- splitup-arity
  [arity]
  (let [[argv & tail] arity]
    (cons argv
          (loop [advice '()
                 body tail]
            (let [[a b & more] body]
              (if (and (keyword? a) (some? b))
                (recur (cons [a b] advice)
                       more)
                [advice body]))))))


(defn- advise-with
  [body [advice-type form]]
  (if-some [adv-mac (get @ADVICE advice-type)]
    (adv-mac form body)
    (do (log/warnf "Unrecognized Advice: [%s %s]"
                   advice-type form)
        body)))


(defmacro fn
  [& fn-args]
  (let [exp-fn (macroexpand-1 (cons `c/fn fn-args))
        [head arities] (split-with (complement seq?) exp-fn)]
    (concat head
            (for [arity arities]
              (let [[argv advs body] (splitup-arity arity)]
                (list argv (reduce advise-with
                                   (cons `do body)
                                   advs)))))))


(defmacro defadvice
  [k binding body]
  {:pre [(= 2 (count binding))]}
  `(register-advice-macro
     ~k
     (c/fn ~binding ~body)))


(defadvice :before
  [advice body]
  `(do ~advice ~body))


(defadvice :after
  [advice body]
  `(let [~'$ ~body]
     ~advice
     ~'$))


(defadvice :finally
  [advice body]
  `(try ~body
     (finally ~advice)))


(defadvice :assert/pre
  [advice body]
  `(do (assert ~advice)
       ~body))


(defadvice :assert/post
  [advice body]
  `(let [~'$ body]
     (assert ~advice)
     ~'$))


(defadvice :log/before
  [advice body]
  `(do (log/logf ~@advice)
       ~body))


(defadvice :log/after
  [advice body]
  `(let [~'$ ~body]
     (log/logf ~@advice)
     ~body))

