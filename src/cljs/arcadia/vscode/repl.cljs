(ns arcadia.vscode.repl
  (:require [clojure.string :as s]
            [vscode.util :as u]))

(def net (js/require "net"))
(def vscode (js/require "vscode"))

(def repl (atom nil))

(def repl-options
  (let [conf (u/get-config "arcadia")]
    {:host (u/get-config conf "replHost")
     :port (u/get-config conf "replPort")}))

(defn handle-input
  [cmd]
  (u/new-promise
   (fn [resolve]
     (let [{:keys [client output]} @repl
           cmd-nl (str cmd "\n")]
       (.append output cmd-nl)
       (.write client cmd-nl)
       (resolve true)))))

(defn parse-data
  [data]
  (-> data 
      (.toString) 
      (.split "\n")
      ((juxt #(.pop %)
             #(.join % "\n")))))

(defn handle-response
  [output data]
  (let [[prompt result] (parse-data data)]
    (.appendLine output result) 
    (.append output prompt)))

(def repl-init
  (quote
   (binding [*warn-on-reflection* false]
     (do
       (println
        (str "\n"
             "; Arcadia REPL"
             "\n"
             "; Clojure " (clojure-version)
             "\n"
             "; Unity "
             (UnityEditorInternal.InternalEditorUtility/GetFullUnityVersion)
             "\n"
             "; Mono "
             (.Invoke
              (.GetMethod
               Mono.Runtime
               "GetDisplayName"
               (enum-or System.Reflection.BindingFlags/NonPublic
                        System.Reflection.BindingFlags/Static))
              nil
              nil)))))))

(defn connect-repl 
  [output host port]
  (let [client (net.Socket.)
        ;; timing issues requires some of these before .connect
        _ (doto client
            (.on "connect"
                 #(.write client (str repl-init "\n")))
            (.on "error"
                 #(println "Server error: " (js->clj %)))
            (.on "data"
                 (partial handle-response output)))
        result (.connect client port host)]
    client))

(defn when-no-repl
  [f]
  (if @repl
    (u/resolved-promise true)
    (u/new-promise
     (fn [resolve]
       (resolve (f))))))

(defn start-repl* 
  []
  (println "Starting REPL...")
  (let [host (:host repl-options)
        _ (println (str "host: " host))
        port (:port repl-options)
        _ (println (str "port: " port))
        out (.createOutputChannel (.. vscode -window) "Arcadia REPL")
        conn (connect-repl out host port)]
    (.show out true)
    (println "REPL started!")
    (reset! repl
            {:client conn
             :output out
             :host host
             :port port})))
            
(defn start-repl
  []
  (when-no-repl
      ;; returning CLJ data structures to command handlers makes vscode unhappy
      #(do (start-repl*) true)))

(defn send 
  [msg]
  (-> (when-no-repl start-repl*)
      (u/then #(handle-input msg))))

(defn send-line
  [editor]
  (send 
   (-> (.-document editor)
       (.lineAt (.. editor -selection -start))
       (.-text))))

(defn send-selection
  [editor]
  (send 
   (-> (.-document editor)
       (.getText (.-selection editor)))))
  
(defn send-file
  [editor]
  (send (.. editor -document getText)))

(defn send-load-file
  [editor]
  (let [fpath (.-fileName (.. editor -document))]
    (send (str "(load-file \"" fpath "\")"))))

(defn is-comment
  [line]
  (let [firstChar (subs (s/trim (.-text line)) 0 1)]
    (= firstChar #";")))

(defn check-line
  [line]
  (not (or (.-isEmptyOrWhitespace line)
           (is-comment line))))

(defn send-region
  [editor]
  (let [activeLine (.. editor -selection -active -line)
        startLine
        (last
         (take-while
          (fn [n]
            (not (.. editor -document
                     (lineAt n) -isEmptyOrWhitespace)))
          (take-while (partial < -1)
                      (iterate dec activeLine))))
        endLines
        (filter
         (comp not is-comment)
         (take-while
          (fn [l] (not (.-isEmptyOrWhitespace l)))
          (map (fn [n]
                 (.. editor -document (lineAt n)))
               (iterate inc
                        (if (.. editor -document
                                (lineAt startLine) -isEmptyOrWhitespace)
                          (+ startLine 1)
                          startLine)))))]
    (send
     (s/join "\n"
             (map aget
                  endLines (repeat "text"))))))

(defn activate-repl
  []
  (println "Registering REPL commands")
  [(u/register-command! "arcadia.replStart" start-repl)
   (u/register-text-editor-command! "arcadia.replSendLine" send-line)
   (u/register-text-editor-command! "arcadia.replSendSelection" send-selection)
   (u/register-text-editor-command! "arcadia.replSendRegion" send-region)
   (u/register-text-editor-command! "arcadia.replSendFile" send-file)
   (u/register-text-editor-command! "arcadia.replLoadFile" send-load-file)])
