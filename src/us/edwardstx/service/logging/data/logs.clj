(ns us.edwardstx.service.logging.data.logs
  (:require [hugsql.core :as hugsql]
            [hugsql.adapter.clojure-java-jdbc :as adapter]
            [us.edwardstx.common.db :refer [get-connection] :as db]
            [manifold.deferred :as d]))

(hugsql/def-db-fns "sql/logs.sql"  {:adapter (adapter/hugsql-adapter-clojure-java-jdbc)})

(defn insert-log [db l]
  (d/future
    (insert-log-sql (get-connection db) l)))
