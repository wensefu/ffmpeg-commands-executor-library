package com.xiachufang.ffmpegexecutor;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.xiachufang.utils.video.FFmpegMediaMetadataRetriever;
import com.xiachufang.utils.video.FFmpegNativeHelper;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    private EditText mInputEdit;
    private TextView mResultBoard;
    private String mCurrentCommand;
    private AsyncTask<String, Void, String> mRunCommandAsyncTask;

    private static final int EVENT_SHOW_PROCESS_DIALOG = 0;
    private static final int EVENT_DISMISS_PROCESS_DIALOG = 1;
    private static ProgressDialog mWaitingDialog;
    private WaitingDialogHanlder mWaitingDialogHandler;

//    private static final String testCommand = "ffmpeg -i /sdcard/demo.mp4 -vframes 30 -y -f gif /sdcard/demoout.gif";
//    private static final String testCommand = "ffmpeg -h";
//    private static final String testCommand = "ffmpeg -ss 0.0 -i /sdcard/demo.mp4 -t 15.0 -vf crop=300:300:0:400 /sdcard/demoout.mp4";
    private static final String testCommand = "ffmpeg -ss 0.0 -i /sdcard/demo.mp4 -t 4.84499979019165 -q:v 5 -s 544x434 -vf crop=544:434:0:0 -r 15 -c:v libx264 /sdcard/demoout.mp4";
//      private static final String testCommand = "ffmpeg -codecs";
    private StandardGSYVideoPlayer mVideoView;
    //    private static final String testCommand = "ffmpeg -i /sdcard/demo.mp4";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInputEdit = (EditText) findViewById(R.id.command_input_edit);
        mResultBoard = (TextView) findViewById(R.id.result_board);
        mResultBoard.setMovementMethod(new ScrollingMovementMethod());
        mVideoView = (StandardGSYVideoPlayer) findViewById(R.id.video_view);

        mRunCommandAsyncTask = null;

        mWaitingDialog = new ProgressDialog(this);
        mWaitingDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mWaitingDialog.setMessage("Waiting...");

        mWaitingDialogHandler = new WaitingDialogHanlder();


        findViewById(R.id.run_command_sync).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runCommandSync();
            }
        });

        findViewById(R.id.run_command_async).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                runCommandAsync();
            }
        });

    }

    private void testFFmpegMediaMetaDataRetriever() {
        FFmpegMediaMetadataRetriever mmr = new FFmpegMediaMetadataRetriever();
        mmr.setDataSource("/sdcard/demoout.mp4");
        Log.e("宽" , mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH));
        Log.e("高" , mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT));
        Log.e("旋转", mmr.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION));


        mmr.release();

        FFmpegMediaMetadataRetriever metadataRetriever = new FFmpegMediaMetadataRetriever();
        metadataRetriever.setDataSource("/sdcard/demoout.mp4");
        metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ALBUM);
        metadataRetriever.extractMetadata(FFmpegMediaMetadataRetriever.METADATA_KEY_ARTIST);
        Bitmap b = metadataRetriever.getFrameAtTime(2000000, FFmpegMediaMetadataRetriever.OPTION_CLOSEST); // frame at 2 seconds
        ImageView imageview = (ImageView) findViewById(R.id.imageviewsss);
        imageview.setImageBitmap(b);
        metadataRetriever.release();
    }

    private void prepare() {
        clearResultBoard();
        mCurrentCommand = mInputEdit.getText().toString();
        if (TextUtils.isEmpty(mCurrentCommand)) {
            mCurrentCommand = testCommand;
        }
        mRunCommandAsyncTask = null;
    }

    // 同步运行命令, 阻塞
    private void runCommandSync() {
        prepare();
        updateResultBoard(executeCommand(mCurrentCommand));
    }

    /* 异步运行命令, 非阻塞 */
    private void runCommandAsync() {
        prepare();
        mRunCommandAsyncTask = new AsyncTask<String, Void, String>() {
            @Override
            protected String doInBackground(String... strings) {
                String command;
                if (strings != null && strings.length > 0) {
                    command = strings[0];
                } else {
                    return "Must input command!";
                }

                return executeCommand(command);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                // we got result, dismiss waiting dialog
                mWaitingDialogHandler.removeMessages(EVENT_SHOW_PROCESS_DIALOG);
                mWaitingDialogHandler.sendEmptyMessage(EVENT_DISMISS_PROCESS_DIALOG);
                updateResultBoard(s);
            }
        };

        mRunCommandAsyncTask.execute(mCurrentCommand);

        // if we can`t get result after 300ms, show the waiting dialog
        mWaitingDialogHandler.sendEmptyMessageDelayed(EVENT_SHOW_PROCESS_DIALOG, 300);

    }

    private String executeCommand(String command) {
        File file = new File("/sdcard/demoout.mp4");
        if (file.exists()) {
            file.delete();
        }
        return FFmpegNativeHelper.runCommand(command);
    }

    private void clearResultBoard() {
        mResultBoard.setText("");
        mResultBoard.scrollTo(0, 0);
    }

    private void updateResultBoard(String message) {
        if (message == null || message.length() == 0) {
            return;
        }
        String currentStr = mResultBoard.getText().toString();
        mResultBoard.setText(currentStr + "\n" + message);

        testFFmpegMediaMetaDataRetriever();

        mVideoView.setUp("/sdcard/demoout.mp4", true, null, "/sdcard/demoout.mp4");
        mVideoView.startPlayLogic();
    }

    private static class WaitingDialogHanlder extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case EVENT_SHOW_PROCESS_DIALOG:
                    mWaitingDialog.show();
                    break;
                case EVENT_DISMISS_PROCESS_DIALOG:
                    mWaitingDialog.dismiss();
                    break;
                default: return;
            }
        }
    }
}
