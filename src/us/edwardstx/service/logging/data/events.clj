(ns us.edwardstx.service.logging.data.events
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.clojure-java-jdbc :as adapter]
            [us.edwardstx.common.db :refer [get-connection] :as db]
            [manifold.deferred :as d]))

(hugsql/def-db-fns "sql/events.sql" {:adapter (adapter/hugsql-adapter-clojure-java-jdbc)})

(defn insert-event [db e]
  (d/future
    (insert-event-sql (get-connection db) e)))

