(ns twitterapp.core
  (:gen-class)
  (:use
    [twitter.oauth]
    [twitter.callbacks]
    [twitter.callbacks.handlers]
    [twitter.api.restful])
  (:require [monger.core :as mg]
            [monger.collection :as mc]
            [monger.query :refer :all]
            [monger.conversion :refer [from-db-object]]
            [monger.operators :refer :all]
            [clojure.edn :as edn])
  (:import [com.mongodb MongoOptions ServerAddress]
      (twitter.callbacks.protocols SyncSingleCallback)))

(def config (edn/read-string (slurp "config.clj")))

(def my-creds (make-oauth-creds (:oauth_consumer_key config)
    (:oauth_consumer_secret config)
    (:oauth_access_token config)
    (:oauth_access_secret config)
))

(defn- trusted-user? [message] (
  some #(= (:sender_screen_name message) %) (:trusted_users config)
))

(defn- get-max-id [] (
  :id
  (first
  (let [conn (mg/connect)
        db (mg/get-db conn (:database_name config))
        coll "documents"]
    (with-collection db coll
      (find {})
      (fields [:id])
      (sort (array-map :id -1))
    (limit 1))))
))

(defn- insert-into-db [data] (
  if (seq data) (let [conn (mg/connect)
        db (mg/get-db conn (:database_name config))
        coll "documents"]
  (mc/insert-batch db coll data))
))

(defn- get-next-message [] (
  first
  (let [conn (mg/connect)
       db (mg/get-db conn (:database_name config))
       coll "documents"]
    (with-collection db coll
      (find {:published false})
      (fields [:id :text])
      (sort (array-map :id 1))
    (limit 1)))
))

(defn- update-data [data] (
  let [conn (mg/connect)
       db (mg/get-db conn (:database_name config))
       coll "documents"]
  (mc/update-by-id db coll (:_id data)
    {:published true
     :id (:id data)
     :text (:text data)})
))

(defn -main
  []
  ; Retrieve last message id from db.
  ;   If there isn't one, get all.
  ; Get all messages since last ID.
  ; Add message to DB.
  ; Get irst non-sent update.
  ; post it
  ; mark it as sent.
  (insert-into-db (map #(conj % {:published false})
    (map #(select-keys % [:text :id :_id])
      (filter trusted-user? ((direct-messages :oauth-creds my-creds :params {:since-id (get-max-id)}) :body))
    )
  ))
  (statuses-update :oauth-creds my-creds :params {:status (:text (get-next-message))})
  (update-data (get-next-message))
  (System/exit 0)
)