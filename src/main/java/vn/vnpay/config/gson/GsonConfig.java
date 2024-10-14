package vn.vnpay.config.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GsonConfig {
    private static Gson gsonInstance;

    private GsonConfig() {
    }

    public static Gson getGson() {
        if (gsonInstance == null) {
            gsonInstance = new GsonBuilder()
                    .setPrettyPrinting()
                    .serializeNulls()
                    .create();
        }
        return gsonInstance;
    }
}
