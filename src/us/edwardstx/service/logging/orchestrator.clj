(ns us.edwardstx.service.logging.orchestrator
  (:require [com.stuartsierra.component :as component]
            [clojure.data.json :as json]
            [clj-time.coerce :as c]
            [manifold.deferred :as d]
            [manifold.stream :as s]
            [us.edwardstx.service.logging.exchange :as exchange]
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
     (d/catch Exception #(println "Error: " %))
     )
    (catch Exception e (println (str "Opps:" (.getMessage e))))
    )
  )

(defrecord Orchestrator [db exchange]
  component/Lifecycle

  (start [this]
    (exchange/consume exchange (partial insert-log this))
    this)

  (stop [this]
    this)
  )

(defn new-orchestrator []
  (component/using
   (map->Orchestrator {})
   [:db :exchange]))

