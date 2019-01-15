(ns arcadia.vscode.core
  (:require [cljs.nodejs :as nodejs]
            [vscode.util :as u]
            [arcadia.vscode.repl :as avr]))

(nodejs/enable-util-print!)

(defn command
  []
  (u/info-message "Hello Arcadia"))

(defn push-all-subs
  [context f]
  (doseq [disp (f)]
    (u/push-subscription! context disp)))

(defn activate
  [context]
  (println "activating vscode-arcadia")
  (push-all-subs context avr/activate-repl))

(defn deactivate
  []
  (println "vscode-arcadia deactivated"))

(defn -main
  []
  (println "vscode-arcadia loaded"))

(u/export! {:activate activate})

(set! *main-cli-fn* -main)
