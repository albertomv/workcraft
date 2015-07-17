/* Generated By:JavaCC: Do not edit this line. VerilogParserConstants.java */
package org.workcraft.plugins.circuit.javacc;


/**
 * Token literal values and constants.
 * Generated by org.javacc.parser.OtherFilesGen#start()
 */
public interface VerilogParserConstants {

  /** End of File. */
  int EOF = 0;
  /** RegularExpression Id. */
  int MODULE = 13;
  /** RegularExpression Id. */
  int ENDMODULE = 14;
  /** RegularExpression Id. */
  int INPUT = 15;
  /** RegularExpression Id. */
  int OUTPUT = 16;
  /** RegularExpression Id. */
  int INOUT = 17;
  /** RegularExpression Id. */
  int REG = 18;
  /** RegularExpression Id. */
  int WIRE = 19;
  /** RegularExpression Id. */
  int NAME = 20;
  /** RegularExpression Id. */
  int HIERARCHICAL_NAME = 21;
  /** RegularExpression Id. */
  int STRING = 22;
  /** RegularExpression Id. */
  int CHAR = 23;
  /** RegularExpression Id. */
  int ESCAPESEQ = 24;
  /** RegularExpression Id. */
  int LOGIC0 = 25;
  /** RegularExpression Id. */
  int LOGIC1 = 26;
  /** RegularExpression Id. */
  int INTEGER = 27;

  /** Lexical state. */
  int DEFAULT = 0;
  /** Lexical state. */
  int WITHIN_SPECIFY = 1;
  /** Lexical state. */
  int WITHIN_PRIMITIVE = 2;

  /** Literal token values. */
  String[] tokenImage = {
    "<EOF>",
    "\" \"",
    "\"\\t\"",
    "\"\\r\"",
    "\"\\n\"",
    "<token of kind 5>",
    "<token of kind 6>",
    "\"specify\"",
    "\"primitive\"",
    "\"endspecify\"",
    "<token of kind 10>",
    "\"endprimitive\"",
    "<token of kind 12>",
    "\"module\"",
    "\"endmodule\"",
    "\"input\"",
    "\"output\"",
    "\"inout\"",
    "\"reg\"",
    "\"wire\"",
    "<NAME>",
    "<HIERARCHICAL_NAME>",
    "<STRING>",
    "<CHAR>",
    "<ESCAPESEQ>",
    "\"1\\\'b0\"",
    "\"1\\\'b1\"",
    "<INTEGER>",
    "\"(\"",
    "\")\"",
    "\"[\"",
    "\"]\"",
    "\",\"",
    "\";\"",
    "\".\"",
  };

}
