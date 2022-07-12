package be.ugent.rml.termgenerator;

import be.ugent.rml.Utils;
import be.ugent.rml.extractor.ReferenceExtractor;
import be.ugent.rml.functions.FunctionUtils;
import be.ugent.rml.functions.SingleRecordFunctionExecutor;
import be.ugent.rml.records.Record;
import be.ugent.rml.term.Literal;
import be.ugent.rml.term.NamedNode;
import be.ugent.rml.term.Term;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.List;

import static be.ugent.rml.Utils.isValidrrLanguage;

public class CustomizedLiteralGenerator extends LiteralGenerator {

    public static LiteralGenerator create(final SingleRecordFunctionExecutor functionExecutor,
                                          final SingleRecordFunctionExecutor languageExecutor) {

        return new CustomizedLiteralGenerator(functionExecutor, languageExecutor);
    }

    // The executor used to get the language for the literal.
    private SingleRecordFunctionExecutor languageExecutor;
    // The URL of the datatype used for the literal.
    private Term datatype;
    private int maxNumberOfTerms;

    private CustomizedLiteralGenerator(
            final SingleRecordFunctionExecutor functionExecutor,
            final SingleRecordFunctionExecutor languageExecutor,
            final Term datatype,
            final int maxNumberOfTerms
    ) {
        super(functionExecutor);
        this.languageExecutor = languageExecutor;
        this.datatype = datatype;
        this.maxNumberOfTerms = maxNumberOfTerms;
    }

    public CustomizedLiteralGenerator(
            final SingleRecordFunctionExecutor functionExecutor,
            final SingleRecordFunctionExecutor languageExecutor
    ) {
        this(functionExecutor, languageExecutor, null, 0);
    }

    @Override
    public List<Term> generate(final Record record) throws Exception {
        final boolean isRelatedToLangBug = languageExecutor instanceof ReferenceExtractor &&
                record instanceof NodeViewAwareRecord &&
                record instanceof ContextAwareRecord &&
                functionExecutor instanceof ReferenceExtractor;

        final ArrayList<Term> terms = new ArrayList<>();
        final ArrayList nodes;

        if(isRelatedToLangBug) {
            final NodeViewAwareRecord nodeView = (NodeViewAwareRecord) record;
            nodeView.setAsNode();
            nodes = new ArrayList<>((List<Node>) this.functionExecutor.execute(record));
            nodeView.clearView();
        }
        else {
            nodes = new ArrayList<>();
            FunctionUtils.functionObjectToList(this.functionExecutor.execute(record), nodes);
        }

        String dataTypeSource = null;
        if (this.functionExecutor instanceof ReferenceExtractor) {
            dataTypeSource = record.getDataType(((ReferenceExtractor) this.functionExecutor).reference);
        }

        if (nodes.size() > 0) {
            //add language tag if present
            String finalDataTypeSource = dataTypeSource;
            nodes.forEach(node -> {
                if (languageExecutor != null) {
                    try {
                        if (isRelatedToLangBug) {
                            ((ContextAwareRecord) record).setContext((ReferenceExtractor)functionExecutor, (Node)node);
                        }

                        final List<String> languages = new ArrayList<>();
                        FunctionUtils.functionObjectToList(this.languageExecutor.execute(record), languages);

                        if (!languages.isEmpty()) {
                            String language = languages.get(0);

                            if (! isValidrrLanguage(language)) {
                                throw new RuntimeException(String.format("Language tag \"%s\" does not conform to BCP 47 standards", language));
                            }

                            terms.add(new Literal(nodeAsText(node, isRelatedToLangBug), language));
                        }
                    }
                    catch (Exception e) {
                        // TODO print error message
                        e.printStackTrace();
                    }
                }
                else if (datatype != null) {
                    //add datatype if present; language and datatype can't be combined because the language tag implies langString as datatype
                    terms.add(new Literal(nodeAsText(node, isRelatedToLangBug), datatype));
                }
                else if (finalDataTypeSource != null) {
                    if (this.functionExecutor instanceof ReferenceExtractor) {
                        node = Utils.transformDatatypeString(nodeAsText(node, isRelatedToLangBug), finalDataTypeSource);
                    }
                    terms.add(new Literal(nodeAsText(node, isRelatedToLangBug), new NamedNode(finalDataTypeSource)));
                }
                else {
                    terms.add(new Literal(nodeAsText(node, isRelatedToLangBug)));
                }
            });
        }

        if (maxNumberOfTerms != 0) {
            return terms.subList(0, maxNumberOfTerms);
        }
        else {
            return terms;
        }
    }

    private String nodeAsText(final Object node, final boolean isRelatedToLangBug) {
        if(isRelatedToLangBug) {
            return ((Node)node).getTextContent();
        }
        else {
            return (String)node;
        }
    }
}
