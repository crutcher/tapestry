package loom.alt.attrgraph;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import loom.common.HasToJsonString;

import java.util.Map;
import java.util.NoSuchElementException;

@Data
@Jacksonized
@Builder
public final class LoomSchema implements HasToJsonString {
    @Data
    @Jacksonized
    @Builder
    public final static class Type {
        String name;
    }

    @Data
    @Jacksonized
    @Builder
    public final static class Attribute {
        String name;

        @Builder.Default
        boolean invertEdge = false;
    }


    String urn;

    @Singular
    Map<String, Type> types;

    @Singular
    Map<String, Attribute> attributes;

    public Type getType(String name) {
        var typ = types.get(name);
        if (typ == null) {
            throw new NoSuchElementException(name);
        }
        return typ;
    }

    public Attribute getAttribute(String name) {
        var attr = attributes.get(name);
        if (attr == null) {
            throw new NoSuchElementException(name);
        }
        return attr;
    }
}
