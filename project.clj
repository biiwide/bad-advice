(defproject biiwide/bad-advice "0.0.3"

  :description "This is probably a bad idea, but..."

  :url "https://github.com/biiwide/bad-advice"

  :license {:name "Eclipse Public License 2.0"
            :url "https://www.eclipse.org/legal/epl-2.0"}

  :dependencies [[org.clojure/clojure "1.8.0"]]
  
  :profiles {:dev {:dependencies [[org.clojure/tools.logging "0.4.0"]]
                   }}

  :release-tasks [["vcs" "assert-committed"]
                  ["change" "version" "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "--no-sign"]
                  ["deploy"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]]

  )
