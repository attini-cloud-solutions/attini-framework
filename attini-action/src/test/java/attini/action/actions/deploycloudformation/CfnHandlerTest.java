package attini.action.actions.deploycloudformation;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import attini.action.actions.deploycloudformation.input.AttiniCfnInput;
import attini.action.actions.deploycloudformation.input.CfnConfig;
import attini.action.actions.deploycloudformation.stackconfig.StackConfigurationService;
import attini.action.builders.TestBuilders;
import attini.action.domain.DesiredState;
import attini.action.system.EnvironmentVariables;
import attini.domain.DeployOriginData;
import attini.domain.DistributionId;
import attini.domain.DistributionName;
import attini.domain.Environment;

@ExtendWith(MockitoExtension.class)
class CfnHandlerTest {

    @Mock
    private DeployCfnService deployCfnService;
    @Mock
    private DeployCfnCrossRegionService deployCfnCrossRegionService;
    @Mock
    private EnvironmentVariables environmentVariables;
    @Mock
    private CfnStackFacade cfnStackFacade;
    @Mock
    private StackConfigurationService stackConfigurationService;


    CfnHandler cfnHandler;

    @BeforeEach
    void setUp() {

        cfnHandler = new CfnHandler(deployCfnService,
                                    deployCfnCrossRegionService,
                                    environmentVariables,
                                    cfnStackFacade,
                                    stackConfigurationService);

    }


    @Test
    void shouldDeployWithPolling() {
        AttiniCfnInput attiniCfnInput = attiniCfnInput();

        when(environmentVariables.getRegion()).thenReturn("eu-west-1");

        when(stackConfigurationService.getStackConfig(any())).thenReturn(TestBuilders.aStackConfig().setRegion("eu-north-1").build());

        cfnHandler.deployCfn(attiniCfnInput);

        verify(deployCfnCrossRegionService).deployWithPolling(any());
    }

    @Test
    void shouldDeployWithCallback() {

        AttiniCfnInput attiniCfnInput = attiniCfnInput();

        when(stackConfigurationService.getStackConfig(any())).thenReturn(TestBuilders.aStackConfig().build());

        cfnHandler.deployCfn(attiniCfnInput);

        verify(deployCfnService).deployStack(any());

    }

    @Test
    void shouldDeleteWithPollingBecauseNoNotificationArn() {
        AttiniCfnInput attiniCfnInput = attiniCfnInput();

        when(stackConfigurationService.getStackConfig(any())).thenReturn(TestBuilders.aStackConfig().setDesiredState(
                DesiredState.DELETED).build());

        cfnHandler.deployCfn(attiniCfnInput);

        verify(deployCfnCrossRegionService).deployWithPolling(any());

    }

    @Test
    void shouldDeleteWithCallbackBecauseNoNotificationArn() {
        AttiniCfnInput attiniCfnInput = attiniCfnInput();

        when(cfnStackFacade.stackHasNotificationArn(any())).thenReturn(true);

        when(stackConfigurationService.getStackConfig(any())).thenReturn(TestBuilders.aStackConfig().setDesiredState(
                DesiredState.DELETED).build());

        cfnHandler.deployCfn(attiniCfnInput);

        verify(deployCfnService).deployStack(any());

    }

    public static AttiniCfnInput attiniCfnInput() {
        return new AttiniCfnInput(cfnConfig(), "AttiniCfn", TestBuilders.aMetaData(), null, deployOriginData(), null);
    }

    public static CfnConfig cfnConfig() {
        return new CfnConfig(null,
                             null,
                             null,
                             null,
                             null,
                             null,
                             null,
                             null,
                             "eu-west-1",
                             null,
                             null,
                             null);
    }

    public static DeployOriginData deployOriginData() {
        return new DeployOriginData(DistributionName.of("test-dist-name"),
                                    10000L,
                                    new DeployOriginData.DeploySource("prefix", "bucket"),
                                    Environment.of("dev"),
                                    DistributionId.of("test-dist"),
                                    null,
                                    null,
                                    "init-stack",
                                    null,
                                    null,
                                    false);
    }

}
