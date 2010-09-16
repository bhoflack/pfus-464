(defproject pfus-464 "1.0.0-SNAPSHOT"
  :description "Monitor to detect when we create orphan CQN's"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [log4j/log4j "1.2.16"]
                 [ojdbc/ojdbc "14"]
		 ]
  :dev-dependencies [[autodoc "0.7.1"]
                     [swank-clojure "1.2.1"]]
  :aot [pfus-464.core]
  :main pfus-464.core)