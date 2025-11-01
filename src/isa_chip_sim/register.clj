(ns isa-chip-sim.register)

(def arm-arch
  {:registers [:r0 :r1 :r2 :r3 :r4 :r5 :r6 :r7 :r8 :r9 :r10 :r11 :r12 :sp :lr :pc
               :f0 :f1 :f2 :f3 :f4 :f5 :f6 :f7]
   :pointer-size 32})

(def x86-arch
  {:registers [:eax :ebx :ecx :edx :esi :edi :esp :ebp :eip]
   :pointer-size 32})

(def architectures
  {:arm arm-arch
   :x86 x86-arch})

(defrecord RegisterFile [arch registers])

(defn new-register-file [arch-keyword]
  (let [arch (get architectures arch-keyword)]
    (->RegisterFile arch (zipmap (:registers arch) (repeat 0)))))

(defn read-reg [^RegisterFile register-file reg-name]
  (get-in register-file [:registers reg-name]))

(defn write-reg [^RegisterFile register-file reg-name value]
  (assoc-in register-file [:registers reg-name] value))
