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
                                               {:width 128})]
    (atom {:title "App title"
           :image {:min-x 0
                   :min-y 0
                   :width (.getWidth image)
                   :height (.getHeight image)
                   :viewport-width (.getWidth image)
                   :viewport-height (.getHeight image)
                   :src image}})))

(defn img [{:keys [src
                   min-x min-y
                   width height
                   viewport-width viewport-height]}]
  {:fx/type :image-view
   ;; how to control the image's zoom?
   ;; https://github.com/cljfx/cljfx/blob/master/src/cljfx/fx/image_view.clj
   :image {;; TODO: apparently you can only read from an input-stream once.
           ;; how can we re-use the same input stream over and over? rewinding
           ;; the object is an option but it seems icky.
           :is (dream-shrine.png/buffered-image->input-stream src)
           ;; this example indicates that viewport can be used to control zoom.
           ;; https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a

           ;; or is this example better? https://community.oracle.com/thread/2541811?tstart=0
           :x min-x
           :y min-y
           :viewport {:min-x min-x :min-y min-y
                      :width viewport-width :height viewport-height}}})

(defn root [{{:keys [title image] :as state} :state}]
  (prn state)
  {:fx/type :stage
   :showing true
   :title "Cljfx example"
   :width 300
   :height 100
   :scene {:fx/type :scene
           :root {:fx/type :scroll-pane
                  :content {:fx/type :group
                            :children [(img image)]}}
                                       ;(slider-view {:min 0 :max 128 :value 128})]}}
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
   {:keys [min-x min-y image-width viewport-width image-height viewport-height]}]
  (let [;; can i do anything useful with the scene and/or window?
        scene (.getScene ^Node (.getTarget event))
        window (.getWindow scene)
        x (.getX event)
        y (.getY event)
        x-proportion (/ x image-width)
        y-proportion (/ y image-width)]
    {:x (+ min-x
           (* x-proportion viewport-width))
     :y (+ min-y
           (* y-proportion viewport-height))}))

(defmulti event-handler :event/type)

;; TODO: follow map events example in order to implement zoom
;; https://cljdoc.org/d/cljfx/cljfx/1.6.7/doc/readme#map-events
(defmethod event-handler ::zoom [{:keys [fx/event]}]
  (let [{:keys [min-x min-y image-width viewport-width image-height viewport-height]} (deref *state)
        ;; if using zoom-factor doesn't feel right, try out the
        ;; exponentiation style in james-d's  gist example
        zoom-factor (.getZoomFactor event)
        mouse-coords (image-view-mouse-coords event
                                              ;; this is basically state
                                              {:min-x min-x
                                               :min-y min-y
                                               :image-width image-width
                                               :image-height image-height
                                               :viewport-width viewport-width
                                               :viewport-height viewport-height})

        width' (* viewport-width zoom-factor)
        height' (* viewport-height zoom-factor)
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
                     :viewport-width (-> width' Math/floor int)
                     :viewport-height (-> height' Math/floor int)}]
                     
    (swap! *state update-in [:image] merge image-attrs)))

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
