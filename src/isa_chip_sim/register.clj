(ns isa-chip-sim.register)

(def arm-arch
  {:registers [:r0 :r1 :r2 :r3 :r4 :r5 :r6 :r7 :r8 :r9 :r10 :r11 :r12 :sp :lr :pc
               :f0 :f1 :f2 :f3 :f4 :f5 :f6 :f7
               :v0 :v1 :v2 :v3 :v4 :v5 :v6 :v7 :v8 :v9 :v10 :v11 :v12 :v13 :v14 :v15
               :v16 :v17 :v18 :v19 :v20 :v21 :v22 :v23 :v24 :v25 :v26 :v27 :v28 :v29 :v30 :v31]
   :pointer-size 32})

(def x86-32-arch
  {:registers [:eax :ebx :ecx :edx :esi :edi :esp :ebp :eip :eflags]
   :pointer-size 32})

(def x86-64-arch
  {:registers [:rax :rbx :rcx :rdx :rsi :rdi :rsp :rbp
               :r8 :r9 :r10 :r11 :r12 :r13 :r14 :r15
               :rip :rflags
               :xmm0 :xmm1 :xmm2 :xmm3 :xmm4 :xmm5 :xmm6 :xmm7
               :xmm8 :xmm9 :xmm10 :xmm11 :xmm12 :xmm13 :xmm14 :xmm15
               :ymm0 :ymm1 :ymm2 :ymm3 :ymm4 :ymm5 :ymm6 :ymm7
               :ymm8 :ymm9 :ymm10 :ymm11 :ymm12 :ymm13 :ymm14 :ymm15]
   :pointer-size 64})

(def architectures
  {:arm arm-arch
   :x86-32 x86-32-arch
   :x86-64 x86-64-arch})

(defrecord RegisterFile [arch registers])

(defn new-register-file [arch-keyword]
  (let [arch (get architectures arch-keyword)]
    (->RegisterFile arch (zipmap (:registers arch) (repeat 0)))))

(defn read-reg [^RegisterFile register-file reg-name]
  (let [reg-str (name reg-name)]
    (if (.startsWith reg-str "xmm")
      (let [ymm-name (keyword (str "ymm" (subs reg-str 3)))
            ymm-val (get-in register-file [:registers ymm-name])]
        (if (vector? ymm-val)
          (subvec ymm-val 0 4)
          (subvec (vec (repeat 8 0)) 0 4)))
      (get-in register-file [:registers reg-name]))))

(defn write-reg [^RegisterFile register-file reg-name value]
  (let [reg-str (name reg-name)]
    (if (.startsWith reg-str "xmm")
      (let [ymm-name (keyword (str "ymm" (subs reg-str 3)))
            ymm-val (get-in register-file [:registers ymm-name])
            ymm-vec (if (vector? ymm-val) ymm-val (vec (repeat 8 0)))
            new-ymm-val (into value (subvec ymm-vec 4 8))]
        (assoc-in register-file [:registers ymm-name] new-ymm-val))
      (assoc-in register-file [:registers reg-name] value))))
