package org.talend.sdk.component.studio.ui.guessschema;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.talend.core.model.process.IContext;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.properties.Property;
import org.talend.designer.core.ui.editor.nodes.Node;
import org.talend.designer.core.ui.editor.properties.controllers.AbstractGuessSchemaProcess;
import org.talend.sdk.component.studio.ui.guessschema.TaCoKitGuessSchemaProcess.GuessSchemaResult;

public class GuessSchemaRunnable implements IRunnableWithProgress {

    private final IContext context;

    private IElementParameter elementParameter;

    private GuessSchemaResult schema = null;

    private boolean canceled = false;

    public GuessSchemaRunnable(final IContext context,
            final IElementParameter elementParameter) {
        this.context = context;
        this.elementParameter = elementParameter;
    }

    @Override
    public void run(final IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
        try {
            Node node = Node.class.cast(elementParameter.getElement());
            Property newmockProperty = AbstractGuessSchemaProcess.getNewmockProperty();
            TaCoKitGuessSchemaProcess gsp = new TaCoKitGuessSchemaProcess(newmockProperty, node, context,
                    discoverSchemaActionName(), elementParameter.getContext());

            final Future<GuessSchemaResult> result = gsp.run();
            while (!result.isDone()) {
                if (monitor.isCanceled()) {
                    result.cancel(true);
                    canceled = true;
                    gsp.kill();
                    return;
                }
            }
            try {
                schema = result.get();
            } catch (ExecutionException e) {
                throw new InvocationTargetException(e);
            }
        } finally {
            monitor.done();
        }
    }

    private String discoverSchemaActionName() {
        return elementParameter.getValue() == null ? null
                : String.valueOf(elementParameter.getValue());
    }

    public synchronized GuessSchemaResult getSchema() {
        return schema;
    }

    public synchronized boolean isCanceled() {
        return canceled;
    }
}
