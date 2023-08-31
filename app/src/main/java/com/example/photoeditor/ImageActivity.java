package com.example.photoeditor;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;

public class ImageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);
        Intent i=getIntent();
        String ascii_text=i.getStringExtra(MainActivity.EXTRA_TEXT);

        TextView tv= findViewById(R.id.tv);
        tv.setTypeface(Typeface.MONOSPACE);
        tv.setText(ascii_text);
        Button copy=findViewById(R.id.copy_button);
        copy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the system clipboard service
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);

                // Create a new ClipData object to hold the text to be copied
                ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
                Toast.makeText(ImageActivity.this,"Copied",Toast.LENGTH_SHORT).show();

                // Set the clipboard's primary clip with the new ClipData object
                clipboard.setPrimaryClip(clip);
            }
        });

//
//        // Get the system clipboard service
//        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
//
//        // Create a new ClipData object to hold the text to be copied
//        ClipData clip = ClipData.newPlainText("label", tv.getText().toString());
//        Toast.makeText(ImageActivity.this,"Copied",Toast.LENGTH_SHORT).show();
//
//        // Set the clipboard's primary clip with the new ClipData object
//        clipboard.setPrimaryClip(clip);

    }
}