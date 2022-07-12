package be.ugent.rml.termgenerator;

import be.ugent.rml.extractor.ReferenceExtractor;
import org.w3c.dom.Node;

public interface ContextAwareRecord {
    void setContext(ReferenceExtractor referenceExtractor, Node getContextNode);

    ReferenceExtractor getContext();

    Node getContextNode();
}
