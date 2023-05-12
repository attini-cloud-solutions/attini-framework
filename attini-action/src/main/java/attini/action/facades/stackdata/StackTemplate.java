package attini.action.facades.stackdata;

import static java.util.Objects.requireNonNull;

import attini.domain.ObjectIdentifier;

public record StackTemplate(ObjectIdentifier objectIdentifier, String template) {

    public StackTemplate(ObjectIdentifier objectIdentifier, String template) {
        this.objectIdentifier = requireNonNull(objectIdentifier, "objectIdentifier");
        this.template = requireNonNull(template, "template");
    }
}
