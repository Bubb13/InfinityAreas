
package com.github.bubb13.infinityareas.gui.editor.gui;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.AbstractFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.FieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedIntEnum;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedIntFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedIntImplementation;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedShortEnum;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedShortFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.MappedShortImplementation;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.TextFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.gui.fieldimplementation.textfield.TextFieldOptions;

public enum FieldType
{
    SIGNED_BYTE
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    UNSIGNED_BYTE
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    SIGNED_SHORT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    UNSIGNED_SHORT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    SIGNED_INT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    UNSIGNED_INT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    TEXT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof TextFieldOptions textFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new TextFieldImplementation<>(fieldEnum, connector, textFieldOptions);
        }
    },

    RESREF
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    STRREF
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    MAPPED_SHORT
    {
        public <FieldEnumType extends Enum<?>> FieldImplementation<FieldEnumType> createImplementation(
            final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof MappedShortFieldOptions<? extends MappedShortEnum> mappedShortFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new MappedShortImplementation<>(fieldEnum, connector, mappedShortFieldOptions);
        }
    },

    MAPPED_INT
    {
        public <FieldEnumType extends Enum<?>> FieldImplementation<FieldEnumType> createImplementation(
            final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof MappedIntFieldOptions<? extends MappedIntEnum> mappedIntFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new MappedIntImplementation<>(fieldEnum, connector, mappedIntFieldOptions);
        }
    },

    UNMAPPED
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    };

    public abstract <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
        final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options);
}
