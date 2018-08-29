(ns supp.app
  (:require
   [clojure.spec.alpha :as s]
   [clojure.core.async :as a]
   [com.stuartsierra.component :as c]
   [buddy.core.keys :as bdyks]
   [buddy.sign.jwt :as bdysgnjwt]
   [ajax.core :as jx]
   [supp.url :as url]
   [supp.option :as opt]))

;; ================================================================
;; app client spec
;; ================================================================

(s/def ::host-url
  string?)

(s/def ::content-type
  #{:transit :json :url :ring :raw :text :detect})

(s/def ::secret
  (s/nilable string?))

(s/def ::size
  pos-int?)

(s/def ::sha-app-client-config
  (s/keys :req-un [::host-url ::content-type ::size]
          :opt-un [::secret]))

(s/def ::algorithm
  #{:es256 :es512 :ps256 :ps512 :rs256 :rs512})

(s/def ::public-key-path
  string?)

(s/def ::asymetric-app-client-config
  (s/keys :req-un [::host-url ::content-type ::algorithm ::public-key-path]))

;; ================================================================
;; protocols
;; ================================================================

(defprotocol IAppClient
  (-reset-token [this username option])
  (-reset-password [this token username password option])
  (-secret-key [this])
  (-algorithm [this]))

;; ================================================================
;; token
;; ================================================================

(defn- forget-token
  [username secret algorithm]
  (bdysgnjwt/sign {:username username} secret {:alg algorithm}))

;; ================================================================
;; sha app client
;; ================================================================

(defn- sha-kind
  [size]
  (if (= 256 size)
    :hs256
    :hs512))

(defrecord SHAAppClient [host-url content-type secret size]
  IAppClient
  (-reset-token [this username {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/POST (url/reset-token-url host-url username)
               (-> {}
                   (opt/specify-format content-type)
                   (opt/attach-callback resp-chan)
                   (opt/inject-forget-token
                    (forget-token username secret (sha-kind size)))))
      resp-chan))
  (-reset-password [this token username password {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/PUT (url/target-user-url host-url username)
              (-> {}
                  (opt/specify-format content-type)
                  (opt/attach-callback resp-chan)
                  (assoc :params {:password password})
                  (opt/inject-reset-token token)))
      resp-chan))
  (-secret-key [this]
    secret)
  (-algorithm [this]
    (sha-kind size)))

(defn make-sha-app-client
  [config]
  (-> (s/assert ::sha-app-client-config config)
      (select-keys [:host-url :content-type :secret :size])
      (map->SHAAppClient)))

;; ================================================================
;; asymetric app client
;; ================================================================

(defrecord AsymetricAppClient [host-url
                               content-type
                               algorithm
                               public-key-path
                               public-key]
  c/Lifecycle
  (start [this]
    (if (some? public-key)
      this
      (let [public-key (bdyks/public-key public-key-path)]
        (assoc this :public-key public-key))))
  (stop [this]
    (if (nil? public-key)
      this
      (assoc this :public-key nil)))

  IAppClient
  (-reset-token [this username {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/POST (url/reset-token-url host-url username)
               (-> {}
                   (opt/specify-format content-type)
                   (opt/attach-callback resp-chan)
                   (opt/inject-forget-token
                    (forget-token username public-key algorithm))))
      resp-chan))
  (-reset-password [this token username password {:keys [chan]}]
    (let [resp-chan (or chan (a/chan))]
      (jx/PUT (url/target-user-url host-url username)
              (-> {}
                  (opt/specify-format content-type)
                  (opt/attach-callback resp-chan)
                  (assoc :params {:password password})
                  (opt/inject-reset-token token)))
      resp-chan))
  (-secret-key [this]
    public-key)
  (-algorithm [this]
    algorithm))

(defn make-asymetric-app-client
  [config]
  (-> (s/assert ::asymetric-app-client-config config)
      (select-keys [:host-url :content-type :algorithm :public-key-path])
      (map->AsymetricAppClient)))

(defn reset-token
  ([app-client username option]
   (-reset-token app-client username option))
  ([app-client username]
   (reset-token app-client username {})))

(defn reset-password
  ([app-client token username password option]
   (-reset-password app-client token username password option))
  ([app-client token username password]
   (reset-password app-client token username password {})))

(defn secret-key
  [app-client]
  (-secret-key app-client))

(defn algorithm
  [app-client]
  (-algorithm app-client))
