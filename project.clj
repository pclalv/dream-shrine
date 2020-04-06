(defproject dream-shrine "0.1.0-SNAPSHOT"
  :description "An editor for Link's Awakening DX."
  :url "https://github.com/pclalv/dream-shrine"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [cljfx "1.6.7"]]

  :main ^:skip-aot dream-shrine.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
