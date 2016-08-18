(ns weir.macros)

(defmacro defevent
  [name strategy bindings & body]
  (let [msg-key (case strategy
                  :all `(gensym '~name)
                  :latest `(quote ~name)
                  :some nil)]
    `(do
       (defmethod weir.core/event-key ~(keyword name)
         [event#]
         ~msg-key)
       (defmethod weir.core/event-handler ~(keyword name)
         ~bindings
         ~@body))))
