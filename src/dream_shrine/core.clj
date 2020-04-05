(ns dream-shrine.core
  (:gen-class)
  (:require [cljfx.api :as fx]
            [dream-shrine.maps]
            [dream-shrine.png])
  (:import [javafx.event ActionEvent]
           [javafx.scene Node]))

(def *state
  (atom {:title "App title"
         :min-x 0
         :min-y 0
         :width 0
         :height 0
         :image (-> (dream-shrine.png/generate-image dream-shrine.png/rom
                                                     {:offset dream-shrine.maps/minimap-overworld-tiles-offset
                                                      :size dream-shrine.maps/minimap-overworld-tiles-size}
                                                     {})
                    dream-shrine.png/buffered-image->input-stream)}))

(defn root [{:keys [title min-x min-y width height image]}]
  (println "in root")
  {:fx/type :stage
   :showing true
   :title "Cljfx example"
   :width 300
   :height 100
   :scene {:fx/type :scene
           :root {:fx/type :scroll-pane
                  :content {:fx/type :group
                            :children (if (nil? image)
                                        []
                                        [{:fx/type :image-view
                                          ;; how to control the image's zoom?
                                          ;; https://github.com/cljfx/cljfx/blob/master/src/cljfx/fx/image_view.clj
                                          :image {:is image
                                                  ;; this example indicates that viewport can be used to control zoom.
                                                  ;; https://gist.github.com/james-d/ce5ec1fd44ce6c64e81a
                                                  :viewport {:min-x min-x :min-y min-y
                                                             :width width :height height}}}])}}
           ;; don't forget that you can pash additionally kwags in the
           ;; event/type map which will then be merged into map passed
           ;; to the event-handler multimethod. could be useful for
           ;; something.
           :on-zoom {:event/type ::zoom}}})

(defn clamp [n min max]
  (cond (< n min) min
        (> n max) max
        :else n))

(defn image-view-mouse-coords
  "convert mouse coordinates in the image-view to coordinates in the actual image"
  [^ActionEvent event
   {:keys [min-x min-y viewport-width viewport-height]}]
  (let [;; can i do anything useful with the scene and/or window?
        scene (.getScene ^Node (.getTarget event))
        window (.getWindow scene)
        {:keys [x y]} event
        image-width 128 ;; FIXME: this value is fake
        image-height 128 ;; FIXME: this value is fake
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
  (let [{:keys [min-x min-y width height]} *state
        ;; if using zoom-factor doesn't feel right, try out the
        ;; exponentiation style in james-d's  gist example
        zoom-factor (.getZoomFactor event)
        mouse-coords (image-view-mouse-coords event
                                              ;; this is basically state
                                              {:min-x min-x
                                               :min-y min-y
                                               :viewport-width width
                                               :viewport-height height})

        width' (* width zoom-factor)
        height' (* height zoom-factor)
        min-x' (- (mouse-coords :x)
                  (* zoom-factor (- (mouse-coords :x)
                                    min-x)))
        min-y' (- (mouse-coords :y)
                  (* zoom-factor (- (mouse-coords :y)
                                    min-y)))]
    (swap! *state merge {:min-x (clamp min-x' 0 (- width width'))
                         :min-y (clamp min-y' 0 (- height height'))
                         :width width'
                         :height height'})))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)))

(defn -main
  [& args]
  (fx/mount-renderer
   *state
   (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler event-handler})))
