(ns supp.option
  (:require
   #?@(:clj  [[clojure.core.async :as a]]
       :cljs [[goog.crypt.base64 :refer [encodeString]]
              [cljs.core.async :as a]]))
  #?(:clj
     (:import
      [java.util Base64])))

;; ================================================================
;; format
;; ================================================================

(defn specify-format
  [option content-type]
  (assoc option
         :format          content-type
         :response-format content-type))

;; ================================================================
;; authorization
;; ================================================================

(defn- ->base64
  [input]
  #?(:clj  (.encodeToString (Base64/getEncoder) (.getBytes input))
     :cljs (encodeString input)))

(defn- inject-authorization
  [option scheme token]
  (let [value (str scheme " " token)]
    (assoc-in option [:headers "authorization"] value)))

(defn inject-http-basic-auth
  [option username password]
  (let [token (->base64 (str username ":" password))]
    (inject-authorization option "Basic" token)))

(defn inject-forget-token
  [option token]
  (inject-authorization option "ObsForget" token))

(defn inject-reset-token
  [option token]
  (inject-authorization option "ObsReset" token))

;; ================================================================
;; callback
;; ================================================================

(defn- wrap-response
  [kind response]
  {:response/kind kind
   :response/data response})

(defn attach-callback
  [option chan]
  (assoc option
         :handler       (comp (partial a/put! chan)
                           (partial wrap-response :success))
         :error-handler (comp (partial a/put! chan)
                           (partial wrap-response :fail))
         #?@(:cljs [:progress-handler (comp (partial a/put! chan)
                                         (partial wrap-response :progress))])))
