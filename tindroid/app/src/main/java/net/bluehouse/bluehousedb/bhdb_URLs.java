package net.bluehouse.bluehousedb;

public class bhdb_URLs{

    public static final String API_KEY = "kdD345dKsfJSd3DFU6W235dRWT2d34EFGJ3453HSAdJ23HdfgD";

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //private static final String ROOT_URL = "http://10.0.2.2:5000";
    private static final String ROOT_URL = "http://192.168.1.102:5000";
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String BLUEPRINT_phauth = ROOT_URL + "/phauth";

    public static final String URL_REGISTER_REQUEST = BLUEPRINT_phauth + "/signup_request";
    public static final String URL_REGISTER = BLUEPRINT_phauth + "/register";
    public static final String URL_VERIFYIMG = BLUEPRINT_phauth + "/verifying";
    public static final String URL_lOGINPASS = BLUEPRINT_phauth + "/loginpass";
    ////////////////////////////////////////////////////////////////////////////////////////////////
    private static final String BLUEPRINT_apartment = ROOT_URL + "/apartment";

    public static final String URL_CreateApartment = BLUEPRINT_apartment + "/create_apartment";
    ////////////////////////////////////////////////////////////////////////////////////////////////
}