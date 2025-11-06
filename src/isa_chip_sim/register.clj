(ns isa-chip-sim.register)

;; --- Architecture Definitions ---

(def arm-arch
  {:registers [:r0 :r1 :r2 :r3 :r4 :r5 :r6 :r7 :r8 :r9 :r10 :r11 :r12 :sp :lr :pc
               :f0 :f1 :f2 :f3 :f4 :f5 :f6 :f7
               :v0 :v1 :v2 :v3 :v4 :v5 :v6 :v7 :v8 :v9 :v10 :v11 :v12 :v13 :v14 :v15
               :v16 :v17 :v18 :v19 :v20 :v21 :v22 :v23 :v24 :v25 :v26 :v27 :v28 :v29 :v30 :v31]
   :pointer-size 32
   :vector-reg-size 128 ; V registers are 128 bits
   :gpr-init-val 0})

(def x86-32-arch
  {:registers [:eax :ebx :ecx :edx :esi :edi :esp :ebp :eip :eflags]
   :pointer-size 32
   :gpr-init-val 0})

(def x86-64-arch
  {:registers [:rax :rbx :rcx :rdx :rsi :rdi :rsp :rbp
               :r8 :r9 :r10 :r11 :r12 :r13 :r14 :r15
               :rip :rflags
               :xmm0 :xmm1 :xmm2 :xmm3 :xmm4 :xmm5 :xmm6 :xmm7
               :xmm8 :xmm9 :xmm10 :xmm11 :xmm12 :xmm13 :xmm14 :xmm15
               :ymm0 :ymm1 :ymm2 :ymm3 :ymm4 :ymm5 :ymm6 :ymm7
               :ymm8 :ymm9 :ymm10 :ymm11 :ymm12 :ymm13 :ymm14 :ymm15]
   :pointer-size 64
   :vector-reg-size 256 ; YMM registers are 256 bits
   :gpr-init-val 0})

(def architectures
  {:arm arm-arch
   :x86-32 x86-32-arch
   :x86-64 x86-64-arch})

;; --- Register File Record and Initialization ---

(defrecord RegisterFile [arch registers])

(defn new-register-file [arch-keyword]
  (let [arch-map (get architectures arch-keyword)
        registers-list (:registers arch-map)
        gpr-val (:gpr-init-val arch-map)]
    (->RegisterFile
      arch-map
      (zipmap
        registers-list
        (map (fn [reg]
               (cond
                 ;; Initialize YMM registers to an 8-element zero vector (32-bit words)
                 (.startsWith (name reg) "ymm") (vec (repeat 8 0))
                 ;; Initialize V (ARM vector) registers to a 4-element zero vector (32-bit words)
                 (.startsWith (name reg) "v") (vec (repeat 4 0))
                 ;; All other GPRs get the single GPR init value (usually 0)
                 :else gpr-val))
             registers-list)))))

;; --- Register Accessors ---

(defn- get-vector-slice [register-file parent-reg start-idx end-idx]
  (let [parent-val (get-in register-file [:registers parent-reg])]
    (subvec parent-val start-idx end-idx)))

(defn- set-vector-slice [register-file parent-reg value start-idx end-idx]
  (let [parent-val (get-in register-file [:registers parent-reg])
        slice-size (- end-idx start-idx)
        value-vec (take slice-size value) ; Ensure value has correct length
        new-parent-val (into (subvec parent-val 0 start-idx)
                             (into value-vec (subvec parent-val end-idx)))]
    (assoc-in register-file [:registers parent-reg] new-parent-val)))

(defn read-reg [^RegisterFile register-file reg-name]
  (let [reg-str (name reg-name)]
    (cond
      ;; XMM (x86-64, lower 128 of YMM)
      (.startsWith reg-str "xmm")
      (get-vector-slice register-file (keyword (str "ymm" (subs reg-str 3))) 0 4)

      ;; V (ARM, full 128-bit V register) - Currently acts as the parent
      (.startsWith reg-str "v")
      (get-in register-file [:registers reg-name])
      
      ;; Add cases here for S, D, Q registers if needed later
      ;; For now, we only defined V0-V31, so they read directly.

      ;; Default: Read general purpose registers directly
      :else
      (get-in register-file [:registers reg-name]))))

(defn write-reg [^RegisterFile register-file reg-name value]
  (let [reg-str (name reg-name)]
    (cond
      ;; XMM (x86-64, modifies lower 128 of YMM)
      (.startsWith reg-str "xmm")
      (set-vector-slice register-file (keyword (str "ymm" (subs reg-str 3))) value 0 4)

      ;; V (ARM, full 128-bit V register)
      (.startsWith reg-str "v")
      (assoc-in register-file [:registers reg-name] value)
      
      ;; Default: Write general purpose registers directly
      :else
      (assoc-in register-file [:registers reg-name] value))))
