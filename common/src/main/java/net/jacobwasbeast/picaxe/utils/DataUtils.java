package net.jacobwasbeast.picaxe.utils;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.Registry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.storage.ValueInput;

import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A utility class for data conversions, including converting NBT tags.
 */
public class DataUtils {

    /**
     * Creates a ValueInput wrapper around a given CompoundTag.
     * This allows treating an in-memory CompoundTag as a source for data reading,
     * similar to how data is read from world storage.
     *
     * @param tag The CompoundTag to wrap.
     * @return A ValueInput instance that reads data from the provided tag.
     */
    public static ValueInput getValueInputFromCompoundTag(CompoundTag tag) {
        return new CompoundTagValueInput(tag);
    }

    /**
     * A private implementation of ValueInput that reads data from a CompoundTag.
     */
    private static final class CompoundTagValueInput implements ValueInput {
        private final CompoundTag tag;
        private static final ValueInputList EMPTY_COMPOUND_LIST = new CompoundTagListValueInput(new ListTag());

        public CompoundTagValueInput(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public <T> Optional<T> read(String key, Codec<T> codec) {
            // Delegates reading a specific key with a codec to the underlying CompoundTag.
            return tag.read(key, codec, NbtOps.INSTANCE);
        }

        @Override
        @Deprecated
        public <T> Optional<T> read(MapCodec<T> codec) {
            // Delegates reading from the root of the tag with a MapCodec.
            return tag.read(codec, NbtOps.INSTANCE);
        }

        @Override
        public Optional<ValueInput> child(String key) {
            // Gets a nested CompoundTag and wraps it in a ValueInput.
            return tag.getCompound(key).map(CompoundTagValueInput::new);
        }

        @Override
        public ValueInput childOrEmpty(String key) {
            // Gets a nested CompoundTag or an empty one, and wraps it.
            return new CompoundTagValueInput(tag.getCompoundOrEmpty(key));
        }

        @Override
        public Optional<ValueInputList> childrenList(String key) {
            // Retrieves a list of compound tags, ensuring type safety.
            return Optional.ofNullable(this.tag.get(key))
                    .filter(ListTag.class::isInstance)
                    .map(ListTag.class::cast)
                    // Ensure the list is either empty or contains CompoundTags by checking the first element.
                    // This is safe because a ListTag can only hold one type of Tag.
                    .filter(list -> list.isEmpty() || list.get(0) instanceof CompoundTag)
                    .map(CompoundTagListValueInput::new);
        }

        @Override
        public ValueInputList childrenListOrEmpty(String key) {
            // Retrieves a list of compound tags or an empty list wrapper if not present or wrong type.
            return childrenList(key).orElse(EMPTY_COMPOUND_LIST);
        }

        @Override
        public <T> Optional<TypedInputList<T>> list(String key, Codec<T> codec) {
            // Retrieves a list of any type and prepares it for decoding with the given codec.
            return tag.getList(key).map(listTag -> new TypedListValueInput<>(listTag, codec));
        }

        @Override
        public <T> TypedInputList<T> listOrEmpty(String key, Codec<T> codec) {
            // Retrieves a list or an empty one for decoding.
            return new TypedListValueInput<>(tag.getListOrEmpty(key), codec);
        }

        // --- Primitive Getters ---
        // These methods directly delegate to the corresponding getter in CompoundTag.

        @Override
        public boolean getBooleanOr(String key, boolean defaultValue) {
            return tag.getBooleanOr(key, defaultValue);
        }

        @Override
        public byte getByteOr(String key, byte defaultValue) {
            return tag.getByteOr(key, defaultValue);
        }

        @Override
        public int getShortOr(String key, short defaultValue) {
            return tag.getShortOr(key, defaultValue);
        }

        @Override
        public Optional<Integer> getInt(String key) {
            return tag.getInt(key);
        }

        @Override
        public int getIntOr(String key, int defaultValue) {
            return tag.getIntOr(key, defaultValue);
        }

        @Override
        public long getLongOr(String key, long defaultValue) {
            return tag.getLongOr(key, defaultValue);
        }

        @Override
        public Optional<Long> getLong(String key) {
            return tag.getLong(key);
        }

        @Override
        public float getFloatOr(String key, float defaultValue) {
            return tag.getFloatOr(key, defaultValue);
        }

        @Override
        public double getDoubleOr(String key, double defaultValue) {
            return tag.getDoubleOr(key, defaultValue);
        }

        @Override
        public Optional<String> getString(String key) {
            return tag.getString(key);
        }

        @Override
        public String getStringOr(String key, String defaultValue) {
            return tag.getStringOr(key, defaultValue);
        }

        @Override
        public Optional<int[]> getIntArray(String key) {
            return tag.getIntArray(key);
        }

        @Override
        @Deprecated
        public HolderLookup.Provider lookup() {
            // Provides a default, empty lookup provider as the context is not available here.
            return new HolderLookup.Provider() {
                @Override
                public Stream<ResourceKey<? extends Registry<?>>> listRegistryKeys() {
                    return Stream.empty();
                }

                @Override
                public <T> Optional<? extends HolderLookup.RegistryLookup<T>> lookup(ResourceKey<? extends Registry<? extends T>> resourceKey) {
                    return Optional.empty();
                }
            };
        }
    }

    /**
     * A ValueInputList implementation for a ListTag containing CompoundTags.
     */
    private static final class CompoundTagListValueInput implements ValueInput.ValueInputList {
        private final ListTag listTag;

        public CompoundTagListValueInput(ListTag listTag) {
            this.listTag = listTag;
        }

        @Override
        public boolean isEmpty() {
            return listTag.isEmpty();
        }

        @Override
        public Stream<ValueInput> stream() {
            // Streams the list elements, casting each to CompoundTag and wrapping it.
            return listTag.stream()
                    .map(tag -> (CompoundTag) tag)
                    .map(CompoundTagValueInput::new);
        }

        @Override
        public Iterator<ValueInput> iterator() {
            return stream().iterator();
        }
    }

    /**
     * A TypedInputList implementation that decodes elements of a ListTag using a Codec.
     * @param <T> The type of the decoded elements.
     */
    private static final class TypedListValueInput<T> implements ValueInput.TypedInputList<T> {
        private final ListTag listTag;
        private final Codec<T> codec;

        public TypedListValueInput(ListTag listTag, Codec<T> codec) {
            this.listTag = listTag;
            this.codec = codec;
        }

        @Override
        public boolean isEmpty() {
            return listTag.isEmpty();
        }

        @Override
        public Stream<T> stream() {
            // Streams the list elements, parsing each one with the provided codec.
            // Elements that fail to parse are skipped.
            return listTag.stream()
                    .map(tag -> codec.parse(NbtOps.INSTANCE, tag))
                    .flatMap(dataResult -> dataResult.result().stream());
        }

        @Override
        public Iterator<T> iterator() {
            return stream().iterator();
        }
    }
}
