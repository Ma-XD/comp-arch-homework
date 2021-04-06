## Homework 4
# ISA

### Программа находит и дизассемблирует секцию кода (.text) ELF-файла с использованием .symtable.
### ISA: RISC-V RV32I, RV32M.
### Аргументы командной строки:
**<имя_входного_elf_файла> [<имя_выходного_файла>]

### Результат работы программы с файлом [elf_test_2](https://github.com/Ma-XD/comp-arch-homework/blob/main/hw4/test_elf_2): 
        Symbol Table:
          Value			Size		Type		Bind		Vis		Index		Name
    [ 0] 0x00000000  		0   		NOTYPE		LOCAL		DEFAULT		UNDEF 				
    [ 1] 0x00000000  		0   		FILE		UNKNOWN		DEFAULT		ABS 		test2.c		
    [ 2] 0x00000030  		0   		NOTYPE		LOCAL		DEFAULT		0002 		.LBB0_1		
    [ 3] 0x00000040  		0   		NOTYPE		LOCAL		DEFAULT		0002 		.LBB0_2		
    [ 4] 0x00000058  		0   		NOTYPE		LOCAL		DEFAULT		0002 		.LBB0_3		
    [ 5] 0x00000068  		0   		NOTYPE		LOCAL		DEFAULT		0002 		.LBB0_4		
    [ 6] 0x00000000  		124   		UNKNOWN		UNKNOWN		DEFAULT		0002 		main		

    Code:
    00000000: <main>		addi r2, r2, -32
    00000004:			sw r2, r1, 28
    00000008:			sw r2, r8, 24
    0000000c:			addi r8, r2, 32
    00000010:			addi r10, zero, 0
    00000014:			sw r8, r10, -12
    00000018:			addi r11, zero, 64
    0000001c:			sw r8, r11, -16
    00000020:			sw r8, r10, -20
    00000024:			addi r10, zero, 1
    00000028:			sw r8, r10, -24
    0000002c: <LOC_0000002c> 	jal zero, 0
    00000030: <.LBB0_1>		lw r10, r8, -24
    00000034:			lw r11, r8, -16
    00000038: <LOC_00000038> 	bge r10, r11, 0
    0000003c: <LOC_0000003c> 	jal zero, 0
    00000040: <.LBB0_2>		lw r10, r8, -24
    00000044:			mul r10, r10, r10
    00000048:			lw r11, r8, -20
    0000004c:			add r10, r11, r10
    00000050:			sw r8, r10, -20
    00000054: <LOC_00000054> 	jal zero, 0
    00000058: <.LBB0_3>		lw r10, r8, -24
    0000005c:			addi r10, r10, 1
    00000060:			sw r8, r10, -24
    00000064: <LOC_00000064> 	jal zero, 0
    00000068: <.LBB0_4>		lw r10, r8, -20
    0000006c:			lw r8, r2, 24
    00000070:			lw r1, r2, 28
    00000074:			addi r2, r2, 32
    00000078: <LOC_00000078> 	jalr zero, r1, 0
    
Вывод также можно посмотреть в файле [output.txt](https://github.com/Ma-XD/comp-arch-homework/blob/main/hw4/output.txt)
