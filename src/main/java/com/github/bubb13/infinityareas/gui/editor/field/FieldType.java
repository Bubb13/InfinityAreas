
package com.github.bubb13.infinityareas.gui.editor.field;

import com.github.bubb13.infinityareas.gui.editor.connector.Connector;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.AbstractFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ByteBitFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ByteFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.FieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.IntBitFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.IntFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedByteBitFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedByteEnum;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntBitFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntEnum;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedIntImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortEnum;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.MappedShortImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.NumericFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ResrefFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ResrefFieldOptions;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.ShortFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.TextFieldImplementation;
import com.github.bubb13.infinityareas.gui.editor.field.implementation.TextFieldOptions;

public enum FieldType
{
    //------//
    // Byte //
    //------//

    SIGNED_BYTE
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(Byte.MIN_VALUE);
            else if (minValue < Byte.MIN_VALUE) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(Byte.MAX_VALUE);
            else if (maxValue > Byte.MAX_VALUE) throw new IllegalArgumentException();

            return new ByteFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
        }
    },

    UNSIGNED_BYTE
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(0);
            else if (minValue < 0) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(255);
            else if (maxValue > 255) throw new IllegalArgumentException();

            return new ByteFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
        }
    },

    BYTE_BITFIELD
    {
        public <FieldEnumType extends Enum<?>> FieldImplementation<FieldEnumType> createImplementation(
            final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof MappedByteBitFieldOptions<? extends MappedByteEnum> bitFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new ByteBitFieldImplementation<>(fieldEnum, connector, bitFieldOptions);
        }
    },

    //-------//
    // Short //
    //-------//

    SIGNED_SHORT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(Short.MIN_VALUE);
            else if (minValue < Short.MIN_VALUE) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(Short.MAX_VALUE);
            else if (maxValue > Short.MAX_VALUE) throw new IllegalArgumentException();

            return new ShortFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
        }
    },

    UNSIGNED_SHORT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(0);
            else if (minValue < 0) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(65535);
            else if (maxValue > 65535) throw new IllegalArgumentException();

            return new ShortFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
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

    //-----//
    // Int //
    //-----//

    SIGNED_INT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(Integer.MIN_VALUE);
            else if (minValue < Integer.MIN_VALUE) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(Integer.MAX_VALUE);
            else if (maxValue > Integer.MAX_VALUE) throw new IllegalArgumentException();

            return new IntFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
        }
    },

    UNSIGNED_INT
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof NumericFieldOptions numericFieldOptions))
            {
                throw new IllegalArgumentException();
            }

            Long minValue = numericFieldOptions.getMinValue();
            Long maxValue = numericFieldOptions.getMaxValue();

            if (minValue == null) numericFieldOptions.minValue(0L);
            else if (minValue < 0) throw new IllegalArgumentException();

            if (maxValue == null) numericFieldOptions.maxValue(4294967295L);
            else if (maxValue > 4294967295L) throw new IllegalArgumentException();

            return new IntFieldImplementation<>(fieldEnum, connector, numericFieldOptions);
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

    INT_BITFIELD
    {
        public <FieldEnumType extends Enum<?>> FieldImplementation<FieldEnumType> createImplementation(
            final FieldEnumType fieldEnum, final Connector<FieldEnumType> connector, final AbstractFieldOptions<?> options)
        {
            if (!(options instanceof MappedIntBitFieldOptions<? extends MappedIntEnum> bitFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new IntBitFieldImplementation<>(fieldEnum, connector, bitFieldOptions);
        }
    },

    //--------//
    // String //
    //--------//

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
            if (!(options instanceof ResrefFieldOptions resrefFieldOptions))
            {
                throw new IllegalArgumentException();
            }
            return new ResrefFieldImplementation<>(fieldEnum, connector, resrefFieldOptions);
        }
    },

    //-----------------//
    // Infinity Engine //
    //-----------------//

    STRREF
    {
        public <EnumType extends Enum<?>> FieldImplementation<EnumType> createImplementation(
            final EnumType fieldEnum, final Connector<EnumType> connector, final AbstractFieldOptions<?> options)
        {
            return null;
        }
    },

    //-------//
    // Other //
    //-------//

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
