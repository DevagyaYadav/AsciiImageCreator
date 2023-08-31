package com.example.photoeditor;


 import static java.lang.StrictMath.log;

 import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
 import androidx.appcompat.app.AppCompatActivity;
 import androidx.core.content.FileProvider;

 import android.app.Activity;
import android.app.ActivityManager;
 import android.app.AlertDialog;
 import android.content.DialogInterface;
 import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
 import android.graphics.Canvas;
 import android.graphics.Color;
 import android.graphics.ColorMatrix;
 import android.graphics.ColorMatrixColorFilter;
 import android.graphics.Matrix;
 import android.graphics.Paint;
 import android.graphics.Typeface;
 import android.media.MediaScannerConnection;
 import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
 import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
 import android.widget.ImageView;
 import android.widget.TextView;
 import android.widget.Toast;
 import android.util.Log;



 import java.io.ByteArrayInputStream;
 import java.io.ByteArrayOutputStream;
 import java.io.File;
 import java.io.FileNotFoundException;
 import java.io.FileOutputStream;
 import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
 import java.util.Locale;


public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_TEXT="com.example.photoeditor.EXTRA_TEXT";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_layout);
        


        init();

    }



    //this is how permissions are taken in apps
    private static final int REQUEST_PERMISSIONS=1234;
    private static final String[] PERMISSIONS = {
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    //check for permissions 
    private static final int PERMISSIONS_COUNT=2;
    private boolean notPermissions(){
        for ( int i=0;i<PERMISSIONS_COUNT;i++)
        {
            if(checkSelfPermission(PERMISSIONS[i])!=PackageManager.PERMISSION_GRANTED)
            {
                return true;
            }
        }return false;
    }

    static {
        System.loadLibrary("photoEditor");
    }

    private static native void greyscale(int[] pixels,int width, int height);
    // if permissions are not given then ask for permissions

    @Override
    protected void onResume(){
        super.onResume();
        if(notPermissions()){
            requestPermissions(PERMISSIONS, REQUEST_PERMISSIONS);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCOde, String[] permissions, int [] grantResults)
    {
        super.onRequestPermissionsResult(requestCOde,permissions,grantResults);
        if(requestCOde==REQUEST_PERMISSIONS &&  grantResults.length>0)
        {
            //if permissions are not given then close the app and ask for permissions again when app is opened
            if(notPermissions()){
                ((ActivityManager)this.getSystemService(ACTIVITY_SERVICE)).clearApplicationUserData();
                recreate();
            }}
    }

    public void onBackPressed(){
        if(editmode){
            findViewById(R.id.welcome_screen).setVisibility(View.VISIBLE);
            findViewById(R.id.editScreen).setVisibility(View.GONE);
            editmode=false;
        }
        else{
            super.onBackPressed();
        }

    }





    private void init() {

        ImageView imageView=findViewById(R.id.imageView);



        //if device has no camera then remove the take photo button
        if (!MainActivity.this.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            findViewById(R.id.takeImagebutton).setVisibility(View.GONE);


        }




        final Button selectImageButton = findViewById(R.id.selectImagebutton);

        selectImageButton.setOnClickListener(view -> {
            final Intent intent =new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            final Intent pickIntent = new Intent(Intent.ACTION_PICK);
            pickIntent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*");
            final Intent chooserIntent =Intent.createChooser(intent,"Select Image");



            pickPhoto.launch(chooserIntent);



        });


        final Button takeImageButton = findViewById(R.id.takeImagebutton);
        takeImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

                // Ensure that there is a camera activity to handle the intent
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    // Create a file to store the image
                    File photoFile = null;
                    try {
                        photoFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        ex.printStackTrace();
                    }


                    // If the file was successfully created, start the camera activity
                    if (photoFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(MainActivity.this,
                                "com.example.android.fileprovider",
                                photoFile);
                        takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        takephotointent.launch(takePictureIntent);

                    }
                }

            }
       });



        final Button asciiImage=findViewById(R.id.nothing2);
        asciiImage.setOnClickListener(view -> {
            new Thread(){
                public void run(){
                    String ascii_text= convertToTextImage(bitmap);
                    Intent intent=new Intent(MainActivity.this,ImageActivity.class);
                    intent.putExtra(EXTRA_TEXT,ascii_text);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            startActivity(intent);


                        }
                    });

                }
            }.start();

        });
        final Button LargeasciiImage=findViewById(R.id.smallTextImage);
        LargeasciiImage.setOnClickListener(view -> {
            new Thread(){
                public void run(){
                    String ascii_text= convertToLargeTextImage(bitmap);
                    Intent intent=new Intent(MainActivity.this,ImageActivity.class);
                    intent.putExtra(EXTRA_TEXT,ascii_text);

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            startActivity(intent);


                        }
                    });

                }
            }.start();

        });

        final Button greyScale =findViewById(R.id.greyScale);
        greyScale.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public void run(){

                        //greyscale(pixels,width,height);
                        int alpha = 0xFF << 24; // ?bitmap?24?
                        for (int i = 0; i < height; i++) {
                            for (int j = 0; j < width; j++) {
                                int grey = pixels[width * i + j];

                                int red = ((grey & 0x00FF0000) >> 16);
                                int green = ((grey & 0x0000FF00) >> 8);
                                int blue = (grey & 0x000000FF);

                                grey = (int) (red * 0.3 + green * 0.59 + blue * 0.11);
                                grey = alpha | (grey << 16) | (grey << 8) | grey;
                                pixels[width * i + j] = grey;
                            }
                        }
                        Bitmap newBmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
                        newBmp.setPixels(pixels, 0, width, 0, 0, width, height);
                        bitmap=newBmp;

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(newBmp);
                            }
                        });

                    }
                }.start();
            }
        });

       final Button GaussianBlur=findViewById(R.id.blurImage);
       GaussianBlur.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               new Thread(){
                   public void run(){

                       int numPixels = width * height;
                       int[] in = new int[numPixels];
                       int[] tmp = new int[numPixels];
                       bitmap.getPixels(in, 0, width, 0, 0, width, height);

                       gaussianBlurFilter(in, tmp, width, height);
                       gaussianBlurFilter(tmp, in, width, height);
                       // Return a bitmap scaled to the desired size.
                       Bitmap filtered = Bitmap.createBitmap(in, width, height, Bitmap.Config.ARGB_8888);
                       bitmap=filtered;
                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                              imageView.setImageBitmap(filtered);
                           }
                       });
                   }
               }.start();
           }
       });

       final Button Negative= findViewById(R.id.negative);
       Negative.setOnClickListener(new View.OnClickListener() {
           @Override
           public void onClick(View view) {
               new Thread(){
                   public void run(){
                       Bitmap base = Bitmap
                               .createBitmap(width, height, bitmap.getConfig());
                       Paint paint = new Paint();

                       Canvas canvas = new Canvas(base);
                       canvas.drawBitmap(bitmap, new Matrix(), paint);
                       for (int i = 0; i < width; i++) {
                           for (int j = 0; j < height; j++) {
                               int color = bitmap.getPixel(i, j);
                               int r = Color.red(color);
                               int g = Color.green(color);
                               int b = Color.blue(color);
                               int a = Color.alpha(color);

                               base.setPixel(i, j,
                                       Color.argb(a, 255 - r, 255 - g, 255 - b));
                           }
                       }
                       bitmap=base;


                       runOnUiThread(new Runnable() {
                           @Override
                           public void run() {
                               imageView.setImageBitmap(base);
                           }
                       });

                   }
               }.start();
           }
       });

        final Button nothing= findViewById(R.id.nothing3);
        nothing.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public void run(){
                        Bitmap blurredbitmap=sharpen(bitmap);
                        bitmap=blurredbitmap;




                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(blurredbitmap);

                            }
                        });

                    }
                }.start();
            }
        });

        final Button nothing4= findViewById(R.id.nothing4);




        final Button compress= findViewById(R.id.nothing);
        compress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new Thread(){
                    public void run(){
                        Bitmap blurredbitmap=compressImage(bitmap);
                        bitmap=blurredbitmap;




                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(blurredbitmap);

                            }
                        });

                    }
                }.start();
            }
        });

        final Button saveImage = findViewById(R.id.saveImage);
        saveImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                final DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        if(i == dialogInterface.BUTTON_POSITIVE) {
                            // Create a file to save the image
                            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                            String fileName = "IMG_" + timeStamp + ".jpg";

                            final File outFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                                    fileName);
                            try {
                                FileOutputStream out = new FileOutputStream(outFile);
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                                out.flush();
                                out.close();

                                // Tell the media scanner about the new file so that it is immediately available to the user.
                                MediaScannerConnection.scanFile(MainActivity.this, new String[]{outFile.toString()}, null,
                                        null);

                                // Show a toast message to indicate that the image has been saved
                                Toast.makeText(MainActivity.this, "Image saved successfully", Toast.LENGTH_SHORT).show();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                };
                builder.setMessage("Save image to gallery?")
                        .setPositiveButton("Yes", dialogClickListener)
                        .setNegativeButton("No", dialogClickListener)
                        .show();
            }
        });

        final Button back=findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findViewById(R.id.welcome_screen).setVisibility(View.VISIBLE);
                findViewById(R.id.editScreen).setVisibility(View.GONE);
            }
        });
    }


    private static final String appID = "photoEditor";
    private Uri imageUri;


    private boolean editmode=false;
    private Bitmap bitmap;
    private int width=0;
    private int height=0;
    private static final int MAX_PIXEL_COUNT=2048;
    private int[] pixels;
    private int pixelCount=0;


    private static final String TAG = "BitmapUtils";
    private static boolean DBG = true;
    private static final int RED_MASK = 0xff0000;
    private static final int RED_MASK_SHIFT = 16;
    private static final int GREEN_MASK = 0x00ff00;
    private static final int GREEN_MASK_SHIFT = 8;
    private static final int BLUE_MASK = 0x0000ff;


    ActivityResultLauncher<Intent> pickPhoto
            = registerForActivityResult(
            new ActivityResultContracts
                    .StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    Intent data = result.getData();


                    imageUri = data.getData();


                }
                setImageview();

            });

    private String currentPhotoPath;
    ActivityResultLauncher<Intent> takephotointent
            = registerForActivityResult(
            new ActivityResultContracts
                    .StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    imageUri = Uri.fromFile(file);
                }
                setImageview();

            });






    // Create a file with a unique name to store the image
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }



//    private static final char[] asciiChars = { ' ', '.', ':', '-', '=', '+', '*', '#', '%', '@' };
//
//    public Bitmap convertToAsciiArt(Bitmap originalBitmap) {
//        // Define the characters to use in the ASCII art, from darkest to lightest
//        String characters = "@#8&WM0QO*o^~,:;'`.";
//
//        // Get the dimensions of the original bitmap
//        int width = originalBitmap.getWidth();
//        int height = originalBitmap.getHeight();
//
//        // Calculate the aspect ratio of the original bitmap
//        double aspectRatio = (double) height / width;
//
//        // Define the desired width and height of the ASCII art
//        int asciiWidth = 100;
//        int asciiHeight = (int) (asciiWidth * aspectRatio);
//
//        // Calculate the width and height of each ASCII character cell
//        int cellWidth = width / asciiWidth;
//        int cellHeight = height / asciiHeight;
//
//        // Create a new bitmap to hold the ASCII art
//        Bitmap asciiBitmap = Bitmap.createBitmap(asciiWidth, asciiHeight, Bitmap.Config.ARGB_8888);
//
//        // Iterate over each cell in the ASCII art
//        for (int row = 0; row < asciiHeight; row++) {
//            for (int col = 0; col < asciiWidth; col++) {
//                // Calculate the average brightness of the cell in the original bitmap
//                int totalBrightness = 0;
//                for (int x = col * cellWidth; x < (col + 1) * cellWidth; x++) {
//                    for (int y = row * cellHeight; y < (row + 1) * cellHeight; y++) {
//                        int color = originalBitmap.getPixel(x, y);
//                        int brightness = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
//                        totalBrightness += brightness;
//                    }
//                }
//                int averageBrightness = totalBrightness / (cellWidth * cellHeight);
//
//                // Map the average brightness to an ASCII character
//                int index = (int) (averageBrightness / 255.0 * (characters.length() - 1));
//                char character = characters.charAt(index);
//
//                // Create a new bitmap with the ASCII character drawn in it
//                Bitmap characterBitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888);
//                Canvas canvas = new Canvas(characterBitmap);
//                Paint paint = new Paint();
//                paint.setColor(Color.BLACK);
//                paint.setTextSize(cellHeight);
//                paint.setTypeface(Typeface.MONOSPACE);
//                paint.setTextAlign(Paint.Align.CENTER);
//                canvas.drawText(String.valueOf(character), cellWidth / 2, cellHeight - (cellHeight / 5), paint);
//
//                // Draw the new bitmap into the ASCII art bitmap
//                canvas = new Canvas(asciiBitmap);
//                canvas.drawBitmap(characterBitmap, col * cellWidth, row * cellHeight, null);
//            }
//        }
//
//        // Return the ASCII art bitmap
//        return asciiBitmap;
//    }
//    public Bitmap convertToAsciiArt2(Bitmap originalBitmap) {
//        // Define the characters to use in the ASCII art, from darkest to lightest
//        String characters = "@#8&WM0QO*o^~,:;'`.";
//
//        // Get the dimensions of the original bitmap
//        int width = originalBitmap.getWidth();
//        int height = originalBitmap.getHeight();
//
//        // Calculate the aspect ratio of the original bitmap
//        double aspectRatio = (double) height / width;
//
//        // Define the desired width and height of the ASCII art
//        int asciiWidth = 500;
//        int asciiHeight = (int) (asciiWidth * aspectRatio);
//
//        // Calculate the width and height of each ASCII character cell
//        int cellWidth = width / asciiWidth;
//        int cellHeight = height / asciiHeight;
//
//        // Create a new bitmap to hold the ASCII art
//        Bitmap asciiBitmap = Bitmap.createBitmap(asciiWidth, asciiHeight, Bitmap.Config.ARGB_8888);
//
//        // Initialize the canvas with a white background
//        Canvas canvas = new Canvas(asciiBitmap);
//        canvas.drawColor(Color.WHITE);
//
//        Bitmap characterBitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888);
//        // Iterate over each cell in the ASCII art
//        for (int row = 0; row < asciiHeight; row++) {
//            for (int col = 0; col < asciiWidth; col++) {
//                // Calculate the average brightness of the cell in the original bitmap
//                int totalBrightness = 0;
//                for (int x = col * cellWidth; x < (col + 1) * cellWidth; x++) {
//                    for (int y = row * cellHeight; y < (row + 1) * cellHeight; y++) {
//                        int color = originalBitmap.getPixel(x, y);
//                        int brightness = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
//                        totalBrightness += brightness;
//                    }
//                }
//                int averageBrightness = totalBrightness / (cellWidth * cellHeight);
//
//                // Map the average brightness to an ASCII character
//                int index = (int) (averageBrightness / 255.0 * (characters.length() - 1));
//                char character = characters.charAt(index);
//
//                // Create a new bitmap with the ASCII character drawn in it
////                Bitmap characterBitmap = Bitmap.createBitmap(cellWidth, cellHeight, Bitmap.Config.ARGB_8888);
////                Canvas characterCanvas = new Canvas(characterBitmap);
////                characterCanvas.drawColor(Color.WHITE);
//                Paint paint = new Paint();
//                paint.setColor(Color.BLACK);
//                paint.setTextSize(cellHeight);
//                paint.setTypeface(Typeface.MONOSPACE);
//                paint.setTextAlign(Paint.Align.CENTER);
//            //    characterCanvas.drawText(String.valueOf(character), cellWidth / 2, cellHeight - (cellHeight / 5), paint);
//
//
//                // Draw the new bitmap into the ASCII art bitmap
//               // canvas.drawBitmap(characterBitmap, col * cellWidth, row * cellHeight, null);
//                canvas.drawText(String.valueOf(character),col,row,paint);
//            }
//        }
//
//        // Return the ASCII art bitmap
//        return asciiBitmap;
//    }


    public String convertToTextImage(Bitmap bitmap) {
        // Define the characters to use in the text image, from darkest to lightest
        String characters = "@#8&WM0QO*o^~,:;'`.";

        // Get the dimensions of the bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate the aspect ratio of the bitmap
        double aspectRatio = (double) height / width;

        // Calculate the number of columns and rows in the text image
        int columns = 50;
        int rows = (int) (columns * aspectRatio);

        // Calculate the width and height of each cell in the text image
        int cellWidth = width / columns;
        int cellHeight = height / rows;

        // Create a new string builder to hold the text image
        StringBuilder sb = new StringBuilder();

        // Iterate through each cell in the text image
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Calculate the average brightness of the cell
                int totalBrightness = 0;
                for (int x = col * cellWidth; x < (col + 1) * cellWidth; x++) {
                    for (int y = row * cellHeight; y < (row + 1) * cellHeight; y++) {
                        int color = bitmap.getPixel(x, y);
                        int brightness = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
                        totalBrightness += brightness;
                    }
                }
                int averageBrightness = totalBrightness / (cellWidth * cellHeight);

                // Map the average brightness to a character in the text image
                int index = (int) (averageBrightness / 255.0 * (characters.length() - 1));
                char character = characters.charAt(index);

                // Append the character to the string builder
                sb.append(character);
            }

            // Add a newline character at the end of each row
            sb.append("\n");
        }

        // Return the text image as a string
        return sb.toString();
    }

    public String convertToLargeTextImage(Bitmap bitmap) {
        // Define the characters to use in the text image, from darkest to lightest
        String characters = "@#8&WM0QO*o^~,:;'`. ";

        // Get the dimensions of the bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Calculate the aspect ratio of the bitmap
        double aspectRatio = (double) height / width;

        // Calculate the number of columns and rows in the text image
        int columns = 100;
        int rows = (int) (columns * aspectRatio);

        // Calculate the width and height of each cell in the text image
        int cellWidth = width / columns;
        int cellHeight = height / rows;

        // Create a new string builder to hold the text image
        StringBuilder sb = new StringBuilder();

        // Iterate through each cell in the text image
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < columns; col++) {
                // Calculate the average brightness of the cell
                int totalBrightness = 0;
                for (int x = col * cellWidth; x < (col + 1) * cellWidth; x++) {
                    for (int y = row * cellHeight; y < (row + 1) * cellHeight; y++) {
                        int color = bitmap.getPixel(x, y);
                        int brightness = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3;
                        totalBrightness += brightness;
                    }
                }
                int averageBrightness = totalBrightness / (cellWidth * cellHeight);

                // Map the average brightness to a character in the text image
                int index = (int) (averageBrightness / 255.0 * (characters.length() - 1));
                char character = characters.charAt(index);

                // Append the character to the string builder
                sb.append(character);
            }

            // Add a newline character at the end of each row
            sb.append("\n");
        }

        // Return the text image as a string
        return sb.toString();
    }



//
//        public String convertToAscii(Bitmap bitmap) {
//            // Convert bitmap to OpenCV Mat
//            Mat mat = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
//            Utils.bitmapToMat(bitmap, mat);
//
//            // Convert to grayscale
//            Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY);
//
//            // Resize the image
//            int targetWidth = 80; // Adjust this value for desired output width
//            int targetHeight = (int) (mat.rows() * (targetWidth / (double) mat.cols()));
//            Size targetSize = new Size(targetWidth, targetHeight);
//            Imgproc.resize(mat, mat, targetSize);
//
//            // Define the ASCII characters
//            char[] asciiChars = {'@', '#', 'S', '%', '?', '*', '+', ';', ':', ',', '.'};
//
//            // Convert pixel values to ASCII characters
//            StringBuilder asciiArt = new StringBuilder();
//            for (int y = 0; y < mat.rows(); y++) {
//                for (int x = 0; x < mat.cols(); x++) {
//                    double[] pixel = mat.get(y, x);
//                    int grayValue = (int) pixel[0];
//                    int charIndex = grayValue * (asciiChars.length - 1) / 255;
//                    asciiArt.append(asciiChars[charIndex]);
//                }
//                asciiArt.append("\n");
//            }
//
//            // Release resources
//            mat.release();
//
//            // Return the ASCII art as a string
//            return asciiArt.toString();
//        }



    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 80, baos);//100baos
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) { //100kb,
            baos.reset();//baosbaos
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//options%baos
            options -= 10;//10
        }/*from w  ww .  jav  a2 s  .c  o m*/
        ByteArrayInputStream isBm = new ByteArrayInputStream(
                baos.toByteArray());//baosByteArrayInputStream
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//ByteArrayInputStream
        return bitmap;
    }

    public static Bitmap sharpen(Bitmap bitmap) {

        int[] laplacian = new int[] { -1, -1, -1, -1, 9, -1, -1, -1, -1 };

        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.RGB_565);

        int pixR = 0;
        int pixG = 0;
        int pixB = 0;

        int pixColor = 0;

        int newR = 0;
        int newG = 0;
        int newB = 0;

        int idx = 0;
        float alpha = 0.3F;
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
        for (int i = 1, length = height - 1; i < length; i++) {
            for (int k = 1, len = width - 1; k < len; k++) {
                idx = 0;
                for (int m = -1; m <= 1; m++) {
                    for (int n = -1; n <= 1; n++) {
                        pixColor = pixels[(i + n) * width + k + m];
                        pixR = Color.red(pixColor);
                        pixG = Color.green(pixColor);
                        pixB = Color.blue(pixColor);

                        newR = newR + (int) (pixR * laplacian[idx] * alpha);
                        newG = newG + (int) (pixG * laplacian[idx] * alpha);
                        newB = newB + (int) (pixB * laplacian[idx] * alpha);
                        idx++;
                    }
                }

                newR = Math.min(255, Math.max(0, newR));
                newG = Math.min(255, Math.max(0, newG));
                newB = Math.min(255, Math.max(0, newB));

                pixels[i * width + k] = Color.argb(255, newR, newG, newB);
                newR = 0;
                newG = 0;
                newB = 0;
            }
        }

        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    private static void gaussianBlurFilter(int[] in, int[] out, int width, int height) {
        // This function is currently hardcoded to blur with RADIUS = 4.
        // (If you change RADIUS, you'll have to change the weights[] too.)
        final int RADIUS = 4;
        final int[] weights = { 13, 23, 32, 39, 42, 39, 32, 23, 13 }; // Adds up to 256
        int inPos = 0;
        int widthMask = width - 1; // width must be a power of two.
        for (int y = 0; y < height; ++y) {
            // Compute the alpha value.
            int alpha = 0xff;
            // Compute output values for the row.
            int outPos = y;
            for (int x = 0; x < width; ++x) {
                int red = 0;
                int green = 0;
                int blue = 0;
                for (int i = -RADIUS; i <= RADIUS; ++i) {
                    int argb = in[inPos + (widthMask & (x + i))];
                    int weight = weights[i + RADIUS];
                    red += weight * ((argb & RED_MASK) >> RED_MASK_SHIFT);
                    green += weight
                            * ((argb & GREEN_MASK) >> GREEN_MASK_SHIFT);
                    blue += weight * (argb & BLUE_MASK);
                }
                // Output the current pixel.
                out[outPos] = (alpha << 24)
                        | ((red >> 8) << RED_MASK_SHIFT)
                        | ((green >> 8) << GREEN_MASK_SHIFT) | (blue >> 8);
                outPos += height;
            }
            inPos += width;
        }
    }


    public static Bitmap compressImage(Bitmap image, int quality) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, quality, outputStream);
        byte[] imageBytes = outputStream.toByteArray();
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);
    }


    public Bitmap convertToDotImage(Bitmap bitmap) {
        // Get the dimensions of the bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        // Create a new bitmap with the same dimensions
        Bitmap dotBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

        // Iterate through each pixel of the original bitmap
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                // Get the color of the pixel
                int color = bitmap.getPixel(x, y);

                // Calculate the luminance (brightness) of the pixel
                double r = Color.red(color);
                double g = Color.green(color);
                double b = Color.blue(color);
                double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255;

                // If the luminance is less than 0.5, set the pixel to black; otherwise, set it to white
                if (luminance < 0.5) {
                    dotBitmap.setPixel(x, y, Color.BLACK);
                } else {
                    dotBitmap.setPixel(x, y, Color.WHITE);
                }
            }
        }

        // Return the dot bitmap
        return dotBitmap;
    }










    void setImageview(){
         ImageView imageView=findViewById(R.id.imageView);
        editmode=true;
        findViewById(R.id.welcome_screen).setVisibility(View.GONE);
        findViewById(R.id.editScreen).setVisibility(View.VISIBLE);

        new Thread(){
            public void run()
            {
                bitmap=null;
                final BitmapFactory.Options bmpOptions =new BitmapFactory.Options();
                bmpOptions.inBitmap=bitmap;
                bmpOptions.inJustDecodeBounds=true;
                try(InputStream input = getContentResolver().openInputStream(imageUri)) {
                    bitmap= BitmapFactory.decodeStream(input ,null,bmpOptions);
                }
                catch (IOException e){
                    e.printStackTrace();
                }
                bmpOptions.inJustDecodeBounds=false;
                width=bmpOptions.outWidth;
                height=bmpOptions.outHeight;
                int resizeScale=1;
                if(width>MAX_PIXEL_COUNT)
                {
                    resizeScale=width/MAX_PIXEL_COUNT;
                }
                else if(height>MAX_PIXEL_COUNT)
                {
                    resizeScale=height/MAX_PIXEL_COUNT;
                }
                if (width / resizeScale > MAX_PIXEL_COUNT || height / resizeScale > MAX_PIXEL_COUNT)
                {
                    resizeScale++;
                }
                bmpOptions.inSampleSize = resizeScale;
                InputStream input = null;
                try{
                    input = getContentResolver().openInputStream(imageUri);
                }catch (FileNotFoundException e){
                    e.printStackTrace(); recreate();
                    return;
                }
                bitmap = BitmapFactory. decodeStream(input, null, bmpOptions);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                       imageView.setImageBitmap(bitmap);
                    }
                });
                width=bitmap.getWidth();
                height=bitmap.getHeight();
                bitmap=bitmap.copy(Bitmap.Config.ARGB_8888,true);

                pixelCount=width*height;
                pixels =new int[pixelCount];
                bitmap.getPixels(pixels,0,width,0,0,width,height);



            }
        }.start();
    }



  }