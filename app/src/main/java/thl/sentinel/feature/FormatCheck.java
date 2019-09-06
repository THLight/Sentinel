package thl.sentinel.feature;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FormatCheck {
    public static boolean macFormatCheck(String mac)
    {
        return mac.matches("([\\da-fA-F]{2}(?::|$)){6}");
    }

    public static boolean uuidFormatCheck(String uuid)
    {
        return uuid.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    public static boolean stringHexFormatCheck(String hex)
    {
        return hex.matches("[0-9a-fA-F]+");
    }

    public static boolean integerFormatCheck(String number) {return  number.matches("-?[0-9]+");}

    public static boolean jsonFormatCheck(String json)
    {
        try {
            new JSONObject(json);
        } catch (JSONException ex) {
            try {
                new JSONArray(json);
            } catch (JSONException ex1) {
                return false;
            }
        }
        return true;
    }
}
