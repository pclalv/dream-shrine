(ns dream-shrine.core
  (:gen-class)
  (:require [cljfx.api :as fx]))

(def *state
  (atom {:title "App title"}))

(defn root [_state]
  (println "in root")
  {:fx/type :stage
   :showing true
   :title "Cljfx example"
   :width 300
   :height 100
   :scene {:fx/type :scene
           :root {:fx/type :scroll-pane
                  :content {:fx/type :group
                            :children [{:fx/type :image-view
                                        :image {:url (str "file:"
                                                          "/Users/paulalvarez/code/dream-shrine/images/test.png")}}]}}
           :on-zoom (fn [zoom-event]
                      (let [zoom-factor (.getZoomFactor zoom-event)]
                        (cond (< 1 zoom-factor)
                              #_=> (prn "zoom in")
                              (> 1 zoom-factor)
                              #_=> (prn "zoom out")
                              (= 1 zoom-factor)
                              #_=> (prn "zoom none"))))}})


(defn map-event-handler [event]
  (case (:event/type event)
    ::zoom-in (swap! *state assoc-in [:by-id (:id event) :done] (:fx/event event))))

;; TODO: follow map events example in order to implement zoom
;; https://cljdoc.org/d/cljfx/cljfx/1.6.7/doc/readme#map-events
(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)))

(defn -main
  [& args]
  (fx/mount-renderer
   *state
   (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler})))
