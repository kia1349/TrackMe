package ie.nuigalway.trackme.helper;

/**
 * Created by matthew on 17/01/2017.
 * https://developer.android.com/reference/android/database/sqlite/SQLiteOpenHelper.html
 * Class handles local database stored on device storage.
 */

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;
import android.database.Cursor;

import java.util.HashMap;


public class LocalDBHandler extends SQLiteOpenHelper{

    private static final String TAG = LocalDBHandler.class.getSimpleName();
    private static final int VERSION = 1;
    private static final String DB_NAME = "trackMe_db";

    private static final String TABLE_USER_DETAILS = "user";
    private static final String TABLE_USER_LOCATION = "location";

    /*
    * Fields created for use in local database tables
    * */
    private static final String ID = "id";
    private static final String FN = "first_name";
    private static final String SN = "surname";
    private static final String EMAIL = "email";
    private static final String PHNO = "phone_no";
    private static final String UID = "unique_id";
    private static final String CR = "created_at";


    public LocalDBHandler(Context ctx){

        super(ctx, DB_NAME, null, VERSION);

    }
    @Override
    public void onCreate(SQLiteDatabase db){

        /*
        * Creating User Table
        * */
        String CREATE_USER_TABLE = "CREATE TABLE " + TABLE_USER_DETAILS+ "("
                + ID + " INTEGER PRIMARY KEY," + FN + " TEXT,"+ SN + " TEXT,"
                + EMAIL + " TEXT UNIQUE," + PHNO + " TEXT,"
                + UID + " TEXT" + CR + " TEXT" + ")";

        db.execSQL(CREATE_USER_TABLE);

        Log.d(TAG, "Database tables created");

        /*
        * Placeholder to create user location table
        * */


    }

    public void addUser(String fn, String sn, String email, String phno, String uid, String cr){


        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues vals = new ContentValues();//empty set of values that will be used to store user info
        vals.put(FN,fn);
        vals.put(SN,sn);
        vals.put(EMAIL, email);
        vals.put(PHNO,phno);
        vals.put(UID,uid);
        vals.put(CR, cr);

        long ins = db.insert(TABLE_USER_DETAILS, null, vals);
        db.close(); // Closing database connection

        Log.d(TAG, "User inserted into db table "+TABLE_USER_DETAILS+ "  " + ins);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int o, int n){

        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_DETAILS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USER_LOCATION);
        onCreate(db);//creates db with tables again

    }


    public void deleteTables(){


    }

    public HashMap<String, String> getUserDetails(){



        HashMap<String, String> userDetails = new HashMap<String, String>();
        String query = "SELECT  * FROM " + TABLE_USER_DETAILS;

        SQLiteDatabase db = this.getReadableDatabase();

        /*
        * https://developer.android.com/reference/android/database/Cursor.html
        * R/W access to obj returned by db query
        * */
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        if (cursor.getCount() > 0) {
            userDetails.put("first_name", cursor.getString(1));
            userDetails.put("surname", cursor.getString(2));
            userDetails.put("email", cursor.getString(3));
            userDetails.put("phone_no", cursor.getString(4));
            userDetails.put("unique_id", cursor.getString(5));
            userDetails.put("created_at", cursor.getString(6));
        }
        cursor.close();

        db.close();
        // return user
        Log.d(TAG, "Fetching user from Sqlite: " + userDetails.toString());

        return userDetails;


    }

}







