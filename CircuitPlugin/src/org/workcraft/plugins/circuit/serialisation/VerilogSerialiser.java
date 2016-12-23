/*
*
* Copyright 2008,2009 Newcastle University
*
* This file is part of Workcraft.
*
* Workcraft is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Workcraft is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Workcraft.  If not, see <http://www.gnu.org/licenses/>.
*
*/

package org.workcraft.plugins.circuit.serialisation;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.UUID;

import org.workcraft.dom.Model;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.exceptions.ArgumentException;
import org.workcraft.formula.BooleanFormula;
import org.workcraft.formula.BooleanVariable;
import org.workcraft.formula.DumbBooleanWorker;
import org.workcraft.formula.Literal;
import org.workcraft.formula.utils.BooleanUtils;
import org.workcraft.formula.utils.FormulaToString;
import org.workcraft.formula.utils.FormulaToString.Style;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.Contact;
import org.workcraft.plugins.circuit.FunctionComponent;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.verilog.SubstitutionRule;
import org.workcraft.plugins.circuit.verilog.SubstitutionUtils;
import org.workcraft.serialisation.Format;
import org.workcraft.serialisation.ModelSerialiser;
import org.workcraft.serialisation.ReferenceProducer;
import org.workcraft.util.Func;
import org.workcraft.util.Hierarchy;
import org.workcraft.util.LogUtils;

public class VerilogSerialiser implements ModelSerialiser {

    private static final String KEYWORD_OUTPUT = "output";
    private static final String KEYWORD_INPUT = "input";
    private static final String KEYWORD_MODULE = "module";
    private static final String KEYWORD_ENDMODULE = "endmodule";
    private static final String KEYWORD_ASSIGN = "assign";

    class ReferenceResolver implements ReferenceProducer {
        HashMap<Object, String> refMap = new HashMap<>();

        @Override
        public String getReference(Object obj) {
            return refMap.get(obj);
        }
    }

    @Override
    public ReferenceProducer serialise(Model model, OutputStream out, ReferenceProducer refs) {
        if (model instanceof Circuit) {
            PrintWriter writer = new PrintWriter(out);
            writer.println("// Verilog netlist file generated by Workcraft -- http://workcraft.org/\n");
            writeCircuit(writer, (Circuit) model);
            writer.close();
        } else {
            throw new ArgumentException("Model class not supported: " + model.getClass().getName());
        }
        return new ReferenceResolver();
    }

    @Override
    public boolean isApplicableTo(Model model) {
        return model instanceof Circuit;
    }

    @Override
    public String getDescription() {
        return "Workcraft Verilog serialiser";
    }

    @Override
    public String getExtension() {
        return ".v";
    }

    @Override
    public UUID getFormatUUID() {
        return Format.VERILOG;
    }

    private final HashMap<Contact, String> contactWires = new HashMap<>();

    private String getWireName(Circuit circuit, Contact contact) {
        String result = contactWires.get(contact);
        if (result == null) {
            if (!circuit.getPreset(contact).isEmpty() || !circuit.getPostset(contact).isEmpty()) {
                Contact signal = CircuitUtils.findSignal(circuit, contact, false);
                Node parent = signal.getParent();
                boolean isAssignOutput = false;
                if (parent instanceof FunctionComponent) {
                    FunctionComponent component = (FunctionComponent) parent;
                    isAssignOutput = signal.isOutput() && !component.isMapped();
                }
                if (isAssignOutput) {
                    result = CircuitUtils.getSignalName(circuit, signal);
                } else {
                    result = CircuitUtils.getContactName(circuit, signal);
                }
            }
            if (result != null) {
                contactWires.put(contact, result);
            }
        }
        return result;
    }

    private void writeCircuit(PrintWriter out, Circuit circuit) {
        contactWires.clear();
        writeHeader(out, circuit);
        writeInstances(out, circuit);
        writeInitialState(out, circuit);
        out.println(KEYWORD_ENDMODULE);
    }

    private void writeHeader(PrintWriter out, Circuit circuit) {
        String topName = circuit.getTitle();
        if ((topName == null) || topName.isEmpty()) {
            topName = "UNTITLED";
            LogUtils.logWarningLine("The top module does not have a name. Exporting as '" + topName + "' module.");
        }
        out.print(KEYWORD_MODULE + " " + topName + " (");
        String inputPorts = "";
        String outputPorts = "";
        boolean isFirstPort = true;
        for (Contact contact: circuit.getPorts()) {
            if (isFirstPort) {
                isFirstPort = false;
            } else {
                out.print(", ");
            }
            String contactRef = circuit.getNodeReference(contact);
            String contactFlatName = NamespaceHelper.hierarchicalToFlatName(contactRef);
            out.print(contactFlatName);
            if (contact.isInput()) {
                if (!inputPorts.isEmpty()) {
                    inputPorts += ", ";
                }
                inputPorts += contactFlatName;
            } else {
                if (!outputPorts.isEmpty()) {
                    outputPorts += ", ";
                }
                outputPorts += contactFlatName;
            }
        }
        out.println(");");
        if (!inputPorts.isEmpty()) {
            out.println("    " + KEYWORD_INPUT + " " + inputPorts + ";");
        }
        if (!outputPorts.isEmpty()) {
            out.println("    " + KEYWORD_OUTPUT + " " + outputPorts + ";");
        }
        out.println();
    }

    private void writeInstances(PrintWriter out, Circuit circuit) {
        HashMap<String, SubstitutionRule> substitutionRules = SubstitutionUtils.readSubsritutionRules();
        // Write out assign statements
        boolean hasAssignments = false;
        for (FunctionComponent component: Hierarchy.getDescendantsOfType(circuit.getRoot(), FunctionComponent.class)) {
            if (!component.isMapped()) {
                if (writeAssigns(out, circuit, component)) {
                    hasAssignments = true;
                } else {
                    String ref = circuit.getNodeReference(component);
                    LogUtils.logErrorLine("Unmapped component '" + ref + "' cannot be exported as assign statements.");
                }
            }
        }
        if (hasAssignments) {
            out.print("\n");
        }
        // Write out mapped components
        for (FunctionComponent component: Hierarchy.getDescendantsOfType(circuit.getRoot(), FunctionComponent.class)) {
            if (component.isMapped()) {
                writeInstance(out, circuit, component, substitutionRules);
            }
        }
    }

    private boolean writeAssigns(PrintWriter out, Circuit circuit, FunctionComponent component) {
        boolean result = false;
        String instanceRef = circuit.getNodeReference(component);
        String instanceFlatName = NamespaceHelper.hierarchicalToFlatName(instanceRef);
        LogUtils.logWarningLine("Component '" + instanceFlatName + "' is not associated to a module and is exported as assign statements.");
        HashMap<String, BooleanFormula> signals = getSignalMap(circuit);
        LinkedList<BooleanVariable> variables = new LinkedList<>();
        LinkedList<BooleanFormula> values = new LinkedList<>();
        for (FunctionContact contact: component.getFunctionContacts()) {
            if (contact.isOutput()) continue;
            String wireName = getWireName(circuit, contact);
            BooleanFormula wire = signals.get(wireName);
            if (wire != null) {
                variables.add(contact);
                values.add(wire);
            }
        }
        for (FunctionContact contact: component.getFunctionContacts()) {
            if (contact.isInput()) continue;
            String formula = null;
            String wireName = getWireName(circuit, contact);
            if ((wireName == null) || wireName.isEmpty()) {
                String contactName = contact.getName();
                LogUtils.logWarningLine("In component '" + instanceFlatName + "' contact '" + contactName + "' is disconnected.");
                continue;
            }
            BooleanFormula setFunction = BooleanUtils.cleverReplace(contact.getSetFunction(), variables, values);
            String setFormula = FormulaToString.toString(setFunction, Style.VERILOG);
            BooleanFormula resetFunction = BooleanUtils.cleverReplace(contact.getResetFunction(), variables, values);
            if (resetFunction != null) {
                resetFunction = new DumbBooleanWorker().not(resetFunction);
            }
            String resetFormula = FormulaToString.toString(resetFunction, Style.VERILOG);
            if (!setFormula.isEmpty() && !resetFormula.isEmpty()) {
                formula = setFormula + " | " + wireName + " & (" + resetFormula + ")";
            } else if (!setFormula.isEmpty()) {
                formula = setFormula;
            } else if (!resetFormula.isEmpty()) {
                formula = resetFormula;
            }
            if ((formula != null) && !formula.isEmpty()) {
                out.println("    " + KEYWORD_ASSIGN + " " + wireName + " = " + formula + ";");
                result = true;
            }
        }
        return result;
    }

    private HashMap<String, BooleanFormula> getSignalMap(Circuit circuit) {
        HashMap<String, BooleanFormula> result = new HashMap<>();
        for (FunctionContact contact: circuit.getFunctionContacts()) {
            String signalName = null;
            BooleanFormula signal = null;
            if (contact.isDriver()) {
                signalName = getWireName(circuit, contact);
                if (contact.isPort()) {
                    signal = contact;
                } else {
                    signal = new Literal(signalName);
                }
            }
            if ((signalName != null) && (signal != null)) {
                result.put(signalName, signal);
            }
        }
        return result;
    }

    private void writeInstance(PrintWriter out, Circuit circuit, FunctionComponent component,
            HashMap<String, SubstitutionRule> substitutionRules) {
        String instanceRef = circuit.getNodeReference(component);
        String instanceFlatName = NamespaceHelper.hierarchicalToFlatName(instanceRef);
        String moduleName = component.getModule();
        SubstitutionRule substitutionRule = substitutionRules.get(moduleName);
        if (substitutionRule != null) {
            String newModuleName = substitutionRule.newName;
            if (newModuleName != null) {
                LogUtils.logInfoLine("In component '" + instanceFlatName + "' renaming module '" + moduleName + "' to '" + newModuleName + "'.");
                moduleName = newModuleName;
            }
        }
        if (component.getIsZeroDelay() && (component.isBuffer() || component.isInverter())) {
            out.println("    // This inverter should have a short delay");
        }
        out.print("    " + moduleName + " " + instanceFlatName + " (");
        boolean first = true;
        for (Contact contact: component.getContacts()) {
            if (first) {
                first = false;
            } else {
                out.print(", ");
            }
            String wireName = getWireName(circuit, contact);
            if ((wireName == null) || wireName.isEmpty()) {
                String contactName = contact.getName();
                LogUtils.logWarningLine("In component '" + instanceFlatName + "' contact '" + contactName + "' is disconnected.");
                wireName = "";
            }
            String contactName = SubstitutionUtils.getContactSubstitutionName(contact, substitutionRule, instanceFlatName);
            out.print("." + contactName + "(" + wireName + ")");
        }
        out.print(");\n");
    }

    private void writeInitialState(PrintWriter out, Circuit circuit) {
        HashSet<Contact> contacts = new HashSet<>();
        for (Contact contact: Hierarchy.getDescendantsOfType(circuit.getRoot(), Contact.class, new Func<Contact, Boolean>() {
            @Override
            public Boolean eval(Contact arg) {
                return arg.isPort() != arg.isOutput();
            }
        })) {
            contacts.add(contact);
        }
        out.println();
        out.println("    // signal values at the initial state:");
        out.print("    //");
        for (Contact contact: contacts) {
            String wireName = getWireName(circuit, contact);
            if ((wireName != null) && !wireName.isEmpty()) {
                out.print(" ");
                if (!contact.getInitToOne()) {
                    out.print("!");
                }
                out.print(wireName);
            }
        }
        out.println();
    }

}
