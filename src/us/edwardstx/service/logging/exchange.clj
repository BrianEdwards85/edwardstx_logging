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

(defn rcv-msg [stream ch {:keys [delivery-tag message-id] :as meta} ^bytes payload]
  (let [body (String. payload)
        ack #(lb/ack ch delivery-tag)]
    (->
     (s/put! stream (assoc meta :body body :ack ack))
     (d/chain #(if (not %) (lb/nack ch delivery-tag false true)))
     )))

(defn create-queue [rabbitmq key]
  (let [channel (get-channel rabbitmq)
        exchange-name (get-in rabbitmq [:conf :conf key :exchange])
        queue-name (get-in rabbitmq [:conf :conf key :queue])
        stream (s/stream 20)]
    (s/on-closed stream #(rmq/close channel))
    (le/declare channel exchange-name "topic" {:durable true :auto-delete false})
    (lq/declare channel queue-name {:exclusive false :auto-delete false :durable true :arguments {"x-message-ttl" 14400000}})
    (lq/bind channel queue-name exchange-name {:routing-key "#"})
    (lc/subscribe channel queue-name (partial rcv-msg stream) {:auto-ack false})
    stream)
  )

