# bad-advice

A library for defining and using AOP-style "advice" in functions
and arbitrary pieces of code in Clojure.

This is for my own amusement and is likely a combination of several bad
ideas.  So far this is strictly macro based, and not very dynamic.  You
should probably consider using [Robert Hooke](https://github.com/technomancy/robert-hooke) instead.

## Usage
### Using in a function
The `biiwide.bad-advice.alpha` namespace includes implementations of `fn` and `defn`
which support an optional _"advice"_ vector immediately after the arguments for an
arity.

This implementation favors side-effecting types of advice such as logging,
validation, timing, etc.

```clj
(require '[biiwide.bad-advice.alpha :as bad])

(bad/defn my-bad [a b]
  [:after  (printf "AFTER:  (my-bad %s %s) => %s" a b $)
   :before (printf "BEFORE: (my-bad %s %s)\n" a b)
   :assert/before [(not (zero? b)) "Denominator cannot be zero."]]
  (/ a b))

(my-bad 3 2)
> BEFORE: (my-bad 3 2)
> AFTER:  (my-bad 3 2) => 3/2
3/2

(my-bad 3 0)
> BEFORE: (my-bad 3 0)
AssertionError Assert failed: Denominator cannot be zero.
(not (zero? b))  user/my-bad (form-init3650484142379080757.clj:1)
```

### Describing Advice
```clj
(bad/describe-advice-macros)

{:after         "Evaluated after the body.
                 Binds the result of the body to $.",
 :before        "Evaluated before the body.",
 :exception     "Evaluate advice when an exception is thrown.
                 The exception will be bound as *e.
                 This only catches Exceptions.  Errors and other
                 Throwables are ignored.",
 :finally       "Always evaluated after the body, even if an exception is thrown.
                 Because this evaluates in a (finally) block, it does not
                 have access to the returned value or any caught exceptions.",
 :assert/after  "Perform an assertion.
                 Evaluated after the body.
                 Binds the result of the body to $.",
 :assert/before "Perform an assertion.
                 Evaluated before the body."
 }
```

### Defining Advice
The catalog of available advice is open to extension.  Custom advice can
be defined and used at any time.

```clj
(bad/defadvice :bad/timing
  "Log elapsed time."
  [advice body]
  `(let [begin# (System/currentTimeMillis)
         result# ~body
	 end#   (System/currentTimeMillis)]
    (printf ~advice (- end# beginE))
    result#))

(bad/advise [:bad/timing "Slept for %sms\n"]
  (Thread/sleep 1234)
  "wakeup")
> Slept for 1234ms
"wakeup"
```

## License

Copyright © 2018 Theodore Cushman

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
