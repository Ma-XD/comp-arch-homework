import java.io.*;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class Disassembler {
    private static List<Integer> inputData;
    private static final String ELF = "\u007FELF\u0001\u0001\u0001";
    private static final String TEXT = "\u002E\u0074\u0065\u0078\u0074\u0000";
    private static final String SYMTAB = "\u002E\u0073\u0079\u006D\u0074\u0061\u0062\u0000";
    private static final String STRTAB = "\u002E\u0073\u0074\u0072\u0074\u0061\u0062\u0000";
    private static int headerTextOffset;
    private static int sectionHeaderOffset;
    private static int sectionHeaderEntrySize;
    private static int sectionHeaderStringTableOffset;

    private static void readFile(BufferedReader reader) throws IOException {
        inputData = new ArrayList<>();
        int read;
        while (true) {
            read = reader.read();
            if (read < 0) {
                break;
            }
            inputData.add(read);
        }
        StringBuilder magicNumber = getLine(0);
        if (!magicNumber.toString().equals(ELF)) {
            throw new FileNotFoundException("It is not ELF-file");
        }
    }

    public  static void main(String[] args) throws IOException {
        if (args.length < 1) {
            throw new IllegalArgumentException(
                    "You should run this file with args " +
                    "<input_filename> " +
                    "[<output_filename>]\n"
            );
        }
        final String inputFilename = args[0];
        BufferedReader reader;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(
                            new FileInputStream(inputFilename),
                            StandardCharsets.ISO_8859_1)
            );
        } catch (IOException e) {
            throw new FileNotFoundException("Cannot open file \"" + inputFilename + "\"");
        }
        readFile(reader);
        reader.close();
        StringBuilder code = getText();
        String[] binaryCommands = getBinaryCommands(code);
        ElfSymbol[] elfSymbols = getSymbolTable();
        int virtualAddress = getValue(getLineOfSize(24, 4));
        BufferedWriter writer;
        if (args.length == 2) {
            String outputFilename = args[1];
            try {
                writer = new BufferedWriter(new FileWriter(outputFilename));
            } catch (IOException e) {
                throw new FileNotFoundException("Cannot open file \"" + outputFilename + "\"");
            }
        } else {
            writer = new BufferedWriter(new OutputStreamWriter(System.out));
        }
        writer.write(
                "Symbol Table:\n" +
                        "\t  " + "Value" +
                        "\t\t\t\t" + "Size" +
                        "\t\t" + "Type" +
                        "\t\t" + "Bind" +
                        "\t\t" + "Vis" +
                        "\t\t\t" + "Index" +
                        "\t\t" + "Name" + "\n"
        );
        for (ElfSymbol elfSymbol : elfSymbols) {
            writer.write(elfSymbol.toString() + "\n");
        }
        writer.write("\nCode:\n");
        for (int i = 0; i < binaryCommands.length; i++) {
            writer.write(
                    String.format("%08x", i * 4 + virtualAddress) +
                            ":" + getMark(elfSymbols, i * 4 + virtualAddress, binaryCommands[i]) +
                            "\t" + toStringCommand(binaryCommands[i]) + "\n"
            );
        }
        writer.close();
    }

    private static StringBuilder getText() {
        sectionHeaderOffset = getValue(getLine(32));
        sectionHeaderEntrySize = getValue(getLine(46));
        int sectionHeaderNumber = getValue(getLine(50));
        int offset = sectionHeaderOffset + sectionHeaderNumber * sectionHeaderEntrySize + 16;
        sectionHeaderStringTableOffset = getValue(getLine(offset));
        headerTextOffset = getHeader(TEXT, 6);
        int textOffset = getValue(getLine(headerTextOffset + 16));
        int textSize = getValue(getLine(headerTextOffset + 20));
        return getLineOfSize(textOffset, textSize);
    }

    private static int getHeader(String equal, int size) {
        int header = sectionHeaderOffset;
        String required;
        while (true) {
            int name = getValue(getLine(header));
            required = getLineOfSize(sectionHeaderStringTableOffset + name, size).toString();
            if (required.equals(equal)) {
                break;
            }
            header += sectionHeaderEntrySize;
        }
        return header;
    }

    private static StringBuilder getLine(int start) {
        StringBuilder line = new StringBuilder();
        int i = start;
        while (inputData.get(i) > 0) {
            line.append(Character.toChars(inputData.get(i)));
            i++;
        }
        return line;
    }

    private static StringBuilder getLineOfSize(int start, int size) {
        StringBuilder line = new StringBuilder();
        for (int i = start; i < start + size; i++) {
            line.append(Character.toChars(inputData.get(i)));
        }
        return line;
    }

    private static int getValue(StringBuilder sb) {
        if (sb.length() == 0) {
            return 0;
        }
        StringBuilder result = new StringBuilder();
        for (int i = sb.length() - 1; i >= 0; i--) {
            result.append(String.format("%02x", (int) sb.charAt(i)));
        }
        return Integer.parseInt(result.toString(), 16);
    }

    private static String[] getBinaryCommands(StringBuilder sData) {
        String[] result = new String[sData.length() / 4];
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sData.length(); i++) {
            sb.append(sData.charAt(i));
            if ((i + 1) % 4 == 0) {
                result[i / 4] = toBinaryCode(sb);
                sb = new StringBuilder();
            }
        }
        return result;
    }

    private static String toBinaryCode(StringBuilder sb) {
        StringBuilder result = new StringBuilder();
        for (int i = sb.length() - 1; i >= 0; i--) {
            String b = Integer.toBinaryString(
                    Integer.parseInt(
                            Integer.toHexString(sb.charAt(i)),
                            16
                    )
            );
            result.append(String.format("%08d", Integer.parseInt(b)));
        }
        return result.toString();
    }

    private static ElfSymbol[] getSymbolTable() {
        int headerSymbolTable = getHeader(SYMTAB, 8);
        int Offset = getValue(getLine(headerSymbolTable + 16));
        int size = getValue(getLine(headerSymbolTable + 20));
        int EntrySize = getValue(getLine(headerSymbolTable + 36));
        getLineOfSize(Offset, size);
        int StringTableOffset = getStringTableOffset();
        ElfSymbol[] result = new ElfSymbol[size / EntrySize];
        for (int i = 0; i < result.length; i++) {
            int shift = i * EntrySize + Offset;
            int offset = StringTableOffset + getValue(getLineOfSize(shift, 4));
            ElfSymbol temp = new ElfSymbol();
            temp.setNumber(i);
            temp.setName(getLine(offset).toString());
            temp.setValue(shift);
            temp.setType(shift);
            temp.setBind(shift);
            temp.setVis(shift);
            temp.setSize(shift);
            temp.setIndex(shift);
            result[i] = temp;
        }
        return result;
    }

    private static int getStringTableOffset() {
        int headerStringTable = getHeader(STRTAB, 8);
        return getValue(getLine(headerStringTable + 16));
    }

    private static class ElfSymbol {
        public int number;
        public int value;
        public int size;
        public int type;
        public int bind;
        public int vis;
        public int index;
        public String name;

        public void setNumber(int number) {
            this.number = number;
        }

        public void setName(String name) {
            this.name = name;
        }

        public void setValue(int start) {
            this.value = getValue(getLineOfSize(start + 4, 4));
        }

        public void setSize(int start) {
            this.size = getValue(getLineOfSize(start + 8, 4));
        }

        public void setType(int start) {
            this.type = getValue(getLineOfSize(start + 12, 1));
        }

        public void setBind(int start) {
            this.bind = getValue(getLineOfSize(start + 12, 1));
        }

        public void setVis(int start) {
            this.vis = getValue(getLineOfSize(start + 13, 1));
        }

        public void setIndex(int start) {
            this.index = getValue(getLineOfSize(start + 14, 2));
        }

        private String getStringType() {
            switch(type) {
                case (0):
                    return "NOTYPE";
                case (1):
                    return "OBJECT";
                case (2):
                    return "FUNC";
                case (3):
                    return "SECTION";
                case (4):
                    return "FILE";
                    case (5):
                    return "COMMON";
                case (6):
                    return "TLS";
                case (10):
                    return "LOOS";
                case (12):
                    return "HIOS";
                case (13):
                    return "LOPROC";
                case (15):
                    return "HIPROC";
                default:
                    return "UNKNOWN";
            }

        }

        private String getStringBind() {
            switch(bind) {
                case (0):
                    return "LOCAL";
                case (1):
                    return "GLOBAL";
                case (2):
                    return "WEAK";
                case (10):
                    return "LOOS";
                case (12):
                    return "HIOS";
                case (13):
                    return "LOPROC";
                case (15):
                    return "HIPROC";
                default:
                    return "UNKNOWN";
            }
        }

        private String getStringVis() {
            switch(vis) {
                case (0):
                    return "DEFAULT";
                case (1):
                    return "INTERNAL";
                case (2):
                    return "HIDDEN";
                case (3):
                    return "PROTECTED";
                default:
                    return "UNKNOWN";
            }
        }

        private String getStringIndex() {
            switch(index) {
                case 0:
                    return "UNDEF";
                case 65280:
                    return "LOPROC";
                case 65311:
                    return "HIPROC";
                case 65312:
                    return "LOOS";
                case 65343:
                    return "HIOS";
                case 65521:
                    return "ABS";
                case 65522:
                    return "COMMON";
                case 65535:
                    return "XINDEX";
                default:
                    return String.format("%04x", index);
            }
        }


        public String toString() {
            return "[ " + number + "]" + " 0x" +
                    String.format("%08x", value) +
                    "  \t\t" + size + "   \t\t" +
                    getStringType() + "\t\t" +
                    getStringBind() + "\t\t" +
                    getStringVis() + "\t\t" +
                    getStringIndex() + " \t\t" +
                    name + "\t\t";
        }
    }

    private static String getMark(ElfSymbol[] elfSymbols, int commandAddress, String binaryCommand) {
        int  SectionTextIndex = (headerTextOffset - sectionHeaderOffset) / 40;
        for (ElfSymbol elfSymbol : elfSymbols) {
            if (SectionTextIndex == elfSymbol.index) {
                if (elfSymbol.value == commandAddress) {
                    return " <" + elfSymbol.name + ">" + "\t\t";
                }
            }
        }
        int opcode = getFunction(binaryCommand, 25, 7);
        if (opcode == 111 || opcode == 103 || (opcode == 99 && getFunction(binaryCommand, 17, 3) == 5) ) {
            return " <LOC_" + String.format("%08x", commandAddress) + "> ";
        }
        return "\t\t\t\t";
    }


    private static int getFunction(String str, int start, int size) {
        return Integer.parseInt(str.substring(start, start + size), 2);
    }

    private static String toStringCommand(String binaryCommand) {
        int opcode = getFunction(binaryCommand, 25, 7);
        switch (opcode) {
            case 55:
                return "lui " + UTypeCommand(binaryCommand);
            case 23:
                return "auipc " + UTypeCommand(binaryCommand);
            case 111:
                return "jal " + JTypeCommand(binaryCommand);
            case 103:
                return "jalr " + ITypeCommand(binaryCommand);
            case 99: {
                int funct3 = getFunction(binaryCommand, 17, 3);
                switch (funct3) {
                    case 0:
                        return "beq " + BTypeCommand(binaryCommand);
                    case 1:
                        return "bne " + BTypeCommand(binaryCommand);
                    case 4:
                        return "blt " + BTypeCommand(binaryCommand);
                    case 5:
                        return "bge " + BTypeCommand(binaryCommand);
                    case 6:
                        return "bltu " + BTypeCommand(binaryCommand);
                    case 7:
                        return "bgeu " + BTypeCommand(binaryCommand);
                }
            }
            case 3: {
                int funct3 = getFunction(binaryCommand, 17, 3);
                switch (funct3) {
                    case 0:
                        return "lb " + ITypeCommand(binaryCommand);
                    case 1:
                        return "lh " + ITypeCommand(binaryCommand);
                    case 2:
                        return "lw " + ITypeCommand(binaryCommand);
                    case 4:
                        return "lbu " + ITypeCommand(binaryCommand);
                    case 5:
                        return "lhu " + ITypeCommand(binaryCommand);
                }
            }
            case 35: {
                int funct3 = getFunction(binaryCommand, 17, 3);
                switch (funct3) {
                    case 0:
                        return "sb " + STypeCommand(binaryCommand);
                    case 1:
                        return "sh " + STypeCommand(binaryCommand);
                    case 2:
                        return "sw " + STypeCommand(binaryCommand);
                }
            }
            case 19: {
                int funct3 = getFunction(binaryCommand, 17, 3);
                switch (funct3) {
                    case 0:
                        return "addi " + ITypeCommand(binaryCommand);
                    case 1:
                        return "slli " + ITypeCommand(binaryCommand);
                    case 2:
                        return "slti " + ITypeCommand(binaryCommand);
                    case 3:
                        return "sltiu " + ITypeCommand(binaryCommand);
                    case 4:
                        return "xori " + ITypeCommand(binaryCommand);
                    case 5: {
                        int funct7 = getFunction(binaryCommand, 0, 7);
                        switch (funct7) {
                            case 0:
                                return "srli " + ITypeCommand(binaryCommand);
                            case 32:
                                return "srai " + ITypeCommand(binaryCommand);
                        }
                    }
                    case 6:
                        return "ori " + ITypeCommand(binaryCommand);
                    case 7:
                        return "andi " + ITypeCommand(binaryCommand);
                }
            }
            case 51: {
                int funct3 = getFunction(binaryCommand, 17, 3);
                int funct7 = getFunction(binaryCommand, 0, 7);
                if (funct7 == 0 || funct7 == 32) {
                    switch (funct3) {
                        case 0:
                            switch (funct7) {
                                case 0:
                                    return "add " + RTypeCommand(binaryCommand);
                                case 32:
                                    return "sub " + RTypeCommand(binaryCommand);
                            }
                        case 1:
                            return "sll " + RTypeCommand(binaryCommand);
                        case 2:
                            return "slt " + RTypeCommand(binaryCommand);
                        case 3:
                            return "sltu " + RTypeCommand(binaryCommand);
                        case 4:
                            return "xor " + RTypeCommand(binaryCommand);
                        case 5:
                            switch (funct7) {
                                case 0:
                                    return "srl " + RTypeCommand(binaryCommand);
                                case 32:
                                    return "sra " + RTypeCommand(binaryCommand);
                            }
                        case 6:
                            return "or " + RTypeCommand(binaryCommand);
                        case 7:
                            return "and " + RTypeCommand(binaryCommand);
                    }
                }
                if (funct7 == 1) {
                    switch (funct3) {
                        case 0:
                            return "mul " + RTypeCommand(binaryCommand);
                        case 1:
                            return "mulh " + RTypeCommand(binaryCommand);
                        case 2:
                            return "mulhsu " + RTypeCommand(binaryCommand);
                        case 3:
                            return "mulhu " + RTypeCommand(binaryCommand);
                        case 4:
                            return "div " + RTypeCommand(binaryCommand);
                        case 5:
                            return "divu " + RTypeCommand(binaryCommand);
                        case 6:
                            return "rem " + RTypeCommand(binaryCommand);
                        case 7:
                            return "remu " + RTypeCommand(binaryCommand);
                    }
                }
            }
            case 15: {
                return "fence ";
            }
            case 115: {
                int funct7 = getFunction(binaryCommand, 0, 7);
                switch (funct7) {
                    case 0:
                        return "ecall ";
                    case 32:
                        return "ebreak ";
                }
            }
            default:
                throw new NullPointerException(
                        "Cannot parse command \"" +
                                binaryCommand +
                                "\""
                );
        }
    }

    public static String getRegister(String str, int start) {
        int register = getFunction(str, start,5);
        if (register == 0) {
            return "zero";
        } else {
            return "r" + register;
        }
    }

    public static int getNumber(String imm) {
        if (imm.charAt(0) == '0') {
            return Integer.parseInt(imm, 2);
        } else {
            char[] result = new char[imm.length()];
            for (int i = 0; i < imm.length(); i++) {
                result[i] = imm.charAt(i) == '1' ? '0' : '1';
            }
            return -(Integer.parseInt(String.valueOf(result), 2) + 1);
        }
    }

    private static String RTypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 20) + ", " +
                getRegister(binaryCommand, 12) + ", " +
                getRegister(binaryCommand, 7);
    }

    private static String ITypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 20) + ", " +
                getRegister(binaryCommand, 12) + ", " +
                getNumber(binaryCommand.substring(0, 12));
    }

    public static String STypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 12) + ", " +
                getRegister(binaryCommand, 7) + ", " +
                getNumber(
                        binaryCommand.substring(0, 7) +
                                binaryCommand.substring(20, 25)
                );
    }

    public static String BTypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 12) + ", " +
                getRegister(binaryCommand, 7)  + ", " +
                (getNumber(
                        binaryCommand.substring(0, 7) +
                                binaryCommand.charAt(24) +
                                binaryCommand.substring(20, 24)
                ) << 1);
    }

    public static String UTypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 20) + ", " +
                (getNumber(binaryCommand.substring(0, 15)) << 12);

    }

    public static String JTypeCommand(String binaryCommand) {
        return getRegister(binaryCommand, 20) + ", " +
                (getNumber(binaryCommand.substring(0, 15)) << 1);
    }
}