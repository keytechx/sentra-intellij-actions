package org.intellij.sdk.action.storages;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;

@Service
@State(
        name = "UserTokenStorage",
        storages = @Storage("userToken.xml")  // The token will be stored in a file named userToken.xml
)
public final class UserTokenStorage implements PersistentStateComponent<UserTokenStorage.State> {

    public static class State {
        public String userToken;
        public String accessToken;
    }

    private State myState = new State();

    public UserTokenStorage() {
        System.out.println("UserTokenStorage Initialized!");
    }

    @Override
    public State getState() {
        return myState;
    }

    @Override
    public void loadState(State state) {
        myState = state;
        System.out.println("State Loaded: " + myState.userToken); // Check if state is loaded
    }

    // Get the token
    public String getUserToken() {
        return myState.userToken;
    }

    public String getAccessToken() {
        return myState.accessToken;
    }

    // Set the token
    public void setUserToken(String token) {
        myState.userToken = token;
    }

    public void setAccessToken(String token) {
        myState.accessToken = token;
    }
}

