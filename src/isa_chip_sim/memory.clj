(ns isa-chip-sim.memory)

(defrecord Memory [text bss heap stack])

(def text-base 0x0000)
(def text-size 0x1000) ; 4KB
(def bss-base 0x1000)
(def bss-size 0x1000) ; 4KB
(def heap-base 0x2000)
(def heap-size 0x2000) ; 8KB
(def stack-base 0x4000)
(def stack-size 0x1000) ; 4KB

(defn new-memory []
  (->Memory (vec (repeat text-size 0))
            (vec (repeat bss-size 0))
            (vec (repeat heap-size 0))
            (vec (repeat stack-size 0))))

(defn- resolve-address [address]
  (cond
    (and (>= address text-base) (< address (+ text-base text-size)))
    [:text (- address text-base)]
    (and (>= address bss-base) (< address (+ bss-base bss-size)))
    [:bss (- address bss-base)]
    (and (>= address heap-base) (< address (+ heap-base heap-size)))
    [:heap (- address heap-base)]
    (and (>= address (- stack-base stack-size)) (< address stack-base))
    [:stack (- stack-base address)]
    :else
    (throw (ex-info "Invalid memory address" {:address address}))))

(defn read-mem [^Memory mem address size]
  (let [[segment offset] (resolve-address address)
        data (get mem segment)
        segment-size (count data)]
    (when (> (+ offset size) segment-size)
      (throw (ex-info "Memory read out of bounds" {:address address :size size :segment segment :offset offset :segment-size segment-size})))
    (subvec data offset (+ offset size))))

(defn write-mem [^Memory mem address value size]
  (let [[segment offset] (resolve-address address)
        current-segment (get mem segment)
        segment-size (count current-segment)]
    (when (> (+ offset size) segment-size)
      (throw (ex-info "Memory write out of bounds" {:address address :size size :segment segment :offset offset :segment-size segment-size})))
    (let [before (subvec current-segment 0 offset)
          after (subvec current-segment (+ offset size))]
      (assoc mem segment (into (into before value) after)))))
