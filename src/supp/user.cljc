(ns supp.user
  (:require
   [ajax.core :as jx]
   [supp.url :as url]
   [supp.option :as opt]
   #?@(:clj  [[clojure.spec.alpha :as s]
              [clojure.core.async :as a]]
       :cljs [[cljs.spec.alpha :as s]
              [cljs.core.async :as a]])))

;; ================================================================
;; protocols
;; ================================================================

(defprotocol IUserClient
  (-register [this username password option])
  (-erase [this username password option])
  (-authenticate [this credentials option])
  (-reset-password [this username password option]))

;; ================================================================
;; user client
;; ================================================================

(defrecord UserClient [host-url content-type]
  IUserClient
  (-register [this username password {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/POST (url/register-url host-url)
               (-> {}
                   (opt/specify-format content-type)
                   (assoc :params {:username username
                                   :password password})
                   (opt/attach-callback resp-chan)))
      resp-chan))
  (-erase [this username password {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/DELETE (url/target-user-url host-url username)
                 (-> {}
                     (opt/specify-format content-type)
                     (opt/inject-http-basic-auth username password)
                     (opt/attach-callback resp-chan)))
      resp-chan))
  (-authenticate [this {:keys [username] :as credentials} {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/POST (url/target-user-url host-url username)
               (-> {}
                   (opt/specify-format content-type)
                   (assoc :params credentials)
                   (opt/attach-callback resp-chan)))
      resp-chan))
  (-reset-password [this username password {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/PUT (url/target-user-url host-url username)
              (-> {}
                  (opt/specify-format content-type)
                  (opt/inject-http-basic-auth username password)
                  (assoc :params {:password password})
                  (opt/attach-callback resp-chan)))
      resp-chan)))

(defn make-user-client
  [option]
  (-> option
      (select-keys [:host-url :content-type])
      (map->UserClient)))

(defn register
  ([user-client username password option]
   (-register user-client username password option))
  ([user-client username password]
   (register user-client username password {})))

(defn erase
  ([user-client username password option]
   (-erase user-client username password option))
  ([user-client username password]
   (erase user-client username password {})))

(defn authenticate
  ([user-client credentials option]
   (-authenticate user-client credentials option))
  ([user-client credentials]
   (authenticate user-client credentials {})))

(defn reset-password
  ([user-client username password option]
   (-reset-password user-client username password option))
  ([user-client username password]
   (reset-password user-client username password {})))
