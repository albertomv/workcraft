package org.workcraft.plugins.circuit.tools;

import java.awt.Color;
import java.util.Collection;
import java.util.HashSet;

import org.workcraft.Trace;
import org.workcraft.dom.Container;
import org.workcraft.dom.Node;
import org.workcraft.dom.hierarchy.NamespaceHelper;
import org.workcraft.dom.hierarchy.NamespaceProvider;
import org.workcraft.dom.math.MathModel;
import org.workcraft.dom.visual.HitMan;
import org.workcraft.dom.visual.VisualGroup;
import org.workcraft.dom.visual.VisualModel;
import org.workcraft.dom.visual.VisualNode;
import org.workcraft.dom.visual.VisualPage;
import org.workcraft.gui.events.GraphEditorMouseEvent;
import org.workcraft.gui.graph.tools.ContainerDecoration;
import org.workcraft.gui.graph.tools.Decoration;
import org.workcraft.gui.graph.tools.Decorator;
import org.workcraft.gui.graph.tools.GraphEditor;
import org.workcraft.plugins.circuit.Circuit;
import org.workcraft.plugins.circuit.CircuitSettings;
import org.workcraft.plugins.circuit.CircuitUtils;
import org.workcraft.plugins.circuit.FunctionContact;
import org.workcraft.plugins.circuit.VisualCircuit;
import org.workcraft.plugins.circuit.VisualCircuitComponent;
import org.workcraft.plugins.circuit.VisualCircuitConnection;
import org.workcraft.plugins.circuit.VisualContact;
import org.workcraft.plugins.circuit.VisualFunctionComponent;
import org.workcraft.plugins.circuit.VisualJoint;
import org.workcraft.plugins.circuit.stg.CircuitToStgConverter;
import org.workcraft.plugins.petri.Place;
import org.workcraft.plugins.petri.Transition;
import org.workcraft.plugins.shared.CommonSimulationSettings;
import org.workcraft.plugins.stg.LabelParser;
import org.workcraft.plugins.stg.SignalTransition;
import org.workcraft.plugins.stg.VisualSignalTransition;
import org.workcraft.plugins.stg.generator.SignalStg;
import org.workcraft.plugins.stg.tools.StgSimulationTool;
import org.workcraft.util.Func;
import org.workcraft.util.Pair;

public class CircuitSimulationTool extends StgSimulationTool {
    private CircuitToStgConverter converter;

    @Override
    public void generateUnderlyingModel(VisualModel model) {
        VisualCircuit circuit = (VisualCircuit) model;
        converter = new CircuitToStgConverter(circuit);
        setUnderlyingModel(converter.getStg());
    }

    @Override
    public void setTrace(Trace mainTrace, Trace branchTrace, GraphEditor editor) {
        Trace circuitMainTrace = convertStgTraceToCircuitTrace(mainTrace);
        if (circuitMainTrace != null) {
            System.out.println("Main trace conversion:");
            System.out.println("  original: " + mainTrace);
            System.out.println("  circuit:  " + circuitMainTrace);
        }
        Trace circuitBranchTrace = convertStgTraceToCircuitTrace(branchTrace);
        if (circuitBranchTrace != null) {
            System.out.println("Branch trace conversion:");
            System.out.println("  original: " + branchTrace);
            System.out.println("  circuit:  " + circuitBranchTrace);
        }
        super.setTrace(circuitMainTrace, circuitBranchTrace, editor);
    }

    private Trace convertStgTraceToCircuitTrace(Trace trace) {
        Trace circuitTrace = null;
        if (trace != null) {
            circuitTrace = new Trace();
            for (String ref: trace) {
                Transition t = getBestTransitionToFire(ref);
                if (t == null) {
                    String flatRef = NamespaceHelper.hierarchicalToFlatName(ref);
                    t = getBestTransitionToFire(flatRef);
                }
                if (t != null) {
                    String circuitRef = getUnderlyingStg().getNodeReference(t);
                    circuitTrace.add(circuitRef);
                    getUnderlyingStg().fire(t);
                }
            }
            writeModelState(initialState);
        }
        return circuitTrace;
    }

    private Transition getBestTransitionToFire(String ref) {
        Transition result = null;
        if (ref != null) {
            String parentName = NamespaceHelper.getParentReference(ref);
            Node parent = getUnderlyingStg().getNodeByReference(parentName);
            String nameWithInstance = NamespaceHelper.getReferenceName(ref);
            String requiredName = LabelParser.getTransitionName(nameWithInstance);
            if ((parent instanceof NamespaceProvider) && (requiredName != null)) {
                for (Transition transition: getUnderlyingStg().getTransitions()) {
                    if (transition.getParent() != parent) continue;
                    if (!isEnabledNode(transition)) continue;
                    String existingRef = getUnderlyingStg().getNodeReference((NamespaceProvider) parent, transition);
                    String existingName = LabelParser.getTransitionName(existingRef);
                    if (requiredName.equals(existingName)) {
                        result = transition;
                        break;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void applySavedState(final GraphEditor editor) {
        if ((savedState == null) || savedState.isEmpty()) {
            return;
        }
        MathModel model = editor.getModel().getMathModel();
        if (model instanceof Circuit) {
            editor.getWorkspaceEntry().saveMemento();
            Circuit circuit = (Circuit) model;
            for (FunctionContact contact: circuit.getFunctionContacts()) {
                String contactName = CircuitUtils.getSignalName(circuit, contact);
                String oneName = SignalStg.getHighName(contactName);
                Node underlyingOneNode = getUnderlyingStg().getNodeByReference(oneName);
                if ((underlyingOneNode instanceof Place) && savedState.containsKey(underlyingOneNode)) {
                    boolean signalLevel = savedState.get(underlyingOneNode) > 0;
                    contact.setInitToOne(signalLevel);
                }
            }
        }
    }

    @Override
    public void initialiseSignalState() {
        super.initialiseSignalState();
        for (String signalName: signalDataMap.keySet()) {
            SignalData signalState = signalDataMap.get(signalName);
            String zeroName = SignalStg.getLowName(signalName);
            Node zeroNode = getUnderlyingStg().getNodeByReference(zeroName);
            if (zeroNode instanceof Place) {
                Place zeroPlace = (Place) zeroNode;
                signalState.value = (zeroPlace.getTokens() > 0) ? SignalState.LOW : SignalState.HIGH;
            }
            String oneName = SignalStg.getHighName(signalName);
            Node oneNode = getUnderlyingStg().getNodeByReference(oneName);
            if (oneNode instanceof Place) {
                Place onePlace = (Place) oneNode;
                signalState.value = (onePlace.getTokens() > 0) ? SignalState.HIGH : SignalState.LOW;
            }
        }
    }

    // Return all enabled transitions associated with the contact
    public HashSet<SignalTransition> getContactExcitedTransitions(VisualContact contact) {
        HashSet<SignalTransition> result = new HashSet<>();
        if ((converter != null) && contact.isDriver()) {
            SignalStg signalStg = converter.getSignalStg(contact);
            if (signalStg != null) {
                for (VisualSignalTransition transition: signalStg.getAllTransitions()) {
                    if (isEnabledNode(transition.getReferencedTransition())) {
                        result.add(transition.getReferencedTransition());
                    }
                }
            }
        }
        return result;
    }

    @Override
    public void mousePressed(GraphEditorMouseEvent e) {
        Node node = HitMan.hitDeepest(e.getPosition(), e.getModel().getRoot(),
                new Func<Node, Boolean>() {
                    @Override
                    public Boolean eval(Node node) {
                        return (node instanceof VisualFunctionComponent) || (node instanceof VisualContact);
                    }
                });

        VisualContact contact = null;
        if (node instanceof VisualCircuitComponent) {
            VisualFunctionComponent component = (VisualFunctionComponent) node;
            if (!component.getIsZeroDelay()) {
                Collection<VisualContact> outputContacts = component.getVisualOutputs();
                if (outputContacts.size() == 1) {
                    contact = outputContacts.iterator().next();
                }
            }
        } else if (node instanceof VisualContact) {
            contact = (VisualContact) node;
        }

        if (contact != null) {
            HashSet<SignalTransition> transitions = getContactExcitedTransitions(contact);
            SignalTransition transition = null;
            Node traceCurrentNode = getTraceCurrentNode();
            if (transitions.contains(traceCurrentNode)) {
                transition = (SignalTransition) traceCurrentNode;
            } else if (!transitions.isEmpty()) {
                transition = transitions.iterator().next();
            }
            if (transition != null) {
                executeTransition(e.getEditor(), transition);
            }
        }
    }

    @Override
    public boolean isContainerExcited(Container container) {
        if (excitedContainers.containsKey(container)) return excitedContainers.get(container);
        boolean ret = false;
        for (Node node: container.getChildren()) {
            if (node instanceof VisualContact) {
                HashSet<SignalTransition> transitions = getContactExcitedTransitions((VisualContact) node);
                ret = ret || !transitions.isEmpty();
            }
            if (node instanceof Container) {
                ret = ret || isContainerExcited((Container) node);
            }
            if (ret) {
                break;
            }
        }
        excitedContainers.put(container, ret);
        return ret;
    }

    @Override
    public String getHintMessage() {
        return "Click on a highlighted contact to toggle its signal state.";
    }

    @Override
    public Decorator getDecorator(final GraphEditor editor) {
        return new Decorator() {
            @Override
            public Decoration getDecoration(Node node) {
                if (converter == null) return null;
                if (node instanceof VisualFunctionComponent) {
                    return getFunctionComponentDecoration((VisualFunctionComponent) node);
                }
                if (node instanceof VisualContact) {
                    return getContactDecoration((VisualContact) node);
                }
                if (node instanceof VisualCircuitConnection) {
                    return getConnectionOrJointDecoration((VisualCircuitConnection) node);
                }
                if (node instanceof VisualJoint) {
                    return getConnectionOrJointDecoration((VisualJoint) node);
                }
                if (node instanceof VisualPage || node instanceof VisualGroup) {
                    return getContainerDecoration((Container) node);
                }
                return null;
            }
        };
    }

    protected Decoration getFunctionComponentDecoration(VisualFunctionComponent component) {
        Collection<VisualContact> outputContacts = component.getVisualOutputs();
        if (component.getIsZeroDelay() || (outputContacts.size() != 1)) {
            return null;
        }
        VisualContact contact = outputContacts.iterator().next();
        Pair<SignalStg, Boolean> signalStgAndInversion = converter.getSignalStgAndInvertion(contact);
        if (signalStgAndInversion == null) {
            return null;
        }
        Node traceCurrentNode = getTraceCurrentNode();
        SignalStg signalStg = signalStgAndInversion.getFirst();
        final boolean isExcited = !getContactExcitedTransitions(contact).isEmpty();
        final boolean isInTrace = signalStg.contains(traceCurrentNode);
        return new Decoration() {
            @Override
            public Color getColorisation() {
                if (isExcited) {
                    if (isInTrace) {
                        return CommonSimulationSettings.getEnabledBackgroundColor();
                    } else {
                        return CommonSimulationSettings.getEnabledForegroundColor();
                    }
                }
                return null;
            }
            @Override
            public Color getBackground() {
                if (isExcited) {
                    if (isInTrace) {
                        return CommonSimulationSettings.getEnabledForegroundColor();
                    } else {
                        return CommonSimulationSettings.getEnabledBackgroundColor();
                    }
                }
                return null;
            }
        };
    }

    public Decoration getContactDecoration(VisualContact contact) {
        Pair<SignalStg, Boolean> signalStgAndInversion = converter.getSignalStgAndInvertion(contact);
        if (signalStgAndInversion == null) {
            return null;
        }
        boolean isZeroDelay = false;
        Node parent = contact.getParent();
        if (parent instanceof VisualFunctionComponent) {
            isZeroDelay = ((VisualFunctionComponent) parent).getIsZeroDelay();
        }
        Node traceCurrentNode = getTraceCurrentNode();
        SignalStg signalStg = signalStgAndInversion.getFirst();
        boolean isInverting = signalStgAndInversion.getSecond();
        final boolean isOne = (signalStg.one.getReferencedPlace().getTokens() == 1) != isInverting;
        final boolean isZero = (signalStg.zero.getReferencedPlace().getTokens() == 1) != isInverting;
        final boolean isExcited = !getContactExcitedTransitions(contact).isEmpty() && !isZeroDelay;
        final boolean isInTrace = signalStg.contains(traceCurrentNode) && !isZeroDelay;
        return new StateDecoration() {
            @Override
            public Color getColorisation() {
                if (isExcited) {
                    if (isInTrace) {
                        return CommonSimulationSettings.getEnabledBackgroundColor();
                    } else {
                        return CommonSimulationSettings.getEnabledForegroundColor();
                    }
                }
                return null;
            }
            @Override
            public Color getBackground() {
                if (isExcited) {
                    if (isInTrace) {
                        return CommonSimulationSettings.getEnabledForegroundColor();
                    } else {
                        return CommonSimulationSettings.getEnabledBackgroundColor();
                    }
                } else {
                    if (isOne && !isZero) {
                        return CircuitSettings.getActiveWireColor();
                    }
                    if (!isOne && isZero) {
                        return CircuitSettings.getInactiveWireColor();
                    }
                }
                return null;
            }
            @Override
            public boolean showForcedInit() {
                return false;
            }
        };
    }

    public Decoration getConnectionOrJointDecoration(VisualNode node) {
        Pair<SignalStg, Boolean> signalStgAndInversion = converter.getSignalStgAndInvertion(node);
        if (signalStgAndInversion == null) {
            return null;
        }
        SignalStg signalStg = signalStgAndInversion.getFirst();
        boolean isInverting = signalStgAndInversion.getSecond();
        final boolean isOne = (signalStg.one.getReferencedPlace().getTokens() == 1) != isInverting;
        final boolean isZero = (signalStg.zero.getReferencedPlace().getTokens() == 1) != isInverting;
        return new StateDecoration() {
            @Override
            public Color getColorisation() {
                if (isOne && !isZero) {
                    return CircuitSettings.getActiveWireColor();
                }
                if (!isOne && isZero) {
                    return CircuitSettings.getInactiveWireColor();
                }
                return null;
            }
            @Override
            public Color getBackground() {
                return null;
            }
            @Override
            public boolean showForcedInit() {
                return false;
            }
        };
    }

    @Override
    public Decoration getContainerDecoration(Container container) {
        final boolean ret = isContainerExcited(container);
        return new ContainerDecoration() {
            @Override
            public Color getColorisation() {
                return null;
            }
            @Override
            public Color getBackground() {
                return null;
            }
            @Override
            public boolean isContainerExcited() {
                return ret;
            }
        };
    }

}
