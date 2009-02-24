;; Copyright (c) James Reeves. All rights reserved.
;; The use and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php) which
;; can be found in the file epl-v10.html at the root of this distribution. By
;; using this software in any fashion, you are agreeing to be bound by the
;; terms of this license. You must not remove this notice, or any other, from
;; this software.

;; compojure.http.request:
;;
;; Functions for pulling useful data out of a HTTP request map.

(ns compojure.http.request
  (:use compojure.control)
  (:use clojure.contrib.duck-streams)
  (:use clojure.contrib.str-utils)
  (:import java.net.URLDecoder))

(defn- assoc-vec
  "Associate a key with a value. If the key already exists in the map, create a
  vector of values."
  [map key val]
  (assoc map key
    (if-let [cur (map key)]
      (if (vector? cur)
        (conj cur val)
        [cur val])
      val)))

(defn parse-params
  "Parse parameters from a string into a map."
  [param-str]
  (reduce
    (fn [param-map s]
      (if (= s "")
        param-map
        (let [[key val] (re-split #"=" s)]
          (assoc-vec param-map
            (keyword (URLDecoder/decode key))
            (URLDecoder/decode val)))))
    {}
    (re-split #"&" param-str)))

(defn get-query-params
  "Parse parameters from the query string."
  [request]
  (if-let [query (request :query-string)]
    (parse-params query)))

(defn get-body-params
  "Parse parameters from the request body."
  [request]
  (if-let [body (request :body)]
    (parse-params (slurp* body))))

(defmacro with-request-bindings
  "Add shortcut bindings for the keys in a request map."
  [request & body]
  `(let [~'request      ~request
         ~'query-params (get-query-params ~request)
         ~'body-params  (get-body-params ~request)
         ~'params       (merge ~'query-params ~'body-params ~'route-params)]
     ~@body))
