(set-env!
 :source-paths #{"src/"}
 :dependencies '[;; ---- clj ----
                 [org.clojure/clojure "1.10.0-alpha5"]
                 [buddy/buddy-core "1.5.0"]
                 [buddy/buddy-sign "3.0.0"]
                 ;; ---- cljc ----
                 [org.clojure/core.async "0.4.474"]
                 [com.stuartsierra/component "0.3.2"]
                 [cljs-ajax "0.7.4"]
                 ;; ---- cljs ----
                 [org.clojure/clojurescript "1.10.339"]
                 ;; ---- dev ----
                 [samestep/boot-refresh "0.1.0" :scope "test"]])

(require
 '[samestep.boot-refresh :refer [refresh]])

(deftask dev-repl
  []
  (comp
   (repl :server true)
   (watch)
   (refresh)))
