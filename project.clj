(defproject twitterapp "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.8.0"],
    [twitter-api "0.8.0"],
    [com.novemberain/monger "3.1.0"],
    [org.slf4j/slf4j-nop "1.7.12"]
  ]
  :main ^:skip-aot twitterapp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
