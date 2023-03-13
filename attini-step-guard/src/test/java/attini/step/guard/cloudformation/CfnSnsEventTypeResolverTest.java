package attini.step.guard.cloudformation;

import static attini.step.guard.cloudformation.CfnEventType.RESOURCE_FAILED;
import static attini.step.guard.cloudformation.CfnEventType.RESOURCE_UPDATE;
import static attini.step.guard.cloudformation.CfnEventType.STACK_DELETED;
import static attini.step.guard.cloudformation.CfnEventType.STACK_FAILED;
import static attini.step.guard.cloudformation.CfnEventType.STACK_IN_PROGRESS;
import static attini.step.guard.cloudformation.CfnEventType.STACK_UPDATED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import attini.step.guard.StepGuardInputBuilder;

class CfnSnsEventTypeResolverTest {

    private final CfnSnsEventTypeResolver cfnSnsEventTypeResolver = new CfnSnsEventTypeResolver();

    @Test
    void resourceUpdate_notCfnEvent() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceType("SOME::OTHER::TYPE")
                                              .build());
        assertEquals(RESOURCE_UPDATE, result);
    }

    @Test
    void resourceUpdate_wrongLogicalId() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setLogicalResourceId("my-stack")
                                              .setStackName("some-other-stack")
                                              .build());
        assertEquals(RESOURCE_UPDATE, result);
    }

    @Test
    void resourceFailed() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceStatus("UPDATE_FAILED")
                                              .setLogicalResourceId("my-stack")
                                              .setStackName("some-other-stack")
                                              .build());
        assertEquals(RESOURCE_FAILED, result);
    }

    @Test
    void stackUpdated() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceStatus("UPDATE_COMPLETE")
                                              .build());
        assertEquals(STACK_UPDATED, result);
    }

    @Test
    void stackFailed() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceStatus("UPDATE_FAILED")
                                              .build());
        assertEquals(STACK_FAILED, result);
    }

    @Test
    void stackDeleted() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceStatus("DELETE_COMPLETE")
                                              .build());
        assertEquals(STACK_DELETED, result);
    }

    @Test
    void stackInProgress() {
        CfnEventType result = cfnSnsEventTypeResolver
                .resolve(StepGuardInputBuilder.aSnsTrigger()
                                              .setResourceStatus("CREATE_IN_PROGRESS")
                                              .build());
        assertEquals(STACK_IN_PROGRESS, result);
    }


}
