package jp.techacademy.yoshie.sekiguchi.autoslideshowapp;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;

import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import android.os.Handler;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    //permission取得した場合に返す用
    private static final int PERMISSIONS_REQUEST_CODE = 100;

    //timer用
    Timer mTimer;
    ImageView mTimerView;
    double mTimerSec = 0.0;
    Handler mHandler = new Handler();

    //ギャラリーからデータ取得する用
    Cursor cursor;

    Button mStartButton;
    Button mBackButton;
    Button mNextButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Permissionを取得する
        // Android 6.0以降の場合
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // パーミッションの許可状態を確認する
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                // 許可されている場合、最初の画像を表示する
                getContentsInfo();
            } else {
                // 許可されていないので許可ダイアログを表示する
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_CODE);
            }
        } else {
            //Android 5系以下の場合、最初の画像を表示する
            getContentsInfo();
        }

        mTimerView = (ImageView) findViewById(R.id.imageView);
        mStartButton = (Button) findViewById(R.id.start_button);
        mBackButton = (Button) findViewById(R.id.back_button);
        mNextButton = (Button) findViewById(R.id.next_button);


        //スタートボタン
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mTimer == null) {   //mTimerがnull（初回起動）の場合のみ、タイマー作成する

                    //再生ボタンを押した場合（timer起動中）ボタンを非活性にする
                    mBackButton.setEnabled(false);
                    mNextButton.setEnabled(false);
                    mStartButton.setText("一時停止");

                    // タイマーの作成
                    mTimer = new Timer();
                    // タイマーの始動
                    mTimer.schedule(new TimerTask() {
                        @Override
                        public void run() {
                            mTimerSec += 2; //2秒timer
                            mHandler.post(new Runnable() {
                                @Override
                                public void run() {
                                    if (cursor.moveToNext()) {
                                        setImageView();
                                    } else {  //最初の画像に戻す
                                        cursor.moveToFirst();
                                        setImageView();
                                    }
                                }
                            });
                        }
                    }, 2000, 2000);    // 最初に始動させるまで 100ミリ秒、ループの間隔を 100ミリ秒 に設定
                } else {
                    //mTimerがnullでない（再生している）場合はTimerを止めてnullにする
                    if (mTimer != null) {
                        mTimer.cancel();
                        mTimer = null;
                    }
                    //一時停止ボタンを押した場合（timer起動中）ボタンを活性にする
                    mStartButton.setText("再生");
                    mBackButton.setEnabled(true);
                    mNextButton.setEnabled(true);
                }
            }
        });


        //戻るボタン
        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1枚前の画像を取得
                if (cursor.moveToPrevious()) {
                    setImageView();
                } else {  //最後の画像に戻す
                    cursor.moveToLast();
                    setImageView();
                }
            }
        });

        //進むボタン
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //1枚目の画像を取得
                if (cursor.moveToNext()) {
                    setImageView();
                } else {  //最初の画像に戻す
                    cursor.moveToFirst();
                    setImageView();
                }

            }
        });
    }


    //画像の情報を取得して、cursorに格納する
    private void getContentsInfo() {
        // 画像の情報を取得する
        ContentResolver resolver = getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // データの種類
                null, // 項目(null = 全項目)
                null, // フィルタ条件(null = フィルタなし)
                null, // フィルタ用パラメータ
                null // ソート (null ソートなし)
        );
        //1枚目の画像を取得
        if (cursor.moveToFirst()) {
            setImageView();
        } else {
            //画像が1枚も無い場合、dialogを表示してアプリを終了する
            new AlertDialog.Builder(this)
                    .setTitle("Confirm")
                    .setMessage("画像がありません。アプリを終了します")
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // OK button pressed
                            cursor.close();
                            finish();
                        }
                    })
                    .show();
        }
    }

    //ImageViewに画像を表示する
    private void setImageView() {
        int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
        Long id = cursor.getLong(fieldIndex);
        Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

        ImageView imageVIew = (ImageView) findViewById(R.id.imageView);
        imageVIew.setImageURI(imageUri);
    }


    //未許諾状態で許諾dialogで許諾OK/NG選択時の挙動
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_CODE:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("ANDROID", "許可された");
                    getContentsInfo();

                } else {
                    Log.d("ANDROID", "許可されなかった");

                    //許諾が得られなければapp終了する
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm")
                            .setMessage("アプリを終了します")
                            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // OK button pressed
                                    finish();
                                }
                            })
                            .show();
                }
                break;
            default:
                break;
        }
    }

    //App終了時にcursor.close()
    @Override
    protected void onDestroy() {
        Log.d("Android", "onDestroy");
        cursor.close();
        super.onDestroy();
    }

    //Androidホームに戻ったとき、再生を停止する
    @Override
    protected void onPause() {
        Log.d("Android", "onPause");
        cursor.close();
        super.onPause();
    }

}