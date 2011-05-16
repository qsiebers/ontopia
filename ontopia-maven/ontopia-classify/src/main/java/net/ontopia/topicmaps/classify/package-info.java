/**
<p>To classify content, use the <tt>SimpleClassifier</tt> class.  Note
that most of the APIs are INTERNAL, and so may change at any time.</p>

<p>If you need more flexibility, it is possible to use the INTERNAL
APIs directly. Below is example code showing how to output a ranked
list of the terms found in a particular document.</p>

<pre>
    // load the topic map
    TopicMapIF topicmap = ImportExportUtils.getReader(args[0]).read();

    // create classifier
    TopicMapClassification tcl = new TopicMapClassification(topicmap);

    // read document
    ClassifiableContentIF cc = ClassifyUtils.getClassifiableContent(args[1]);

    // classify document
    tcl.classify(cc);

    // dump the ranked terms
    TermDatabase tdb = tcl.getTermDatabase();
    tdb.dump(50);
</pre>
*/

package net.ontopia.topicmaps.classify;
