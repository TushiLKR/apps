package com.example.tstftst;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.client.android.Intents;
import com.journeyapps.barcodescanner.Intents;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int ZXING_SCANNER_REQUEST = 2;

    private ImageView imageView;
    private Bitmap selectedBitmap;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            this::handleImageResult
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.imageView);
    }

    public void chooseImage(View view) {
        pickImageLauncher.launch("image/*");
    }

    public void scanQRCode(View view) {
        Intent intent = new Intent(this, CaptureActivity.class);
        startActivityForResult(intent, ZXING_SCANNER_REQUEST);
    }

    private void handleImageResult(Uri uri) {
        try {
            selectedBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
            imageView.setImageBitmap(selectedBitmap);
            generateQRCodeFromImage();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ZXING_SCANNER_REQUEST && resultCode == RESULT_OK && data != null) {
            String scannedData = data.getStringExtra(Intents.Scan.RESULT);
            generateQRCodeFromData(scannedData);
        }
    }

    private void generateQRCodeFromImage() {
        if (selectedBitmap != null) {
            try {
                String binaryData = convertBitmapToBinary(selectedBitmap);
                Bitmap qrCodeBitmap = generateQRCodeBitmap(binaryData);
                imageView.setImageBitmap(qrCodeBitmap);
            } catch (WriterException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to generate QR code from image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void generateQRCodeFromData(String data) {
        try {
            Bitmap qrCodeBitmap = generateQRCodeBitmap(data);
            imageView.setImageBitmap(qrCodeBitmap);
        } catch (WriterException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to generate QR code from scanned data", Toast.LENGTH_SHORT).show();
        }
    }

    private String convertBitmapToBinary(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        StringBuilder binary = new StringBuilder();
        for (byte b : byteArray) {
            int value = b;
            for (int i = 0; i < 8; i++) {
                binary.append((value & 128) == 0 ? 0 : 1);
                value <<= 1;
            }
        }
        return binary.toString();
    }

    private Bitmap generateQRCodeBitmap(String binaryData) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

        BitMatrix bitMatrix = new MultiFormatWriter().encode(binaryData, BarcodeFormat.QR_CODE, 500, 500, hints);
        int width = bitMatrix.getWidth();
        int height = bitMatrix.getHeight();
        Bitmap qrCodeBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                qrCodeBitmap.setPixel(x, y, bitMatrix.get(x, y) ? ContextCompat.getColor(getApplicationContext(), R.color.black) : ContextCompat.getColor(getApplicationContext(), R.color.white));
            }
        }

        return qrCodeBitmap;
    }
}
