package io.github.bennofs.wdumper.diffing;

import com.google.common.collect.Sets;

import java.util.*;

public class DiffWikidataRDF {
    private Diff diff;
    private final String entityId;
    private final ParsedDocument pDump;
    private final ParsedDocument pSer;
    private final MatchMemorizer memo;

    public DiffWikidataRDF(String entityId, ParsedDocument pDump, ParsedDocument pSer, MatchMemorizer memo) {
        this.entityId = entityId;
        this.pDump = pDump;
        this.pSer = pSer;
        this.memo = memo;
    }

    public void reportDifference(String tag, Set<Triple> inDump, Set<Triple> inSer) {
        System.out.println("DIFF " + tag);
        if (this.diff == null) {
            this.diff = new Diff(entityId, pDump, pSer);
        }
        for (Triple t : inDump) {
            System.out.println(t.toString());
        }
        System.out.println("---");
        for (Triple t : inSer) {
            System.out.println(t.toString());
        }
        this.diff.differences.add(new Diff.Difference(tag, inDump, inSer));
    }

    public String diffName(ParsedDocument.BlankNode dump, ParsedDocument.BlankNode ser) {
        if (dump.subject.equals(ser.subject)) {
            return Utils.bufferString(ser.subject);
        }

        return Utils.bufferString(dump.subject) + "-" + Utils.bufferString(ser.subject);
    }

    public void diffBlankNode(String prefix, ParsedDocument.BlankNode dump, ParsedDocument.BlankNode ser) {
        dump.removeMatchingOther(ser);
        if (dump.other.size() + ser.other.size() != 0) {
            dump.removeMatchingOther(ser);
            this.reportDifference(prefix + diffName(dump, ser)+ "/" + "other", dump.other, ser.other);
        }
    }

    public void diffNode(String prefix, ParsedDocument.Node dump, ParsedDocument.Node ser) {
        diffBlankNode(prefix, dump, ser);

        // handle bnodes
        dump.consumeMatchingBNodes(ser, (bDump, bSer) -> {
            if (!matchChecked(prefix + diffName(dump, ser) + "/", bDump, bSer)) {
                diffBlankNode(prefix + diffName(dump, ser) + "/", bDump, bSer);
            }
        });

        dump.getBNodes().forEach(bnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/bnode/dump", Sets.union(bnode.links, bnode.other), Collections.emptySet());
        });
        ser.getBNodes().forEach(bnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/bnode/serialized", Collections.emptySet(), Sets.union(bnode.links, bnode.other));
        });
    }

    public void diffNodeWithValues(String prefix, ParsedDocument.NodeWithValues dump, ParsedDocument.NodeWithValues ser) {
        diffNode(prefix, dump, ser);

        // handle values
        dump.consumeMatchingValues(ser, (vDump, vSer) -> {
            if (!matchChecked(prefix + diffName(dump, ser) + "/", vDump, vSer)) {
                diffNode(prefix + diffName(dump, ser) + "/", vDump, vSer);
            }
        });

        dump.getValues().forEach(vnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/value/dump", Sets.union(vnode.links, vnode.other), Collections.emptySet());
        });
        ser.getValues().forEach(vnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/value/serialized", Collections.emptySet(), Sets.union(vnode.links, vnode.other));
        });
    }

    public void diffNodeWithReferences(String prefix, ParsedDocument.NodeWithReferences dump, ParsedDocument.NodeWithReferences ser) {
        diffNodeWithValues(prefix, dump, ser);

        // handle references
        dump.consumeMatchingReferences(ser, (rDump, rSer) -> {
            if (!matchChecked(prefix + diffName(dump, ser) + "/", rDump, rSer)) {
                diffNode(prefix + diffName(dump, ser) + "/", rDump, rSer);
            }
        });

        dump.getReferences().forEach(rnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/reference/dump", Sets.union(rnode.links, rnode.other), Collections.emptySet());
        });
        ser.getReferences().forEach(rnode -> {
            this.reportDifference(prefix + diffName(dump, ser) + "/reference/serialized", Collections.emptySet(), Sets.union(rnode.links, rnode.other));
        });
    }

    public boolean matchChecked(String prefix, ParsedDocument.BlankNode dump, ParsedDocument.BlankNode ser) {
        if (ser.matchedNode == dump) return true;
        if (ser.matchedNode == null && dump.matchedNode == null) {
            memo.memorize(dump, ser);
            ser.matchedNode = dump;
            dump.matchedNode = ser;

            // skip matching if either node is empty (this can happen due to value/reference node deduplication)
            return dump.isEmpty() || ser.isEmpty();
        }

        // allow mismatches if the data for both nodes is equal
        if ((ser.matchedNode == null || ser.matchedNode.origOther.equals(dump.origOther)) &&
                (dump.matchedNode == null || dump.matchedNode.origOther.equals(ser.origOther))) {
            if (ser.matchedNode == null) ser.matchedNode = dump;
            if (dump.matchedNode == null) dump.matchedNode = ser;
            return true; // the existing node pair has already been diffed
        }

        // mismatch
        reportDifference(
                prefix + diffName(dump, ser) + "/mismatch",
                Sets.union(dump.other, ser.matchedNode == null ? Collections.emptySet() : ser.matchedNode.other),
                Sets.union(ser.other, dump.matchedNode == null ? Collections.emptySet() : dump.matchedNode.other)
        );
        return false;
    }

    public Diff compute() {
        memo.recall(this.pDump, this.pSer);

        this.pDump.consumeMatchingStatements(this.pSer, (sDump, sSer) -> {
            diffNodeWithReferences("statement/", sDump, sSer);
        });
        diffNode("", this.pDump.getRoot(), this.pSer.getRoot());

        return this.diff;
    }
}