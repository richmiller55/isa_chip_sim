(ns isa-chip-sim.ui-ascii
  (:require [isa-chip-sim.register :as reg]
            [clojure.string :as str]))

;; --- Content Generation ---

(defn- render-registers
  "Renders the register file to a raw string, one register per line."
  [state]
  (let [register-file (:registers state)
        registers (:registers register-file)
        reg-names (sort (keys registers))]
    (str/join "\n"
              (for [reg-name reg-names]
                (format "%s: %s" reg-name (reg/read-reg register-file reg-name))))))

(defn- render-pipeline
  "Renders the pipeline latches to a raw string, one latch per line."
  [state]
  (let [format-instruction (fn [instruction]
                             (if instruction
                               (str (:opcode instruction) " " (str/join ", " (:operands instruction)))
                               "empty"))]
    (str/join "\n"
              [(format "IF/ID: %s" (format-instruction (:if-id-latch state)))
               (format "ID/EX: %s" (format-instruction (:id-ex-latch state)))
               (format "EX/MEM: %s" (format-instruction (:ex-mem-latch state)))
               (format "MEM/WB: %s" (format-instruction (:mem-wb-latch state)))])))

;; --- ASCII Windowing System ---

(defn- create-screen
  "Creates a blank screen (a vector of strings) of a given size."
  [width height]
  (vec (repeat height (apply str (repeat width \space)))))

(defn- place-string
  "Places a multi-line string onto a screen buffer at the given x, y coordinates."
  [screen x y text]
  (let [lines (str/split-lines text)
        screen-height (count screen)
        screen-width (if (pos? screen-height) (count (first screen)) 0)]
    (loop [i 0
           s screen]
      (if (and (< i (count lines)) (< (+ y i) screen-height))
        (let [line-to-place (get lines i)
              target-y (+ y i)
              current-line (get s target-y)
              new-line (str (subs current-line 0 (min (count current-line) x))
                            line-to-place
                            (subs current-line (min (count current-line) (+ x (count line-to-place)))))]
          (recur (inc i) (assoc s target-y (subs new-line 0 screen-width))))
        s))))

(defn- create-window
  "Creates a bordered window as a multi-line string."
  [title content width height]
  (let [content-lines (str/split-lines content)
        inner-width (- width 2)
        top-border (str "+" (apply str (repeat inner-width "-")) "+")
        bottom-border top-border
        title-bar (str "| " title (apply str (repeat (- inner-width 1 (count title)) " ")) " |")

        body-lines (for [i (range (- height 3))]
                     (let [content-line (get content-lines i "")]
                       (str "| " (format (str "%-" (- inner-width 1) "s") (subs content-line 0 (min (count content-line) (- inner-width 1)))) " |")))

        all-lines (concat [top-border title-bar] body-lines [bottom-border])]
    (str/join "\n" all-lines)))

;; --- UI Composition ---

(defn render-ui
  "Renders the entire UI to an ASCII string using a windowed layout."
  ([state]
   (render-ui state 80 24)) ; Default screen size
  ([state width height]
   (let [screen (create-screen width height)

         ;; Register Window
         registers-content (render-registers state)
         reg-win-width 25
         reg-win-height (+ 4 (count (str/split-lines registers-content)))
         registers-window (create-window "Registers" registers-content reg-win-width reg-win-height)

         ;; Pipeline Window
         pipeline-content (render-pipeline state)
         pipe-win-width 40
         pipe-win-height (+ 4 (count (str/split-lines pipeline-content)))
         pipeline-window (create-window "Pipeline" pipeline-content pipe-win-width pipe-win-height)

         ;; Layout
         screen-with-registers (place-string screen 1 1 registers-window)
         screen-with-pipeline (place-string screen-with-registers (+ reg-win-width 2) 1 pipeline-window)]

     (str/join "\n" screen-with-pipeline))))
