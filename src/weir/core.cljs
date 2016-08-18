(ns weir.core
  (:require-macros [reagent.ratom :as rm])
  (:require [clojure.set :as set]
            [datascript.core :as d]
            [franz.core :as franz]
            [reagent.core :as r]
            [taoensso.sente :as sente]
            [weasel.repl :as repl]))

(defmulti event-key
  (fn [event] event))

(defmethod event-key :default
  [_]
  nil)

(defmulti event-handler
  (fn [event data context]
    event))

(defmethod event-handler :default
  [event _ _]
  (.log js/console (str "Unhandled event " event)))

(defn- watch-db
  [app-db]
  (fn [_ _ _ new-db] (reset! app-db new-db)))

(defn- watch-franz
  [context]
  (franz/sparse-handler
   (fn [offset [key [event data]]]
     (.log js/console (str "Got event message: " event))
     (event-handler event data context))))

(defn- sente-router
  [topic]
  (fn [msg]
    (let [{:keys [id ?data]} msg]
      (if (= id :chsk/recv)
        (let [[event data] ?data]
          (franz/send! topic (event-key event) [event data]))
        (.log js/console (str "Got non-event sente message: " id))))))

(defn ^:export initialize!
  [{:keys [schema init-tx topic-opts sente-path sente-opts]
    :or   {sente-path "/chsk"
           sente-opts {:type :auto}}}]
  (let [conn    (d/create-conn schema)
        app-db  (r/atom (d/db conn))
        topic   (franz/topic topic-opts)
        emit-fn (fn emit-event
                  ([event]
                   (emit-event event nil))
                  ([event data]
                   (franz/send! topic (event-key event) [event data])))
        sente   (sente/make-channel-socket! sente-path sente-opts)
        context {:conn conn :emit-fn emit-fn :sente! sente}]
    (add-watch conn :watch-db (watch-db app-db))
    (when init-tx
      (d/transact! conn init-tx))
    (franz/subscribe! topic :watch-franz (watch-franz context))
    (let [stop-fn (sente/start-client-chsk-router! (:ch-recv sente) (sente-router topic))]
      {:app-db  app-db
       :emit-fn emit-fn
       :sente!  sente
       :topic!  topic
       :conn!   conn
       :stop-fn stop-fn})))
