(ns dream-shrine.core
  (:gen-class)
  (:require [cljfx.api :as fx]))

(fx/on-fx-thread
 (fx/create-component
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
                                                          "/Users/paulalvarez/code/dream-shrine/images/test.png")}}]}}}}))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
