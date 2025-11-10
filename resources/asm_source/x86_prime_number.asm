section .data
    newline db 10               ; Newline character
    space db ' '                ; Space character

section .bss
    ; A buffer to store the ASCII representation of a number for printing.
    buffer resb 12

section .text
    global _start

_start:
    mov ecx, 2                  ; Outer loop counter, starts at 2. Numbers 0 and 1 are not prime.

prime_loop:
    cmp ecx, 100                ; Check if we have tested all numbers up to 100. (Original code used 20, changed to 100)
    jg end_program              ; If ecx > 100, we are done.

    mov ebx, 2                  ; Inner loop counter (divisor), starts at 2.

test_divisor_loop:
    cmp ebx, ecx                ; Check if ebx (divisor) is less than ecx (number).
    jge is_prime                ; If ebx >= ecx, number is prime. Jump to print.

    mov eax, ecx                ; Load number to be tested into eax.
    xor edx, edx                ; Clear edx before division (divides edx:eax by a register).
    div ebx                     ; Divide eax by ebx. Remainder is in edx.

    cmp edx, 0                  ; Check if the remainder is 0.
    je not_prime                ; If remainder is 0, it's not a prime number.

    inc ebx                     ; Increment the divisor.
    jmp test_divisor_loop       ; Loop again with the next divisor.

is_prime:
    mov eax, ecx                ; The current number is prime. Move it to eax for conversion.
    call print_number           ; Call a subroutine to print the number.

not_prime:
    inc ecx                     ; Increment to the next number to test.
    jmp prime_loop              ; Continue the outer loop.

end_program:
    push newline                ; Push newline character for final print
    push 1
    call write_string
    add esp, 8                  ; Clean up stack

    mov eax, 1                  ; sys_exit syscall number
    xor ebx, ebx                ; exit code 0
    int 0x80                    ; call kernel

; --------------------------------------------------------
; Subroutine to print an integer in EAX.
; It converts the number to ASCII characters and writes to stdout.
; --------------------------------------------------------
print_number:
    push eax                    ; Save eax, ebx, ecx, edx
    push ebx
    push ecx
    push edx

    mov edi, buffer + 10        ; Point to the end of the buffer. We write backwards.
    mov byte [edi], 0           ; Null-terminate the string for safety (optional for sys_write).

    mov ecx, 1                  ; Counter for number of digits.
    mov ebx, 10                 ; Divisor for number conversion.

print_number_loop:
    xor edx, edx                ; Clear edx for division.
    div ebx                     ; Divide eax by 10. Remainder (digit) in edx. Quotient in eax.

    add edx, '0'                ; Convert digit to its ASCII character representation.
    mov byte [edi], dl          ; Store the ASCII character in the buffer.

    dec edi                     ; Move to the next position in the buffer.
    inc ecx                     ; Increment the digit count.

    cmp eax, 0                  ; Is the quotient 0?
    jne print_number_loop       ; If not, continue the conversion.

    inc edi                     ; Point edi to the first digit.
    dec ecx                     ; ecx now holds the number of digits.

    ; Call a helper function to write the string to stdout
    push edi
    push ecx
    call write_string
    add esp, 8                  ; Clean up stack

    ; Now, print a space
    push space
    push 1
    call write_string
    add esp, 8                  ; Clean up stack

    pop edx                     ; Restore registers.
    pop ecx
    pop ebx
    pop eax
    ret

; --------------------------------------------------------
; Helper subroutine to write a string to stdout.
; Expects the address of the string on the stack, and its length.
; --------------------------------------------------------
write_string:
    push ebp
    mov ebp, esp

    mov eax, 4                  ; sys_write syscall number
    mov ebx, 1                  ; file descriptor 1 (stdout)
    mov ecx, [ebp + 8]          ; address of string from stack
    mov edx, [ebp + 12]         ; length of string from stack
    int 0x80                    ; call kernel

    pop ebp
    ret