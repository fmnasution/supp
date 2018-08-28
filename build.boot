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
                 [samestep/boot-refresh "0.1.0" :scope "test"]
                 [adzerk/bootlaces "0.1.13" :scope "test"]])

(require
 '[samestep.boot-refresh :refer [refresh]]
 '[adzerk.bootlaces :refer [bootlaces! build-jar push-snapshot push-release]])

(def +version+
  "0.1.0-SNAPSHOT")

(bootlaces! +version+)

(task-options!
 push {:ensure-branch nil
       :repo-map      {:checksum :warn}}
 pom  {:project     'supp
       :version     +version+
       :description "Client for obs"
       :url         "http://github.com/fmnasution/supp"
       :scm         {:url "http://github.com/fmnasution/supp"}
       :license     {"Eclipse Public License"
                     "http://www.eclipse.org/legal/epl-v10.html"}})

(deftask dev-repl
  []
  (comp
   (repl :server true)
   (watch)
   (refresh)))

(deftask snapshot-to-clojars
  []
  (comp
   (build-jar)
   (push-snapshot)))
