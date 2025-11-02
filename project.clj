(defproject isa_chip_sim "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "https://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.3"]
                 [cljfx "1.9.6"]
                 [org.openjfx/javafx-controls "17.0.6"]
                 [org.openjfx/javafx-fxml "17.0.6"]
                 [org.openjfx/javafx-graphics "17.0.6"]
                 [org.openjfx/javafx-base "17.0.6"]]
  :java-source-paths ["src/java"]
  :main ^:skip-aot isa-chip-sim.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
