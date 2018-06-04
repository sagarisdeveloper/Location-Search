package com.sagar.locationsearch;

import android.app.Dialog;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "";
    private static final int ERROR_DIALOG = 900;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isServiceOK();
        init();
    }

    public boolean isServiceOK() {
        Log.d(TAG, "isServiceOK: ");
        int avaliable = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (avaliable == ConnectionResult.SUCCESS) {
            return true;
        } else if (GoogleApiAvailability.getInstance().isUserResolvableError(avaliable)) {
            Log.d(TAG, "isServiceOK: an error is occured");
            Dialog dialog = GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, avaliable, ERROR_DIALOG);
            dialog.show();

        } else {
            Toast.makeText(this, "You can't make map request", Toast.LENGTH_SHORT).show();
        }
        return false;
    }
    void init(){
        Button button=findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(MainActivity.this,MapActivity.class);
                startActivity(intent);
            }
        });

    }
}
