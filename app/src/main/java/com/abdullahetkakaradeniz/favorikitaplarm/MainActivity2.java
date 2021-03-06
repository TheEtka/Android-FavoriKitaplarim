package com.abdullahetkakaradeniz.favorikitaplarm;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity2 extends AppCompatActivity {

    Bitmap resim;
    ImageView imageView;
    EditText kitapAdiText, yazarText, yilText;
    Button button;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = findViewById(R.id.imageView);
        kitapAdiText = findViewById(R.id.kitapAdiText);
        yazarText = findViewById(R.id.yazarText);
        yilText = findViewById(R.id.yilText);
        button = findViewById(R.id.button);

        database = this.openOrCreateDatabase("Kitaplar",MODE_PRIVATE,null);

        Intent intent = getIntent();

        String info = intent.getStringExtra("info");

        if (info.matches("new")) {
            kitapAdiText.setText("");
            yazarText.setText("");
            yilText.setText("");
            button.setVisibility(View.VISIBLE);

            Bitmap kayitResim = BitmapFactory.decodeResource(getApplicationContext().getResources(),R.drawable.resim);
            imageView.setImageBitmap(kayitResim);


        } else {
            int kitapId = intent.getIntExtra("kitapId",1);
            button.setVisibility(View.INVISIBLE);

            try {

                Cursor cursor = database.rawQuery("SELECT * FROM kitaplar WHERE id = ?",new String[] {String.valueOf(kitapId)});

                int kitapAdiIx = cursor.getColumnIndex("kitapadi");
                int yazarIx = cursor.getColumnIndex("yazar");
                int yilIx = cursor.getColumnIndex("yil");
                int resimIx = cursor.getColumnIndex("resim");

                while (cursor.moveToNext()) {
                    kitapAdiText.setText(cursor.getString(kitapAdiIx));
                    yazarText.setText(cursor.getString(yazarIx));
                    yilText.setText(cursor.getString(yilIx));

                    byte[] bytes = cursor.getBlob(resimIx);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    imageView.setImageBitmap(bitmap);

                }

                cursor.close();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void resim(View view) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},1);
        } else {
            Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(intentToGallery,2);
        }

    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Intent intentToGallery = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(intentToGallery,2);
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == 2 && resultCode == RESULT_OK && data != null) {

            Uri resimData = data.getData();

            try {

                if (Build.VERSION.SDK_INT >= 28) {

                    ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(),resimData);
                    resim = ImageDecoder.decodeBitmap(source);
                    imageView.setImageBitmap(resim);

                } else {
                    resim = MediaStore.Images.Media.getBitmap(this.getContentResolver(),resimData);
                    imageView.setImageBitmap(resim);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }


        }

        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(getApplicationContext(),"Resim Eklendi",Toast.LENGTH_SHORT).show();
    }

    public void kaydet(View view) {

        String kitapAdi = kitapAdiText.getText().toString();
        String yazar = yazarText.getText().toString();
        String yil = yilText.getText().toString();

        Bitmap smallImage = makeSmallerImage(resim,300);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray = outputStream.toByteArray();

        try {

            database = this.openOrCreateDatabase("Kitaplar",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS kitaplar (id INTEGER PRIMARY KEY, kitapadi VARCHAR, yazar VARCHAR, yil VARCHAR, resim BLOB)");


            String sqlString = "INSERT INTO kitaplar (kitapadi, yazar, yil, resim) VALUES (?, ?, ?, ?)";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,kitapAdi);
            sqLiteStatement.bindString(2,yazar);
            sqLiteStatement.bindString(3,yil);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();


        } catch (Exception e) {

        }

        Intent intent = new Intent(MainActivity2.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP); //Daha ??nceki b??t??n aktiviteleri kapat??r!
        startActivity(intent);

        //finish();

        Toast.makeText(getApplicationContext(),"Kitap Eklendi",Toast.LENGTH_SHORT).show();

    }

    public Bitmap makeSmallerImage(Bitmap image, int maximumSize) {

        int width = image.getWidth();
        int height = image.getHeight();

        float bitmapRatio = (float) width / (float) height;

        if (bitmapRatio > 1) {
            width = maximumSize;
            height = (int) (width / bitmapRatio);
        } else {
            height = maximumSize;
            width = (int) (height * bitmapRatio);
        }

        return Bitmap.createScaledBitmap(image,width,height,true);
    }

}