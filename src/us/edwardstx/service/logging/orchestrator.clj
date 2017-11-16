(ns us.edwardstx.service.logging.orchestrator
  (:require [com.stuartsierra.component :as component]
            [clojure.data.json :as json]
            [clj-time.coerce :as c]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [us.edwardstx.common.uuid :refer [uuid]]
            [us.edwardstx.service.logging.exchange :as exchange]
            [us.edwardstx.service.logging.data.events :as events]
            [us.edwardstx.service.logging.data.logs :as l]))

(defn parse-log [{:keys [message-id app-id routing-key body] :as log}]
  (let [body (json/read-str body :key-fn keyword)
        msg (:message body)
        ts (-> :body :timeMillis c/from-long c/to-timestamp)]
     (hash-map :id message-id
               :time_stamp ts
               :message msg
               :routing_key routing-key
               :msg (json/write-str
                     (assoc body
                            :app-id app-id
                            :message-id message-id
                            :routing-key routing-key)))))

(defn insert-log [o {:keys [message-id ack] :as log}]
  (try
    (->
     (l/insert-log (:db o) (parse-log log))
     (d/chain (fn [_] (ack)))
     (d/catch Exception #(println "Error: " %)))
    (catch Exception e (println (str "Opps:" (.getMessage e))))))

(defn parse-event [{:keys [message-id app-id routing-key body]}]
  (hash-map
   :id (or message-id (uuid))
   :event_key routing-key
   :app_id app-id
   :event_obj body))

(defn insert-event [o {:keys [message-id ack] :as event}]
  (try
    (->
     (events/insert-event (:db o) (parse-event event))
     (d/chain (fn [_] (ack)))
     (d/catch Exception #(println "Error: " %)))
    (catch Exception e (println (str "Opps:" (.getMessage e))))))

(defrecord Orchestrator [db rabbitmq log-stream event-stream]
  component/Lifecycle

  (start [this]
    (let [log-stream (exchange/create-queue rabbitmq :logging)
          event-stream (exchange/create-queue rabbitmq :events)]
      (s/consume (partial insert-log this) log-stream)
      (s/consume (partial insert-event this) event-stream)
      (assoc this :log-stream log-stream :event-stream event-stream)))

  (stop [this]
    (s/close! log-stream)
    (s/close! event-stream)
    (assoc this :log-stream nil :events-stream nil)
    )
  )

(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:db :rabbitmq]))

