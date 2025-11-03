(ns isa-chip-sim.register)

(def arm-arch
  {:registers [:r0 :r1 :r2 :r3 :r4 :r5 :r6 :r7 :r8 :r9 :r10 :r11 :r12 :sp :lr :pc
               :f0 :f1 :f2 :f3 :f4 :f5 :f6 :f7]
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
  (get-in register-file [:registers reg-name]))

(defn write-reg [^RegisterFile register-file reg-name value]
  (assoc-in register-file [:registers reg-name] value))
