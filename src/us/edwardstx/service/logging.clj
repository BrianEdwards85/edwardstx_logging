(ns us.edwardstx.service.logging
  (:require [config.core :refer [env]]
            [manifold.deferred :as d]
            [com.stuartsierra.component :as component]
            [us.edwardstx.service.logging.exchange :refer [new-rabbit-exchange]]
            [us.edwardstx.service.logging.orchestrator :refer [new-orchestrator]]
            [us.edwardstx.common.rabbitmq :refer [new-rabbitmq]]
            [us.edwardstx.common.conf :refer [new-conf]]
            [us.edwardstx.common.token :refer [new-token]]
            [us.edwardstx.common.keys :refer [new-keys]]
            [us.edwardstx.common.db :refer [new-database]])
  (:gen-class)
  )

(defonce system (atom {}))

(defn init-system [env]
  (component/system-map
   :keys (new-keys env)
   :token (new-token env)
   :conf (new-conf env)
   :db (new-database)
   :rabbitmq (new-rabbitmq)
   :exchange (new-rabbit-exchange)
   :orchestrator (new-orchestrator)
   ))

(defn -main [& args]
  (let [semaphore (d/deferred)]
    (reset! system (init-system env))

    (swap! system component/start)

    (deref semaphore)

    (component/stop @system)

    (shutdown-agents)
    ))
