/*
 * Copyright (c) 2023 Attini Cloud Solutions International AB.
 * All Rights Reserved
 */

package deployment.plan.custom.resource.service;

import static java.util.Objects.requireNonNull;

import java.util.Objects;

class SfnProperties {
    public final String sfnArn;
    public final String sfnName;
    public final String stackName;

    public SfnProperties(String sfnArn, String sfnName, String stackName) {
        this.sfnArn = requireNonNull(sfnArn, "sfnArn");
        this.sfnName = requireNonNull(sfnName, "sfnName");
        this.stackName = requireNonNull(stackName, "stackName");
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SfnProperties that = (SfnProperties) o;
        return Objects.equals(sfnArn, that.sfnArn) && Objects.equals(sfnName,
                                                                     that.sfnName) && Objects.equals(
                stackName,
                that.stackName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sfnArn, sfnName, stackName);
    }
}
