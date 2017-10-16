(ns us.edwardstx.service.logging.excange
  (:require [com.stuartsierra.component :as component]
            [us.edwardstx.common.db :refer [get-channel] :as db]
            [langohr.core :as rmq]
            [langohr.exchange  :as le]
            [langohr.queue     :as lq]
            [langohr.channel   :as lch]
            [langohr.consumers :as lc]))

(defrecord RabbitExchange [rabbitmq conf channel exchange queue]
  component/Lifecycle

  (start [this]
    (let [channel (get-channel rabbitmq)
          exchange-name (-> conf :conf :logging :exchange)
          exchange (le/declare channel exchange-name "topic" {:durable true :auto-delete true})
          queue-name (-> conf :conf :logging :queue)
          queue (lq/declare channel queue-name {:exclusive false :auto-delete false})]
      (lc/bind channel queue-name "#")
      (assoc this
             :channel channel
             :exchange exchange
             :queue queue)))

  (stop [this]
    (rmq/close channel)
    (assoc this
           :channel nil
           :exchange nil
           :queue nil)))

(defn new-rabbit-excange []
  (component/using
   (new->RabbitExchange {})
   [:rabbitmq :conf]))
