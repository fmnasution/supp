(ns supp.url)

;; ================================================================
;; url
;; ================================================================

(defn register-url
  [host-url]
  (str host-url "/user"))

(defn reset-token-url
  [host-url username]
  (str host-url "/" username "/reset"))

(defn target-user-url
  [host-url username]
  (str host-url "/" username))
