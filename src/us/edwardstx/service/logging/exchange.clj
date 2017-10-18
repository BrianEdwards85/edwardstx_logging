(ns us.edwardstx.service.logging.exchange
  (:require [com.stuartsierra.component :as component]
            [us.edwardstx.common.rabbitmq :refer [get-channel] :as rabbitmq]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [langohr.basic :as lb]
            [langohr.core :as rmq]
            [langohr.exchange  :as le]
            [langohr.queue     :as lq]
            [langohr.channel   :as lch]
            [langohr.consumers :as lc]))

(defn rcv-msg [stream ch {:keys [delivery-tag message-id content-encoding] :as meta} ^bytes payload]
  (let [body (String. payload content-encoding)
        ack #(lb/ack ch delivery-tag)]
    (->
     (s/put! stream (assoc meta :body body :ack ack))
     (d/chain #(if (not %) (lb/nack ch delivery-tag false true)))
     )))

(defn consume [this f]
  (s/consume f (:stream this)))

(defrecord RabbitExchange [rabbitmq conf channel exchange queue stream]
  component/Lifecycle

  (start [this]
    (let [channel (get-channel rabbitmq)
          exchange-name (-> conf :conf :logging :exchange)
          exchange (le/declare channel exchange-name "topic" {:durable true :auto-delete false})
          queue-name (-> conf :conf :logging :queue)
          queue (lq/declare channel queue-name {:exclusive false :auto-delete false})
          stream (s/stream 20)]
      (lq/bind channel queue-name exchange-name {:routing-key "#"})
      (lc/subscribe channel queue-name (partial rcv-msg stream) {:auto-ack false})
      (assoc this
             :stream stream
             :channel channel
             :exchange exchange
             :queue queue)))

  (stop [this]
    (rmq/close channel)
    (s/close! stream)
    (assoc this
           :stream nil
           :channel nil
           :exchange nil
           :queue nil)))

(defn new-rabbit-exchange []
  (component/using
   (map->RabbitExchange {})
   [:rabbitmq :conf]))
