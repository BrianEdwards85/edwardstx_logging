-- sql/logs.sql

-- :name insert-log-sql :! :n
INSERT INTO logging.logs(id, time_stamp, message, routing_key, msg)
     VALUES (:id, :time_stamp, :message, :routing_key,CAST(:msg AS jsonb));
