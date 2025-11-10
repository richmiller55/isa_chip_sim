# Makefile.mk for assembling and linking x86 assembly files

ASM_SOURCE_DIR := resources/asm_source
ASM_FILE := x86_prime_number.asm
OBJ_FILE := $(ASM_SOURCE_DIR)/$(patsubst %.asm,%.o,$(ASM_FILE))
EXECUTABLE := $(ASM_SOURCE_DIR)/$(patsubst %.asm,%,$(ASM_FILE))

.PHONY: all clean debug

all: $(EXECUTABLE)

$(EXECUTABLE): $(OBJ_FILE)
	@echo "Linking $(OBJ_FILE) to create $(EXECUTABLE)..."
	ld -m elf_i386 -s -o $@ $<

$(OBJ_FILE): $(ASM_SOURCE_DIR)/$(ASM_FILE)
	@echo "Assembling $< to create $@..."
	nasm -f elf -o $@ $<

debug: $(ASM_SOURCE_DIR)/x86_prime_number_debug

$(ASM_SOURCE_DIR)/x86_prime_number_debug: $(ASM_SOURCE_DIR)/x86_prime_number_debug.o
	@echo "Linking for debug..."
	ld -m elf_i386 -g -o $@ $<

$(ASM_SOURCE_DIR)/x86_prime_number_debug.o: $(ASM_SOURCE_DIR)/$(ASM_FILE)
	@echo "Assembling for debug..."
	nasm -g -F dwarf -f elf -o $@ $<

clean:
	@echo "Cleaning up generated files..."
	rm -f $(OBJ_FILE) $(EXECUTABLE) $(ASM_SOURCE_DIR)/x86_prime_number_debug $(ASM_SOURCE_DIR)/x86_prime_number_debug.o