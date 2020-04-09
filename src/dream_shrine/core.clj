(ns dream-shrine.core
  (:gen-class)
  (:require [cljfx.api :as fx]
            [dream-shrine.maps]
            [dream-shrine.png])
  (:import [javafx.event ActionEvent]
           [javafx.scene Node]))

(def *state
  (let [image (dream-shrine.png/generate-image dream-shrine.png/rom
                                               {:offset dream-shrine.maps/minimap-overworld-tiles-offset
                                                :size dream-shrine.maps/minimap-overworld-tiles-size}
                                               {:width 64})]
    (atom {:title "App title"
           :image {:min-x 0
                   :min-y 0
                   :width (.getWidth image)
                   :height (.getHeight image)
                   :viewport-width (.getWidth image)
                   :viewport-height (.getHeight image)
                   :src image}})))

(defn spinner-view [{:keys [label values event]}]
  {:fx/type :h-box
   :children [{:fx/type :label
               :text label}
              {:fx/type :spinner
               :value-factory {:fx/type :list-spinner-value-factory
                               :items values}
               :on-value-changed {:event/type event}}]})

(defn img [{{:keys [src
                    min-x min-y
                    width height
                    viewport-width viewport-height]} :image}]
  {:fx/type :image-view
   :preserve-ratio true
   :image {;; TODO: apparently you can only read from an input-stream
           ;; once.  how can we re-use the same input stream over and
           ;; over? rewinding the object is an option but it seems
           ;; icky, and re-generating this everytime seems
           ;; inefficient.
           :is (dream-shrine.png/buffered-image->input-stream src)}
   ;; this example indicates that viewport can be used to control zoom.
   ;; https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
   :viewport {:min-x min-x :min-y min-y
              :width viewport-width :height viewport-height}})

(defn root [{{:keys [title image] :as state} :state}]
  {:fx/type :stage
   :showing true
   :title "Cljfx example"
   :width 512
   :height 512
   :scene {:fx/type :scene
           :root {:fx/type :scroll-pane
                  :content {:fx/type :v-box
                            :children [{:fx/type img
                                        :image image}
                                       {:fx/type spinner-view
                                        :label "width"
                                        :values [8 16 32 64 128]
                                        :event ::set-width}]}}
           ;; don't forget that you can pass additionally kwargs in
           ;; the event/type map which will then be merged into map
           ;; passed to the event-handler multimethod. could be useful
           ;; for something.
           :on-zoom {:event/type ::zoom}}})

(defn clamp [n min max]
  (cond (< n min) min
        (> n max) max
        :else n))

(defn image-view-mouse-coords
  "convert mouse coordinates in the image-view to coordinates in the actual image"
  [^ActionEvent event
   {:keys [min-x min-y image-width viewport-width image-height viewport-height] :as whatever}]
  (println "whatever " whatever)
  (let [;; can i do anything useful with the scene and/or window?
        scene (.getScene ^Node (.getTarget event))
        window (.getWindow scene)
        x (.getX event)
        y (.getY event)
        _ (prn "x" x)
        _ (prn "image-width" image-width)
        x-proportion (/ x image-width)
        y-proportion (/ y image-width)]
    {:x (+ min-x
           (* x-proportion viewport-width))
     :y (+ min-y
           (* y-proportion viewport-height))}))

(defmulti event-handler :event/type)

;; TODO: follow map events example in order to implement zoom
;; https://cljdoc.org/d/cljfx/cljfx/1.6.7/doc/readme#map-events
(defmethod event-handler ::zoom [{event :fx/event
                                  event-type :event/type}]
  (let [_ (prn "(deref *state)" (deref *state))
        {{:keys [min-x min-y width height viewport-width viewport-height]} :image} (deref *state)
        ;; if using zoom-factor doesn't feel right, try out the
        ;; exponentiation style in james-d's  gist example
        zoom-factor (.getZoomFactor event)
        mouse-coords (image-view-mouse-coords event
                                              ;; this is basically state
                                              {:min-x min-x
                                               :min-y min-y
                                               :image-width width
                                               :image-height height
                                               :viewport-width viewport-width
                                               :viewport-height viewport-height})

        width' (-> (* viewport-width zoom-factor)
                   Math/floor
                   int)
        height' (-> (* viewport-height zoom-factor)
                    Math/floor
                    int)
        min-x' (- (mouse-coords :x)
                  (* zoom-factor (- (mouse-coords :x)
                                    min-x)))
        min-y' (- (mouse-coords :y)
                  (* zoom-factor (- (mouse-coords :y)
                                    min-y)))
        image-attrs {:min-x (-> min-x' Math/floor int)
                     :min-y (-> min-y' Math/floor int)
                     ;; :min-x (clamp min-x' 0 (- width width'))
                     ;; :min-y (clamp min-y' 0 (- height height'))
                     :viewport-width width'
                     :viewport-height height'}]
    
    (swap! *state update-in [:image] merge image-attrs)))

(defmethod event-handler ::set-width [{:keys [fx/event]}]
  (let [width event
        image (dream-shrine.png/generate-image dream-shrine.png/rom
                                               {:offset dream-shrine.maps/minimap-overworld-tiles-offset
                                                :size dream-shrine.maps/minimap-overworld-tiles-size}
                                               {:width width})]
    (swap! *state update :image merge {:width (.getWidth image)
                                       :height (.getHeight image)
                                       :src image})))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc (fn [state]
                                   {:fx/type root
                                    :state state}))
   :opts {:fx.opt/map-event-handler event-handler}))

(fx/mount-renderer *state
                   renderer)

(defn -main
  [& args]
  nil)
