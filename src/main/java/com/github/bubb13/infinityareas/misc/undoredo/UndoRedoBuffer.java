
package com.github.bubb13.infinityareas.misc.undoredo;

import com.github.bubb13.infinityareas.misc.referencetracking.AbstractReferenceTrackable;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHandle;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceHolder;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTrackable;
import com.github.bubb13.infinityareas.misc.referencetracking.ReferenceTracker;
import com.github.bubb13.infinityareas.misc.referencetracking.TrackingLinkedList;
import com.github.bubb13.infinityareas.util.MiscUtil;

import java.util.stream.Collectors;

public class UndoRedoBuffer
{
    ///////////////////////////
    // Private Static Fields //
    ///////////////////////////

    private static final boolean DEBUG = false;

    ////////////////////
    // Private Fields //
    ////////////////////

    private final TrackingLinkedList<TransactionInternal> undoStack =
        new TrackingLinkedList<>("UndoRedoBuffer::undoStack");

    private final TrackingLinkedList<TransactionInternal> redoStack
        = new TrackingLinkedList<>("UndoRedoBuffer::redoStack");

    private TransactionInternal currentTransaction;
    private int transactionNestCount = 0;
    private int suppressStackManipulationMode = 0;

    ////////////////////
    // Public Methods //
    ////////////////////

    //-----------//
    // Undo-Redo //
    //-----------//

    public void undo()
    {
        if (undoStack.isEmpty()) return;

        debugPrintInternal("  Undoing transaction: " + undoStack.peek());
        final TransactionInternal transaction = collectTransactionInternal(() -> undoStack.pop().undoAll());
        debugPrintInternal("  Undid transaction");

        if (transaction != null)
        {
            redoStack.push(transaction);
            debugPrintInternal("  Pushed transaction to redo stack");
        }
    }

    public void redo()
    {
        if (redoStack.isEmpty()) return;

        debugPrintInternal("  Redoing transaction: " + redoStack.peek());
        final TransactionInternal transaction = collectTransactionInternal(() -> redoStack.pop().undoAll());
        debugPrintInternal("  Redid transaction");

        if (transaction != null && pushToUndoStackInternal(transaction, false))
        {
            debugPrintInternal("  Pushed transaction to undo stack");
        }
    }

    //------------------------//
    // Transaction Management //
    //------------------------//

    public void runAsTransaction(final Runnable runnable)
    {
        startTransactionInternal();
        try
        {
            runnable.run();
        }
        finally
        {
            final TransactionInternal transaction = endTransactionInternal();

            if (transaction != null && pushToUndoStackInternal(transaction, true))
            {
                debugPrintInternal("  Pushed transaction to undo stack");
            }
        }
    }

    public void performAsTransaction(final Runnable runnable)
    {
        clearRedo();
        runAsTransaction(runnable);
    }

    public void runWithUndoSuppressed(final Runnable runnable)
    {
        final int savedValue = suppressStackManipulationMode;
        suppressStackManipulationMode = 2;
        try
        {
            runnable.run();
        }
        finally
        {
            suppressStackManipulationMode = savedValue;
        }
    }

    //-----------------------------------//
    // Manual Undo-Redo Stack Management //
    //-----------------------------------//

    public IUndoHandle pushUndo(final String name, final Runnable runnable)
    {
        final AbstractUndo undo = new NamedUndo(name)
        {
            @Override
            public void undo()
            {
                runnable.run();
            }
        };

        pushUndoInternal(undo);
        return undo::delete;
    }

    public void clearRedo()
    {
        if (suppressStackManipulationMode == 0)
        {
            redoStack.clear();
            debugPrintInternal("  Cleared redo stack");
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void pushUndoInternal(final AbstractUndo undo)
    {
        if (transactionNestCount == 0)
        {
            if (pushToUndoStackInternal(new TransactionInternal(undo), true))
            {
                debugPrintInternal("  Pushed lone undo to undo stack: " + undo);
            }
        }
        else if (suppressStackManipulationMode < 2)
        {
            currentTransaction.pushUndo(undo);
            debugPrintInternal("  Added undo to the current transaction: " + undo);
        }
    }

    private void startTransactionInternal()
    {
        if (transactionNestCount++ == 0)
        {
            currentTransaction = new TransactionInternal();
            debugPrintInternal("Started new transaction");
        }
        else
        {
            debugPrintInternal("  Started nested transaction");
        }
    }

    private TransactionInternal endTransactionInternal()
    {
        if (transactionNestCount <= 0)
        {
            throw new IllegalStateException();
        }

        if (--transactionNestCount == 0)
        {
            final TransactionInternal savedTransaction = currentTransaction;
            currentTransaction = null;
            return !savedTransaction.isEmpty() ? savedTransaction : null;
        }

        return null;
    }

    private boolean pushToUndoStackInternal(final TransactionInternal transaction, final boolean clearRedoStack)
    {
        if (suppressStackManipulationMode > 0) return false;
        undoStack.push(transaction);
        if (clearRedoStack) redoStack.clear();
        return true;
    }

    private TransactionInternal collectTransactionInternal(final Runnable runnable)
    {
        TransactionInternal toReturn;

        final int savedValue = suppressStackManipulationMode;
        suppressStackManipulationMode = 1;

        startTransactionInternal();
        try
        {
            runnable.run();
        }
        finally
        {
            toReturn = endTransactionInternal();
            suppressStackManipulationMode = savedValue;
        }

        return toReturn;
    }

    private void debugPrintInternal(final String toPrint)
    {
        if (DEBUG && suppressStackManipulationMode == 0)
        {
            System.out.println(toPrint);
        }
    }

    ////////////////////////////
    // Private Static Classes //
    ////////////////////////////

    private static abstract class AbstractUndo implements IUndo, ReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ReferenceTracker referenceTracker = new ReferenceTracker();

        ////////////////////
        // Public Methods //
        ////////////////////

        //------------------------------//
        // ReferenceTrackable Overrides //
        //------------------------------//

        @Override
        public void addedTo(final ReferenceHolder<?> referenceHolder, ReferenceHandle referenceHandle)
        {
            referenceTracker.addedTo(referenceHolder, referenceHandle);
        }

        @Override
        public void removedFrom(final ReferenceHolder<?> referenceHolder)
        {
            referenceTracker.removedFrom(referenceHolder);
        }

        @Override
        public void softDelete()
        {
            referenceTracker.softDelete();
        }

        @Override
        public void restore()
        {
            referenceTracker.restore();
        }

        @Override
        public void delete()
        {
            referenceTracker.delete();
        }
    }

    private static abstract class NamedUndo extends AbstractUndo
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final String name;

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public NamedUndo(final String name)
        {
            this.name = name;
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        //------------------//
        // Object Overrides //
        //------------------//

        @Override
        public String toString()
        {
            return name;
        }
    }

    private static class TransactionInternal extends AbstractReferenceTrackable
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final TrackingLinkedList<AbstractUndo> undoStack =
            new TrackingLinkedList<>("UndoRedoBuffer::TransactionInternal::undoStack")
        {
            @Override
            public void referencedObjectDeleted(final ReferenceHandle referenceHandle)
            {
                super.referencedObjectDeleted(referenceHandle);
                if (isEmpty()) delete();
            }
        };

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TransactionInternal() {}

        public TransactionInternal(final AbstractUndo undo)
        {
            pushUndo(undo);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public void pushUndo(final AbstractUndo undo)
        {
            undoStack.push(undo);
        }

        public void undoAll()
        {
            undoStack.reversed().forEach(IUndo::undo);
        }

        public boolean isEmpty()
        {
            return undoStack.isEmpty();
        }

        //------------------//
        // Object Overrides //
        //------------------//

        @Override
        public String toString()
        {
            return "[" +
                MiscUtil.iteratorStream(undoStack.reverseIterator())
                .map(Object::toString)
                .collect(Collectors.joining(", "))
            + "]";
        }
    }
}
