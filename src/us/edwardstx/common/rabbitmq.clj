(ns us.edwardstx.common.rabbitmq
  (:require [com.stuartsierra.component :as component]
            [langohr.channel :as lch]
            [langohr.core :as rmq]))

(defprotocol RabbitmqChannel
  (get-channel [this]))

(defrecord Rabbitmq [conf connection]
  component/Lifecycle

  (start [this]
    (let [rabbit-conf (-> conf :conf :rabbit)
          connection (rmq/connect rabbit-conf)]
      (assoc this :connection connection)))

  (stop [this]
    (rmq/close (:connection this))
    (assoc this :connection nil))

  RabbitmqChannel
  (get-channel [this]
    (lch/open (:connection this))))

(defn new-rabbitmq []
  (component/using
   (map->Rabbitmq {})
   [:conf]))
