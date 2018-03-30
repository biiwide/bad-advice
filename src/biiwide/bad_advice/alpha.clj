(ns biiwide.bad-advice.alpha
  (:refer-clojure :exclude [defn fn])
  (:require [clojure.core :as c]))


(c/defonce ^:private ADVICE-MACROS
  (atom {}))


(c/defn- set-doc-string
  [x doc-string]
  (vary-meta x assoc :doc doc-string))


(c/defn register-advice-macro!
  "Registers an advice macro for use."
  [advice-key doc-string macro]
  {:pre [(keyword? advice-key)
         (fn? macro)]}
  (swap! ADVICE-MACROS assoc advice-key
         (set-doc-string macro doc-string))
  advice-key)


(c/defn unregister-advice-macro!
  "Removes advice from the registry."
  [advice-key]
  {:pre [(keyword? advice-key)]}
  (swap! ADVICE-MACROS dissoc advice-key)
  nil)


(c/defn advice-macro-doc
  "Returns the documentation for an advice keyword."
  [advice]
  (cond (keyword? advice) (recur (get @ADVICE-MACROS advice))
        (fn? advice) (:doc (meta advice))))


(c/defn describe-advice-macros
  "Returns a map of all advice keywords and their documentation."
  []
  (reduce-kv (c/fn [result k mac]
               (assoc result k (advice-macro-doc mac)))
             {} @ADVICE-MACROS))


(c/defn- advise-with
  [body [advice-type form]]
  (if-some [adv-mac (get @ADVICE-MACROS advice-type)]
    (adv-mac form body)
    (do (binding [*out* *err*]
          (printf "WARNING: Unrecognized Advice [%s %s]\n"
                  advice-type form))
        body)))


(c/defmacro advise
  "Wraps a form with advise.
Advice is proviced in a vector.  The same type
of advice can be used multiple times.

(let [a 123
      b 456]
  (advise [:after  (println \"Result:\" $)
           :before (println \"A:\" a)
           :before (println \"B:\" b)]
    (+ a b)))"
  [advice-vec & body]
  (assert (vector? advice-vec))
  (assert (even? (count advice-vec)))
  (let [adv-pairs (reverse (partition 2 advice-vec))]
    (reduce advise-with (cons `do body) adv-pairs)))


(defn- parse-fn
  [fn-form]
  (if (seq (filter vector? fn-form))
    (let [not-vec? (complement vector?)]
      {:prelude (take-while not-vec? fn-form)
       :arities (list (drop-while not-vec? fn-form))})
    (let [not-list? (complement list?)]
      {:prelude (take-while not-list? fn-form)
       :arities (drop-while not-list? fn-form)})))


(defn- advise-fn
  [f badfn-form]
  (let [{:keys [prelude arities]} (parse-fn badfn-form)]
    (concat (cons f prelude)
            (for [arity arities]
              (let [[args & body] arity]
                (if (vector? (first body))
                  (list args (cons `advise body))
                  arity))))))

(c/defmacro fn
  "Constructs an advised function.
Just like 'clojure.core/fn, with added support for
an optional advice vector in each arity.

(fn fn-name [a b]
  [:after  (printf \"After:  %s <= (fn-name %s %s)\\n\" $ a b)
   :before (println \"-------\")
   :before (printf \"Before: (fn-name %s %s)\\n\" a b)]
  (/a b))"
  [& fn-form]
  (advise-fn `c/fn fn-form))


(c/defmacro defn
  "Define an advised function.
Just like 'clojure.core/defn, with added support for
an optional advice vector in each arity."
  [& defn-form]
  (advise-fn `c/defn defn-form))
  

(c/defn- add-doc
  [x doc-string]
  (vary-meta x assoc :doc doc-string))


(c/defmacro defadvice
  [k doc-string binding body]
  {:pre [(= 2 (count binding))]}
  `(register-advice-macro!
     ~k ~doc-string
     (c/fn ~binding ~body)))


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


(defadvice :exception
  "Evaluate advice when an exception is thrown.
The exception will be bound as *e.
This only catches Exceptions.  Errors and other
Throwables are ignored."
  [advice body]
  
  `(try ~body
     (catch Exception ~'*e
       ~advice
       (throw ~'*e))))



(c/defmacro extend-advice
  "Extend existing advice.
Allows for futher transformation of existing advice
while maintaining the same relation to the form
being extended.

Documentation will be created by prepending the
new doc-string to the doc-string for the parent
advice.

Example:  Extend both :before and :after advice,
          creating :assert/before and :assert/after,
          which will perform assertions.

(extend-advice :assert
   \"Perform an assertion.\"
   [:after :before]
   (fn [advice]
     `(assert ~@advice)))"
  [namespace-keyword doc-prefix advices advicefn]
  (let [afn-sym  (gensym "afn")
        adv-sym  (gensym "adv")
        body-sym (gensym "body")]
    `(let [~afn-sym ~advicefn]
       ~@(for [parent-key advices]
           (let [parent-doc (advice-macro-doc parent-key)
                 child-key  (keyword (name namespace-keyword)
                                     (name parent-key))]
             `(defadvice ~child-key
                ~(format "%s\n%s" doc-prefix parent-doc)
                [~adv-sym ~body-sym]
                `(advise ~[~parent-key (~afn-sym ~adv-sym)]
                         ~~body-sym)))))))


(extend-advice :assert
   "Perform an assertion."
   [:after :before]
   (c/fn [advice]
     (if (vector? advice)
       `(assert ~@advice)
       `(assert ~advice))))


(extend-advice :clojure.tools.logging
  "Log a formatted message using clojure.tools.logging/logf."
  [:after :before :exception :finally]
  (c/fn [advice]
    (list* 'clojure.tools.logging/logf advice)))

