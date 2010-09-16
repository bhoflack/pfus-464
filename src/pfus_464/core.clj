(ns pfus-464.core
    (:require (clojure.contrib [logging :as log] [sql :as sql]))
    (:gen-class))

(def *db-info* {:classname "oracle.jdbc.driver.OracleDriver"
                :subprotocol "oracle:thin"
                :subname "@oracle-test.colo.elex.be:1521/XE"
                :user "projplan"
                :password "projplan"
               })

(def new-orphan-cqn-query
     "SELECT cqn.id, qn.SHORTNAME, qn.VERSION, qn.NAME
      FROM completedqn cqn
        LEFT OUTER JOIN qn2proj pql ON pql.cqn_id = cqn.id
        LEFT OUTER JOIN orphan_cqn orphan ON  orphan.id = cqn.id
        JOIN questionnaire qn ON qn.id = cqn.qn_id
                                 AND qn.version = cqn.qn_version
      WHERE pql.PROJNUM IS NULL
        AND orphan.id IS NULL")

(def drop-orphan-table-query
     "DROP TABLE orphan_cqn")

(def create-orphan-table-query
     "CREATE GLOBAL TEMPORARY TABLE orphan_cqn
      ON COMMIT PRESERVE ROWS
      AS SELECT cqn.*
         FROM completedqn cqn
         LEFT OUTER JOIN qn2proj pql ON pql.cqn_id = cqn.id
         WHERE pql.PROJNUM IS NULL")

(defn create-orphan-table []
  "Create the table with the current orphaned CQN's. We use a temporary table
   which must be dropped before creating the snapshot"
  (sql/do-commands drop-orphan-table-query
                   create-orphan-table-query))

(defn cqn-to-str [cqn]
  "Make a human readable string representing a CQN"
  (str (:id cqn)
       " " (:shortname cqn)
       "/" (:version cqn)
       " " (:name cqn)))

(defn new-orphans [map]
  "checks the database for orphans since the start of the program.
  If a CQN orphan is found which is not already in the passed map
  log an info message and add it to the orphans map.

  Return the updated map"
  (let [orphans (atom map)]
    (sql/with-query-results result [new-orphan-cqn-query]
      (doseq [record result]
        (let [orphan-str (cqn-to-str record)
              id         (:id record)]
          (if (not (contains? @orphans id))
            (log/info (str "New orphan detected : " orphan-str)))
          (reset! orphans (assoc @orphans id record)))))
    @orphans
    ))

(def active (atom nil))

(defn monitor-orphans [orphans]
  "Check every second if a new orphan was created"
  (let [n (new-orphans orphans)]
    (Thread/sleep 1000)
    (if active (recur n))))


(defn monitor []
  "Create the database connection, create a snapshot temporary table for the
   existing orphans, and start monitoring."
  (sql/with-connection *db-info*
    (do
      (log/info "Orphan CQN Monitor started")
      (create-orphan-table)
      (try (monitor-orphans {})
           (catch InterruptedException e  (log/info "Monitor interrupted")))
      (log/info "Orphan CQN monitor stopped"))))

(defn start []
  "Start the monitor thread."
  (do
    (reset! active (Thread. monitor "Monitor"))
    (.start @active)))

(defn stop []
  "Stop the monitor thread."
  (let [t @active]
    (.interrupt t)))

(defn -main []
  (println "Orphan Completed Questionnaire Monitor")
  (log/info "Testing the logger")
  (start)
)




