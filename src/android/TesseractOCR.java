package com.juleskelly.tesseract;

// android imports
import android.os.Bundle;
import android.os.Environment;
import android.content.res.AssetManager;
import android.util.Log;
import android.util.Base64;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;


// java imports
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.lang.*;

// cordova imports
import org.apache.cordova.*;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

// TessBaseAPI import for tesseract
import com.googlecode.tesseract.android.TessBaseAPI;
import com.googlecode.leptonica.android.Pixa;


public class TesseractOCR extends CordovaPlugin {
  public final String ACTION_LOADENGINE = "loadEngine";
  public final String ACTION_RECOGNIZEIMAGE = "recognizeImage";
  public final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/OCRFolder/";
  public final String TAG = "OCREngine";
  public final String lang = "eng";

  @Override
  public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
    if (ACTION_LOADENGINE.equals(action)) {
      loadEngine(callbackContext);
      return true;
    }
    else if (ACTION_RECOGNIZEIMAGE.equals(action)) {
      JSONObject arg_obj = args.getJSONObject(0);
      String imagePath = arg_obj.getString("imageURL");

      recognizeImage(imagePath, callbackContext);
    };


    return false;
  }

  // load training data for tesseract
  public void loadEngine(final CallbackContext callbackContext) {
    String[] paths = new String[] {DATA_PATH, DATA_PATH + "tessdata/"};
    // create directories if they did not exist
    for (String path : paths) {
      File dir = new File(path);

      if (!dir.exists()) {
        if (!dir.mkdirs()) {
          Log.e(TAG, "Error: Creation of directory " + path + " on sdcard failed");
          callbackContext.error("Error: Creation of directory " + path + " on sdcard failed");
          return;
        }
        else {
          Log.e(TAG, "Directory " + path + " created on sdcard.");
        }
      }
      else {
        Log.e(TAG, "Directory already exists");
      }
    }

    // copy trained data if file doesn't exist
    if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
      try {
        AssetManager assetManager = cordova.getActivity().getApplicationContext().getAssets();
        InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
        OutputStream out = new FileOutputStream(DATA_PATH + "tessdata/" + lang + ".traineddata");

        byte[] buf = new byte[1024]; // 1024 bytes = 1 KB
        int len;

        while ((len = in.read(buf)) > 0) {
          out.write(buf, 0, len);
        }
        in.close();
        out.close();

        Log.e(TAG, "Copied " + lang + ".traineddata successfully");
      }
      catch (IOException e) {
        Log.e(TAG, "Unable to copy " + lang + ".traineddata: " + e.toString());
        callbackContext.error("Unable to copy " + lang + ".traineddata: " + e.toString());
        return;
      }
    }
    else
      Log.e(TAG, "Train data file already exists");

    Log.e(TAG, "Tesseract engine has been loaded");
    callbackContext.success("Tesseract engine has been loaded");
  }


  public Bitmap createBinaryImage( Bitmap bm ){
    int[] pixels = new int[bm.getWidth()*bm.getHeight()];
    bm.getPixels( pixels, 0, bm.getWidth(), 0, 0, bm.getWidth(), bm.getHeight() );
    int w = bm.getWidth();

    // Calculate overall lightness of image
    long gLightness = 0;
    int lLightness;
    int c;
    for ( int x = 0; x < bm.getWidth(); x++ ){
        for ( int y = 0; y < bm.getHeight(); y++ ){
            c = pixels[x+y*w];
            lLightness = ((c&0x00FF0000 )>>16) + ((c & 0x0000FF00 )>>8) + (c&0x000000FF);
            pixels[x+y*w] = lLightness;
            gLightness += lLightness;
        }
    }
    gLightness /= bm.getWidth() * bm.getHeight();
    gLightness = gLightness * 2 / 5;

    Bitmap bitmap = bm.copy(Bitmap.Config.ARGB_8888, true);


    int negros = 0;
    int blancos = 0;
    Log.e(TAG, "---------------Lightness----------------");
    for ( int x = 0; x < bm.getWidth(); x++ ){
      for ( int y = 0; y < bm.getHeight(); y++ ){
          if (pixels[x+y*w] <= gLightness){
            bitmap.setPixel(x,y, Color.BLACK);
            negros += 1;
          }else{
            bitmap.setPixel(x,y, Color.WHITE);
            blancos += 1;
          }
      }   
    }
    Log.e(TAG, "Negros----------------" + negros);
    Log.e(TAG, "Blancos----------------" + blancos);
    Log.e(TAG, "Total----------------" + (bm.getWidth()*bm.getHeight()));



    return bitmap;
  }

  // recognize the image using tesseract
  public void recognizeImage(String imageURL, final CallbackContext callbackContext) {
    BitmapFactory.Options options = new BitmapFactory.Options();

    Log.e(TAG, imageURL);
    byte[] decodedBytes = Base64.decode(imageURL, Base64.DEFAULT);
    Log.e(TAG, "Starting image decoding...");

      Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, options);
      int imageHeight = options.outHeight;
      int imageWidth = options.outWidth;
      Log.e(TAG, "Dimension de la imagen width "+ imageWidth+ ", height "+ imageHeight);

      try {
        //bitmap = createBinaryImage(bitmap);
        bitmap = Bitmap.createScaledBitmap(bitmap, imageWidth*2, imageHeight*2, false);

      // scan and recognize the bitmap image
      String recognizedText = "";

      Log.e(TAG, "Before baseApi");
      TessBaseAPI baseApi = new TessBaseAPI();
      baseApi.setDebug(true);
      //baseApi.setPageSegMode(1);
      baseApi.init(DATA_PATH, lang);
      baseApi.setVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ(),.-+auroilnsrmtv");
      baseApi.setImage(bitmap);
      recognizedText = baseApi.getUTF8Text();
      //Log.e(TAG,"OCR wordConfidences" + Arrays.toString(baseApi.wordConfidences().getGeometry()));
      Log.e(TAG,"OCR meanConfidence" + baseApi.meanConfidence());
      Log.e(TAG, "OCRED Text: " + recognizedText);

      Pixa words = baseApi.getWords();
      int num_words = words.size();
      int[] word_confidences = baseApi.wordConfidences();

      for(int i =0; i < num_words; i++){
        int[] b = words.getBoxRect(i);
        Log.e(TAG, "_________");
        Log.e(TAG, "Word "+ i + ", x: " + b[0] + ", y: " + b[1] + ",  w: " + b[2] +", h: "+ b[3]);
        Log.e(TAG, "confidence: " + word_confidences[i]);
      }


      /*
      int total_width = baseApi.getRegions().getBox(0).getWidth();
      int blocks_y = baseApi.getTextlines().getBox(2).getY();
      int blocks_height = 0;
      if (baseApi.getTextlines().size()>=11){
        blocks_height = baseApi.getTextlines().getBox(11).getY();
      } else {
        blocks_height = baseApi.getTextlines().getBox(baseApi.getTextlines().size() - 6).getY();
      }
      baseApi.setVariable("tessedit_char_whitelist", "0123456789+.");
      baseApi.setRectangle(0, blocks_y, total_width, blocks_height);
      recognizedBlocks = baseApi.getUTF8Text();
      Log.e(TAG, "OCRED Blocks!: " + recognizedBlocks);
      */
      /*
      int total_width = baseApi.getRegions().getBox(0).getWidth();
      int total_height = baseApi.getRegions().getBox(0).getHeight();
      baseApi.setVariable("tessedit_char_whitelist", "0123456789()EuroMilnsLaPmtvAUST");
      baseApi.setRectangle(0, 0, total_width, (int)(total_height*0.3));
      recognizedTitle = baseApi.getUTF8Text();
      Log.e(TAG, "OCRED Title: " + recognizedTitle);

      baseApi.setVariable("tessedit_char_whitelist", "0123456789+.");
      baseApi.setRectangle(0, (int)(total_height*0.3), total_width, (int)(total_height*0.4));
      recognizedBlocks = baseApi.getUTF8Text();
      Log.e(TAG, "OCRED Blcoks!: " + recognizedBlocks);

      baseApi.setVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ,-a");
      baseApi.setRectangle(0, (int)(total_height*0.3), total_width, (int)(total_height*0.7));
      recognizedNumbers = baseApi.getUTF8Text();
      Log.e(TAG, "OCRED Numberssss: " + recognizedNumbers);
      */


          //Log.e(TAG,"OCR box rect regiones" + Arrays.toString(baseApi.getRegions().getBoxRects()));

      //Log.e(TAG,"OCR box rect lines" + Arrays.toString(baseApi.getTextlines().getBoxRects().getGeometry()));
      //Log.e(TAG,"OCR box rect words" + Arrays.toString(baseApi.getWords().getBoxRects().getGeometry()));
      //Log.e(TAG,"OCR box rect strips" + Arrays.toString(baseApi.getStrips().getBoxRects().getGeometry()));
      baseApi.end();



      callbackContext.success(recognizedText);
      Log.e(TAG, "Scanning completed");
      return;
    }
    catch (Exception ex) {
      Log.e(TAG,ex.getMessage());
      callbackContext.error(ex.getMessage());
      return;
    }
  }
}