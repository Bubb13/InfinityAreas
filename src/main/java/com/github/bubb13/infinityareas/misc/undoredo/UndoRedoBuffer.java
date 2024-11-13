
package com.github.bubb13.infinityareas.misc.undoredo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Stack;
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

    private final Stack<TransactionInternal> undoStack = new Stack<>();
    private final Stack<TransactionInternal> redoStack = new Stack<>();
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

        if (transaction != null && pushToUndoStackInternal(transaction))
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

            if (transaction != null && pushToUndoStackInternal(transaction))
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

    public void pushUndo(final IUndo undo)
    {
        if (transactionNestCount == 0)
        {
            pushToUndoStackInternal(new TransactionInternal(undo));
            debugPrintInternal("  Pushed lone undo to undo stack: " + undo);
        }
        else if (suppressStackManipulationMode < 2)
        {
            currentTransaction.pushUndo(undo);
            debugPrintInternal("  Added undo to the current transaction: " + undo);
        }
    }

    public void pushUndo(final String name, final Runnable undo)
    {
        pushUndo(new NamedUndo(name)
        {
            @Override
            public void undo()
            {
                undo.run();
            }
        });
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

    private boolean pushToUndoStackInternal(final TransactionInternal transaction)
    {
        if (suppressStackManipulationMode > 0) return false;
        undoStack.push(transaction);
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

    private static abstract class NamedUndo implements IUndo
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

    private static class TransactionInternal
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final Deque<IUndo> undoStack = new ArrayDeque<>();

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TransactionInternal() {}

        public TransactionInternal(final IUndo undo)
        {
            pushUndo(undo);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public void pushUndo(final IUndo undo)
        {
            undoStack.push(undo);
        }

        public void undoAll()
        {
            undoStack.forEach(IUndo::undo);
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
                undoStack.stream()
                .map(Object::toString)
                .collect(Collectors.joining(", "))
            + "]";
        }
    }
}
