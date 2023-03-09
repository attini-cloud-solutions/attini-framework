package deployment.plan.transform;


import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public record AttiniStep(String name,
                         String type){

}
