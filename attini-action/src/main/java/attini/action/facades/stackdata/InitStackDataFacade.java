package attini.action.facades.stackdata;

import java.util.Map;

public interface InitStackDataFacade {

    Map<String, String> getInitConfigVariables(String initStackName);

    Map<String, String> getInitStackParameters(String initStackName);
}
