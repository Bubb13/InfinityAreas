
package com.github.bubb13.infinityareas.misc.undoredo;

import java.util.ArrayList;
import java.util.Stack;

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
    private boolean suppressStackManipulation;

    ////////////////////
    // Public Methods //
    ////////////////////

    //-----------//
    // Undo-Redo //
    //-----------//

    public void undo()
    {
        if (undoStack.isEmpty()) return;

        final TransactionInternal transaction = collectTransactionInternal(() -> undoStack.pop().undoAll());

        debugPrintInternal("  Undid transaction");

        if (transaction != null)
        {
            debugPrintInternal("  Pushed undo transaction to redo stack");
            redoStack.push(transaction);
        }
    }

    public void redo()
    {
        if (redoStack.isEmpty()) return;

        final TransactionInternal transaction = collectTransactionInternal(() -> redoStack.pop().undoAll());

        debugPrintInternal("  Redid transaction");

        if (transaction != null)
        {
            debugPrintInternal("  Pushed redo transaction to undo stack");
            pushToUndoStackInternal(transaction);
        }
    }

    //---------//
    // Perform //
    //---------//

    public void perform(final IUndoRedo undoRedo)
    {
        clearRedo();
        undoRedo.perform();
        addUndo(undoRedo);
    }

    public void perform(final Runnable perform, final Runnable undo)
    {
        perform(new AbstractUndoRedo()
        {
            @Override
            public void perform()
            {
                perform.run();
            }

            @Override
            public void undo()
            {
                undo.run();
            }
        });
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

            if (transaction != null)
            {
                debugPrintInternal("  Pushed transaction to undo stack");
                pushToUndoStackInternal(transaction);
            }
        }
    }

    public void performAsTransaction(final Runnable perform, final Runnable undo)
    {
        runAsTransaction(() -> perform(new AbstractUndoRedo()
        {
            @Override
            public void perform()
            {
                perform.run();
            }

            @Override
            public void undo()
            {
                undo.run();
            }
        }));
    }

    public void runWithUndoRedoSuppressed(final Runnable runnable)
    {
        final boolean savedValue = suppressStackManipulation;
        suppressStackManipulation = true;
        try
        {
            runnable.run();
        }
        finally
        {
            suppressStackManipulation = savedValue;
        }
    }

    //-----------------------------------//
    // Manual Undo-Redo Stack Management //
    //-----------------------------------//

    public void addUndo(final IUndoRedo undoRedo)
    {
        if (transactionNestCount == 0)
        {
            debugPrintInternal("  Pushed lone undoRedo to undo stack");
            pushToUndoStackInternal(new TransactionInternal(undoRedo));
        }
        else
        {
            debugPrintInternal("  Added undoRedo to the current transaction");
            currentTransaction.addUndoRedo(undoRedo);
        }
    }

    public void addUndo(final Runnable undo)
    {
        addUndo(new AbstractUndo()
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
        if (!suppressStackManipulation)
        {
            debugPrintInternal("  Cleared redo stack");
            redoStack.clear();
        }
    }

    /////////////////////
    // Private Methods //
    /////////////////////

    private void startTransactionInternal()
    {
        if (transactionNestCount++ == 0)
        {
            debugPrintInternal("Started new transaction");
            currentTransaction = new TransactionInternal();
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
            return savedTransaction;
        }

        return null;
    }

    private void pushToUndoStackInternal(final TransactionInternal transaction)
    {
        if (suppressStackManipulation) return;
        undoStack.push(transaction);
    }

    private TransactionInternal collectTransactionInternal(final Runnable runnable)
    {
        TransactionInternal toReturn;

        final boolean savedValue = suppressStackManipulation;
        suppressStackManipulation = true;

        startTransactionInternal();
        try
        {
            runnable.run();
        }
        finally
        {
            toReturn = endTransactionInternal();
            suppressStackManipulation = savedValue;
        }

        return toReturn;
    }

    private void debugPrintInternal(final String toPrint)
    {
        if (DEBUG && !suppressStackManipulation)
        {
            System.out.println(toPrint);
        }
    }

    ////////////////////////////
    // Private Static Classes //
    ////////////////////////////

    private static class TransactionInternal
    {
        ////////////////////
        // Private Fields //
        ////////////////////

        private final ArrayList<IUndoRedo> undoStack = new ArrayList<>();

        /////////////////////////
        // Public Constructors //
        /////////////////////////

        public TransactionInternal() {}

        public TransactionInternal(final IUndoRedo undoRedo)
        {
            addUndoRedo(undoRedo);
        }

        ////////////////////
        // Public Methods //
        ////////////////////

        public void addUndoRedo(final IUndoRedo undoRedo)
        {
            undoStack.add(undoRedo);
        }

        public void undoAll()
        {
            undoStack.reversed().forEach(IUndoRedo::undo);
        }
    }
}
