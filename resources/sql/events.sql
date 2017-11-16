-- sql/events.sql

-- :name insert-event-sql :! :n
INSERT INTO logging.events(id, event_key, app_id, event_obj)
     VALUES (:id, :event_key, :app_id, CAST(:event_obj as jsonb));
