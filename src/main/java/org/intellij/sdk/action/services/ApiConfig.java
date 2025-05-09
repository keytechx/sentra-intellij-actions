package org.intellij.sdk.action.services;

public class ApiConfig {
    private static final String BASE_URL = System.getenv("API_BASE_URL") != null
            ? System.getenv("API_BASE_URL")
            : "http://localhost:8080/api/v1";

    private static final String UNIT_TEST_BASE_URL = BASE_URL + "/unit-test-results";
    private static final String USER_TOKEN_BASE_URL = BASE_URL + "/user-tokens";

    public static class API_ENDPOINTS {
        public static final String GENERATE_UNIT_TEST = UNIT_TEST_BASE_URL + "/generate_unit_test";
        public static final String EXTRACT_BASE_CLASS = UNIT_TEST_BASE_URL + "/extract_base_class";
        public static final String MERGE_CLASS = UNIT_TEST_BASE_URL + "/merge_class";
        public static final String REGISTER_USER_TOKEN = USER_TOKEN_BASE_URL + "/register";
        public static final String GENERATE_USER_TOKEN = USER_TOKEN_BASE_URL + "/generate-token";
        public static final String CHECK_ACCESS_TOKEN = USER_TOKEN_BASE_URL + "/do/check";
    }
}
