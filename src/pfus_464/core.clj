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

(def wrong-cqn-query
     "SELECT q2p.projnum, q2p.projvers, q2p.cqn_id, q2p.qn_id, cqn.qn_id
      FROM qn2proj q2p, completedqn cqn
      WHERE q2p.CQN_ID = cqn.ID and cqn.QN_ID <> q2p.qn_id")

(def drop-wrong-cqn-table-query
     "DROP TABLE wrong_questionnaire4")

(def create-wrong-cqn-table-query
     "CREATE GLOBAL TEMPORARY TABLE wrong_questionnaire4
      ON COMMIT PRESERVE ROWS
      AS SELECT cqn.*
         FROM completedqn cqn
           JOIN qn2proj pql on pql.cqn_id = cqn.id
         WHERE cqn.qn_id <> pql.qn_id")

(def drop-orphan-table-query
     "DROP TABLE orphan_cqn4")

(def create-orphan-table-query
     "CREATE GLOBAL TEMPORARY TABLE orphan_cqn4
      ON COMMIT PRESERVE ROWS
      AS SELECT cqn.*
         FROM completedqn cqn
         LEFT OUTER JOIN qn2proj pql ON pql.cqn_id = cqn.id
         WHERE pql.PROJNUM IS NULL")

(defn table-exists? [table]
  "Verify if a table exists"
  (= "1"
     (sql/do-commands
      (str "select count(*)
              from all_tables
              where table_name='" table "'"))))

(defn execute-if-exists [table & commands]
  (when (table-exists? table)
    (sql/do-commands commands)))

(defn create-orphan-table []
  "Create the table with the current orphaned CQN's. We use a temporary table
   which must be dropped before creating the snapshot"
  (do
    (log/info "Creating orphan tables")
    (map (fn [table command] (apply 'execute-if-exists table command))
         '("wrong_questionnaire" "orphan_cqn")
         '(drop-wrong-cqn-table-query drop-orphan-table-query))
    (sql/do-commands create-orphan-table-query
                     create-wrong-cqn-table-query)))

(defn create-wrong-cqn-table []
  "Create the table with the wrong CQN's."
  (sql/do-commands drop-wrong-cqn-table-query
                   create-wrong-cqn-table-query))

(defn cqn-to-str [cqn]
  "Make a human readable string representing a CQN"
  (str (:id cqn)
       " " (:shortname cqn)
       "/" (:version cqn)
       " " (:name cqn)))

(defn new-orphans [map query message]
  "checks the database for orphans since the start of the program.
  If a CQN orphan is found which is not already in the passed map
  log an info message and add it to the orphans map.

  Return the updated map"
  (let [orphans (atom map)]
    (sql/with-query-results result [query]
      (doseq [record result]
        (let [orphan-str (cqn-to-str record)
              id         (:id record)]
          (if (not (contains? @orphans id))
            (log/info (str query orphan-str)))
          (reset! orphans (assoc @orphans id record)))))
    @orphans))

(def active (atom nil))

(defn monitor-new-errors [new-errors-fn errors]
  "Check every second if a new error was created"
  (let [n (new-errors-fn errors)]
    (Thread/sleep 1000)
    (if active
      (recur new-errors-fn n))))

(defn monitor []
  "Create the database connection, create a snapshot temporary table for the
   existing orphans, and start monitoring."
  (sql/with-connection *db-info*
    (do
      (log/info "Orphan CQN Monitor started")
      (create-orphan-table)
      (try (monitor-new-errors #(new-orphans %1 new-orphan-cqn-query "New orphan detected : ") {})
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
  (start))




