package se.havochvatten.unionvms;

import org.apache.commons.csv.CSVRecord;

import java.util.Map;
import java.util.TreeMap;

public class AISEncoder {

    private Map<String, Character> binToAsciiMap;
    private Map<Character, String> asciiToBinMap;
    private Map<String, Integer> shipTypes;

    public AISEncoder() {
        buildBinToAsciiMap();
        buildAsciiToBinMap();
        buildShipTypeMap();
    }

    // [# Timestamp, Type of mobile, MMSI, Latitude, Longitude, Navigational status, ROT, SOG, COG, Heading, IMO, Callsign, Name, Ship type, Cargo type, Width, Length, Type of position fixing device, Draught, Destination, ETA, Data source type, A, B, C, D]
    // 13/05/2019 00:00:00,Class A,219000028,55.858673,12.835107,Under way using engine,0.0,0.0,221.7,74,Unknown,,,Undefined,,,,Undefined,,,,AIS,,,,
    public String encode(CSVRecord record) {
        return "!AIVDM,1,1,,B," + binaryStringToSymbol(encodePayload(record)) + ",0*5C";
    }

    public String encodeType5(CSVRecord record) {
        return "!AIVDM,1,1,,B," + binaryStringToSymbol(encodeType5Payload(record)) + ",0*5C";
    }

    private String encodePayload(CSVRecord record) {
        StringBuilder binaryString = new StringBuilder();
        // MessageType, length 6
        binaryString.append(prependZeros(Integer.toBinaryString(1), 6));
        // Repeat indicator, length 2
        binaryString.append("00");
        // MMSI, length 30
        binaryString.append(prependZeros(Integer.toBinaryString(Integer.parseInt(record.get("MMSI"))), 30));
        // Navigation Status, length 4
        binaryString.append(prependZeros("0", 4));
        // Rate of turn, length 8
        binaryString.append(prependZeros("0", 8));
        // Speed over ground, length 10
        String sogString = record.get("SOG");
        Double sog;
        if (!sogString.isEmpty()) {
            sog = Double.parseDouble(sogString);
        } else {
            sog = 102.3;
        }
        binaryString.append(prependZeros(Integer.toBinaryString((int) (sog * 10)), 10));
        // Position accuracy, length 1
        binaryString.append("0");
        // Longitude, length 28
        double lon = Double.parseDouble(record.get("Longitude"));
        binaryString.append(prependZeros(Integer.toBinaryString((int) (lon * 10000 * 60)), 28));
        // Latitude, length 27
        double lat = Double.parseDouble(record.get("Latitude"));
        binaryString.append(prependZeros(Integer.toBinaryString((int) (lat * 10000 * 60)), 27));
        // Course over ground, length 12
        String cogString = record.get("COG");
        double cog;
        if (!cogString.isEmpty()) {
            cog = Double.parseDouble(cogString);
        } else {
            cog = 3600;
        }
        binaryString.append(prependZeros(Integer.toBinaryString((int) (cog * 10)), 12));
        // True Heading, length 9
        String headingString = record.get("Heading");
        int heading;
        if (!headingString.isEmpty()) {
            heading = Integer.parseInt(headingString);
        } else {
            heading = 511;
        }
        binaryString.append(prependZeros(Integer.toBinaryString(heading), 9));
        // Timestamp, length 6 (second)
        int second = Integer.parseInt(record.get("# Timestamp").split(":")[2]);
        binaryString.append(prependZeros(Integer.toBinaryString(second), 6));
        // Maneuver Indicator, length 2
        binaryString.append(prependZeros("1", 2));
        // Spare, length 3
        binaryString.append(prependZeros("0", 3));
        // RAIM, length 1
        binaryString.append(prependZeros("0", 1));
        // Radio status, length 19
        binaryString.append(prependZeros("0", 19));
        return binaryString.toString();
    }

    private String encodeType5Payload(CSVRecord record) {
        StringBuilder binaryString = new StringBuilder();
        // MessageType, length 6
        binaryString.append(prependZeros(Integer.toBinaryString(5), 6));
        // Repeat indicator, length 2
        binaryString.append("00");
        // MMSI, length 30
        binaryString.append(prependZeros(Integer.toBinaryString(Integer.parseInt(record.get("MMSI"))), 30));
        // AIS version, length 2
        binaryString.append(prependZeros("0", 2));
        // IMO number, length 30
        binaryString.append(prependZeros("0", 30));
        // Callsign, length 42
        binaryString.append(prependZeros(symbolToBinary(record.get("Callsign")), 42));
        // Vessel Name, length 120
        binaryString.append(prependZeros(symbolToBinary(record.get("Name")), 120));
        // Ship Type, length 8
        binaryString.append(prependZeros(Integer.toBinaryString(shipTypes.getOrDefault(record.get("Ship type"), 0)), 8));
        // Dimension to Bow, length 9
        binaryString.append(prependZeros("0", 9));
        // Dimension to Stern, length 9
        binaryString.append(prependZeros("0", 9));
        // Dimension to Port, length 6
        binaryString.append(prependZeros("0", 6));
        // Dimension to Starboard, length 6
        binaryString.append(prependZeros("0", 6));
        // Position Fix Type, length 4
        binaryString.append(prependZeros("0", 4));
        // ETA month (UTC), length 4
        binaryString.append(prependZeros("0", 4));
        // ETA day (UTC), length 5
        binaryString.append(prependZeros("0", 5));
        // ETA hour (UTC), length 5
        binaryString.append(prependZeros("0", 5));
        // ETA minute (UTC), length 6
        binaryString.append(prependZeros("0", 6));
        // Draught, length 8
        binaryString.append(prependZeros("0", 8));
        // Destination, length 120
        binaryString.append(prependZeros("0", 120));
        // DTE, length 1
        binaryString.append(prependZeros("0", 1));
        // Spare, length 1
        binaryString.append(prependZeros("0", 1));
        return binaryString.toString();
    }

    private String prependZeros(String value, int expectedLength) {
        if (value.length() < expectedLength) {
            return new String(new char[expectedLength - value.length()]).replace("\0", "0") + value;
        }
        return value;
    }

    private String symbolToBinary(String symbolString) {
        StringBuilder binaryString = new StringBuilder();
        for (char c : symbolString.toCharArray()) {
            binaryString.append(asciiToBinMap.get(c));
        }
        return binaryString.toString();
    }

    private String binaryStringToSymbol(String binary) {
        StringBuilder symbolString = new StringBuilder();
        for (int i = 0; i < binary.length() - 6; i += 6) {
            symbolString.append(binToAsciiMap.get(binary.substring(i, i + 6)));
        }
        return symbolString.toString();
    }

    private void buildBinToAsciiMap() {
        // Table 3 sixbit ascii
        // http://catb.org/gpsd/AIVDM.html

        binToAsciiMap = new TreeMap<>();

        binToAsciiMap.put("000000", '0');
        binToAsciiMap.put("000001", '1');
        binToAsciiMap.put("000010", '2');
        binToAsciiMap.put("000011", '3');
        binToAsciiMap.put("000100", '4');
        binToAsciiMap.put("000101", '5');
        binToAsciiMap.put("000110", '6');
        binToAsciiMap.put("000111", '7');
        binToAsciiMap.put("001000", '8');
        binToAsciiMap.put("001001", '9');
        binToAsciiMap.put("001010", ':');
        binToAsciiMap.put("001011", ';');
        binToAsciiMap.put("001100", '<');
        binToAsciiMap.put("001101", '=');
        binToAsciiMap.put("001110", '>');
        binToAsciiMap.put("001111", '?');


        binToAsciiMap.put("010000", '@');
        binToAsciiMap.put("010001", 'A');
        binToAsciiMap.put("010010", 'B');
        binToAsciiMap.put("010011", 'C');
        binToAsciiMap.put("010100", 'D');
        binToAsciiMap.put("010101", 'E');
        binToAsciiMap.put("010110", 'F');
        binToAsciiMap.put("010111", 'G');
        binToAsciiMap.put("011000", 'H');
        binToAsciiMap.put("011001", 'I');
        binToAsciiMap.put("011010", 'J');
        binToAsciiMap.put("011011", 'K');
        binToAsciiMap.put("011100", 'L');
        binToAsciiMap.put("011101", 'M');
        binToAsciiMap.put("011110", 'N');
        binToAsciiMap.put("011111", 'O');

        binToAsciiMap.put("100000", 'P');
        binToAsciiMap.put("100001", 'Q');
        binToAsciiMap.put("100010", 'R');
        binToAsciiMap.put("100011", 'S');
        binToAsciiMap.put("100100", 'T');
        binToAsciiMap.put("100101", 'U');
        binToAsciiMap.put("100110", 'V');
        binToAsciiMap.put("100111", 'W');
        binToAsciiMap.put("101000", '`');
        binToAsciiMap.put("101001", 'a');
        binToAsciiMap.put("101010", 'b');
        binToAsciiMap.put("101011", 'c');
        binToAsciiMap.put("101100", 'd');
        binToAsciiMap.put("101101", 'e');
        binToAsciiMap.put("101110", 'f');
        binToAsciiMap.put("101111", 'g');

        binToAsciiMap.put("110000", 'h');
        binToAsciiMap.put("110001", 'i');
        binToAsciiMap.put("110010", 'j');
        binToAsciiMap.put("110011", 'k');
        binToAsciiMap.put("110100", 'l');
        binToAsciiMap.put("110101", 'm');
        binToAsciiMap.put("110110", 'n');
        binToAsciiMap.put("110111", 'o');
        binToAsciiMap.put("111000", 'p');
        binToAsciiMap.put("111001", 'q');
        binToAsciiMap.put("111010", 'r');
        binToAsciiMap.put("111011", 's');
        binToAsciiMap.put("111100", 't');
        binToAsciiMap.put("111101", 'u');
        binToAsciiMap.put("111110", 'v');
        binToAsciiMap.put("111111", 'w');
    }

    private void buildAsciiToBinMap() {
        asciiToBinMap = new TreeMap<>();

        asciiToBinMap.put('@', "000000");
        asciiToBinMap.put('A', "000001");
        asciiToBinMap.put('B', "000010");
        asciiToBinMap.put('C', "000011");
        asciiToBinMap.put('D', "000100");
        asciiToBinMap.put('E', "000101");
        asciiToBinMap.put('F', "000110");
        asciiToBinMap.put('G', "000111");
        asciiToBinMap.put('H', "001000");
        asciiToBinMap.put('I', "001001");
        asciiToBinMap.put('J', "001010");
        asciiToBinMap.put('K', "001011");
        asciiToBinMap.put('L', "001100");
        asciiToBinMap.put('M', "001101");
        asciiToBinMap.put('N', "001110");
        asciiToBinMap.put('O', "001111");


        asciiToBinMap.put('P', "010000");
        asciiToBinMap.put('Q', "010001");
        asciiToBinMap.put('R', "010010");
        asciiToBinMap.put('S', "010011");
        asciiToBinMap.put('T', "010100");
        asciiToBinMap.put('U', "010101");
        asciiToBinMap.put('V', "010110");
        asciiToBinMap.put('W', "010111");
        asciiToBinMap.put('X', "011000");
        asciiToBinMap.put('Y', "011001");
        asciiToBinMap.put('Z', "011010");
        asciiToBinMap.put('[', "011011");
        asciiToBinMap.put('\\', "011100");
        asciiToBinMap.put(']', "011101");
        asciiToBinMap.put('^', "011110");
        asciiToBinMap.put('_', "011111");

        asciiToBinMap.put(' ', "100000");
        asciiToBinMap.put('!', "100001");
        asciiToBinMap.put('\"', "100010");
        asciiToBinMap.put('#', "100011");
        asciiToBinMap.put('$', "100100");
        asciiToBinMap.put('%', "100101");
        asciiToBinMap.put('&', "100110");
        asciiToBinMap.put('\'', "100111");
        asciiToBinMap.put('(', "101000");
        asciiToBinMap.put(')', "101001");
        asciiToBinMap.put('*', "101010");
        asciiToBinMap.put('+', "101011");
        asciiToBinMap.put(',', "101100");
        asciiToBinMap.put('-', "101101");
        asciiToBinMap.put('.', "101110");
        asciiToBinMap.put('/', "101111");

        asciiToBinMap.put('0', "110000");
        asciiToBinMap.put('1', "110001");
        asciiToBinMap.put('2', "110010");
        asciiToBinMap.put('3', "110011");
        asciiToBinMap.put('4', "110100");
        asciiToBinMap.put('5', "110101");
        asciiToBinMap.put('6', "110110");
        asciiToBinMap.put('7', "110111");
        asciiToBinMap.put('8', "111000");
        asciiToBinMap.put('9', "111001");
        asciiToBinMap.put(':', "111010");
        asciiToBinMap.put(';', "111011");
        asciiToBinMap.put('<', "111100");
        asciiToBinMap.put('=', "111101");
        asciiToBinMap.put('>', "111110");
        asciiToBinMap.put('?', "111111");
    }

    private void buildShipTypeMap() {
        shipTypes = new TreeMap<>();
        shipTypes.put("Fishing", 30);
        shipTypes.put("Towing", 31);
        shipTypes.put("Sailing", 36);
        shipTypes.put("Pleasure", 37);
        shipTypes.put("Passenger", 60);
        shipTypes.put("Cargo", 70);
        shipTypes.put("Tanker", 80);
    }
}