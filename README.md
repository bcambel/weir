# weir

![weir](doc/weir.jpg)

A Clojure library for a Function Reactive Frontend, based on the [sente](https://github.com/ptaoussanis/sente), [franz](https://gitlab.com/j-pb/franz), [datascript](https://github.com/tonsky/datascript) and [reagent](https://github.com/reagent-project/reagent) stack.

## Usage

### Getting started

Add `[functionalbytes/weir "0.1.0-SNAPSHOT"]` to your dependencies.

Require at least the `weir.core` namespace and the `weir.macros` macros in your ClojureScript file, for example:

```clj
(ns my.app
  (:require-macros [reagent.ratom :as rm]
                   [weir.macros :refer (defevent)])
  (:require [datascript.core :as d]
            [reagent.core :as r]
            [weir.core :as weir]))
```

### Defining event handlers

Events are read from a _franz_ topic, coming from the server using _sente_, from other events or from event functions in the _reagent_ view.

To handle these events, you define  event handlers, using the `defevent` macro.
The macro takes two arguments, a bindings vector and a body.

```clj
(defevent my.app/my-event :all
  [event data {:keys [conn emit-fn] :as context}]
  ...)
```

The first argument is a namespace qualified symbol, defining what event triggers the execution of the body.

The second argument is a keyword, which gives a hint towards _weir_ what kind of strategy should be followed, when the number of events in the log is getting too large and needs a clean-up/compaction.

Currently it has three options:

* `:all` - do not clean up or compact events of this kind if they are not read yet.
* `:latest` - it is allowed to compact this kind of event, such that at least the latest unread event is kept.
* `:some` - it is allowed to compact or completely clean/drop these kind of events.

Next is a binding vector, just as with a normal function. It acts as a 3-arity function.
It receives the event (a namespaced keyword), the data for that event, and a context map.
This context map contains the following:

* `:conn` - the DataScript connection (`:conn`) for quering and transacting.
* `:emit-fn` - a 1-to-2-arity function to emit a new event onto the _franz_ topic, where the first argument is a namespaced keyword identifying the event kind, and the second optional argument is the event data.

### Defining your view

The view is created by standard _reagent_ components.
The components base their information use a `app-db`, which is reactive atom containing the latest DataScript database (and thus trigger a re-render when the database has changed by an event handler).
To influence "the world" from a component, the same `emit-fn` function as explained above can used.
The component should live by these rules, to fully benefit from the reactive design.
For example:

```clj
(defn my-component [app-db emit-fn]
  [:input {:type "submit"
           :on-click #(emit-event ::my-event)}]
```

But how do these components get these `app-db` and `emit-fn`? Read the next section.

### Initializing weir

To set everything in motion, you need to call the `weir/initialize!` function.
It will set everything up and connect _sente_ to the server.
It takes no required arguments, though options can be specified using keyword arguments.

It returns a map, with several entries, but the important two are `:app-db` and `:emit-fn`.
Now that you have these, you can pass the to the view components, as soon as you mount them.
For example:

```clj
(defn ^:export mount []
  (let [weir (weir/initialize!)]
    (r/render-component [my-component (:app-db weir) (:emit-fn weir)]
                        (.getElementById js/document "my-id"))))
```

The `initialize!` function can take the following keyword arguments:

* `:sente-path` -
* `:sente-opts` -
* `:schema` -
* `:init-tx` -
* `:topic-opts` -

_As always, have fun!_

## License

Copyright © 2016 Functional Bytes

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
