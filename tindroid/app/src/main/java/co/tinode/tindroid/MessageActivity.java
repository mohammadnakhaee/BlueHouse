package co.tinode.tindroid;

import android.app.DownloadManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;

import com.google.firebase.messaging.RemoteMessage;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.util.Timer;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.preference.PreferenceManager;

import co.tinode.tindroid.account.Utils;
import co.tinode.tindroid.db.BaseDb;
import co.tinode.tindroid.media.VxCard;
import co.tinode.tinodesdk.ComTopic;
import co.tinode.tinodesdk.NotConnectedException;
import co.tinode.tinodesdk.PromisedReply;
import co.tinode.tinodesdk.ServerResponseException;
import co.tinode.tinodesdk.Tinode;
import co.tinode.tinodesdk.Topic;
import co.tinode.tinodesdk.model.Description;
import co.tinode.tinodesdk.model.Drafty;
import co.tinode.tinodesdk.model.MsgServerData;
import co.tinode.tinodesdk.model.MsgServerInfo;
import co.tinode.tinodesdk.model.MsgServerPres;
import co.tinode.tinodesdk.model.PrivateType;
import co.tinode.tinodesdk.model.ServerMessage;
import co.tinode.tinodesdk.model.Subscription;

/**
 * View to display a single conversation
 */
public class MessageActivity extends AppCompatActivity {
    private static final String TAG = "MessageActivity";

    static final String FRAGMENT_MESSAGES = "msg";
    static final String FRAGMENT_INVALID = "invalid";
    static final String FRAGMENT_INFO = "info";
    static final String FRAGMENT_payments = "payments";
    static final String FRAGMENT_mypayments = "mypayments";
    static final String FRAGMENT_PERMISSIONS = "permissions";
    static final String FRAGMENT_EDIT_MEMBERS = "edit_members";
    static final String FRAGMENT_VIEW_IMAGE = "view_image";
    static final String FRAGMENT_FILE_PREVIEW = "file_preview";

    private static final int MESSAGES_TO_LOAD = 24;

    private static final int READ_DELAY = 1000;

    // How long a typing indicator should play its animation, milliseconds.
    private static final int TYPING_INDICATOR_DURATION = 4000;

    private BroadcastReceiver onNotificationClick = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            // FIXME: handle notification click.
            Log.d(TAG, "onNotificationClick" + intent.getExtras());
        }
    };

    private Timer mTypingAnimationTimer;
    private String mMessageText = null;
    private PausableSingleThreadExecutor mMessageSender = null;

    private String mTopicName = null;
    private ComTopic<VxCard> mTopic = null;

    private MessageEventListener mTinodeListener;

    // Handler for sending {note what="read"} notifications after a READ_DELAY.
    private Handler mNoteReadHandler = null;

    // Notification settings.
    private boolean mSendTypingNotifications = false;
    private boolean mSendReadReceipts = false;

    BroadcastReceiver onDownloadComplete = new BroadcastReceiver() {
        public void onReceive(Context ctx, Intent intent) {
            DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (dm == null || downloadId == -1) {
                return;
            }

            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = dm.query(query);
            if (c.moveToFirst()) {
                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));
                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    URI fileUri = URI.create(c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI)));
                    String mimeType = c.getString(c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
                    intent = new Intent();
                    intent.setAction(android.content.Intent.ACTION_VIEW);
                    intent.setDataAndType(FileProvider.getUriForFile(MessageActivity.this,
                            "co.tinode.tindroid.provider", new File(fileUri)), mimeType);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException ignored) {
                        startActivity(new Intent(DownloadManager.ACTION_VIEW_DOWNLOADS));
                    }
                } else if (DownloadManager.STATUS_FAILED == status) {
                    int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                    Log.w(TAG, "Download failed. Reason: " + reason);
                }
            }
            c.close();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_messages);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
            ab.setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isFragmentVisible(FRAGMENT_MESSAGES) || isFragmentVisible(FRAGMENT_INVALID)) {
                    Intent intent = new Intent(MessageActivity.this, ChatsActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                } else {
                    getSupportFragmentManager().popBackStack();
                }
            }
        });

        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(onNotificationClick, new IntentFilter(DownloadManager.ACTION_NOTIFICATION_CLICKED));

        mMessageSender = new PausableSingleThreadExecutor();
        mMessageSender.pause();

        mNoteReadHandler = new NoteHandler(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        setIntent(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        final Intent intent = getIntent();

        final Tinode tinode = Cache.getTinode();
        mTinodeListener = new MessageEventListener(tinode.isConnected());
        tinode.addListener(mTinodeListener);

        // Get the topic name from intent, internal or external.
        String topicName = readTopicNameFromIntent(intent);
        if (!changeTopic(topicName)) {
            finish();
            return;
        }

        mMessageText = intent.getStringExtra(Intent.EXTRA_TEXT);
        Uri attachment = intent.getData();
        String type = intent.getType();
        if (attachment != null && type != null) {
            // Need to retain access right to the given Uri.
            Bundle args  = new Bundle();
            args.putParcelable(AttachmentHandler.ARG_SRC_URI, attachment);
            args.putString(AttachmentHandler.ARG_MIME_TYPE, type);
            if (type.startsWith("image/")) {
                args.putString(AttachmentHandler.ARG_IMAGE_CAPTION, mMessageText);
                showFragment(FRAGMENT_VIEW_IMAGE, args, true);
            } else {
                showFragment(FRAGMENT_FILE_PREVIEW, args, true);
            }
        }

        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);

        mSendReadReceipts = pref.getBoolean(UiUtils.PREF_READ_RCPT, true);
        mSendTypingNotifications = pref.getBoolean(UiUtils.PREF_TYPING_NOTIF, true);
    }

    // Topic has changed. Update all the views with the new data.
    private boolean changeTopic(String topicName) {
        final Tinode tinode = Cache.getTinode();

        if (TextUtils.isEmpty(topicName)) {
            Log.w(TAG, "Activity resumed with an empty topic name");
            return false;
        }

        ComTopic<VxCard> topic;
        try {
            //noinspection unchecked
            topic = (ComTopic<VxCard>) tinode.getTopic(topicName);
        } catch (ClassCastException ex) {
            Log.w(TAG, "Activity resumed with non-comm topic");
            return false;
        }

        // Cancel all pending notifications addressed to the current topic.
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (nm != null) {
            nm.cancel(mTopicName, 0);
        }

        mTopicName = topicName;
        mTopic = topic;

        if (mTopic == null) {
            UiUtils.setupToolbar(this, null, mTopicName, false, null);
            try {
                //noinspection unchecked
                mTopic = (ComTopic<VxCard>) tinode.newTopic(mTopicName, null);
            } catch (ClassCastException ex) {
                Log.w(TAG, "The unknown topic is a non-comm topic: " + mTopicName);
                return false;
            }
            showFragment(FRAGMENT_INVALID, null, false);

        } else {
            UiUtils.setupToolbar(this, mTopic.getPub(), mTopicName, mTopic.getOnline(), mTopic.getLastSeen());
            // Check if another fragment is already visible. If so, don't change it.
            if (UiUtils.getVisibleFragment(getSupportFragmentManager()) == null) {
                // No fragment is visible. Show default and clear back stack.
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                showFragment(FRAGMENT_MESSAGES, null, false);
            }
        }

        mTopic.setListener(new TListener());

        if (!mTopic.isAttached()) {
            // Try immediate reconnect.
            topicAttach(true);
        } else {
            MessagesFragment fragmsg = (MessagesFragment) getSupportFragmentManager()
                    .findFragmentByTag(FRAGMENT_MESSAGES);
            if (fragmsg != null) {
                fragmsg.topicSubscribed();
            }
        }

        return true;
    }

    // Get topic name from Intent the Activity was launched with (push notification, other app, other activity).
    private String readTopicNameFromIntent(Intent intent) {
        // Check if the activity was launched by internally-generated intent.
        String name = intent.getStringExtra("topic");
        if (!TextUtils.isEmpty(name)) {
            return name;
        }

        // Check if activity was launched from a background push notification.
        RemoteMessage msg = intent.getParcelableExtra("msg");
        if (msg != null) {
            RemoteMessage.Notification notification = msg.getNotification();
            if (notification != null) {
                return notification.getTag();
            }
        }

        if (TextUtils.isEmpty(name)) {
            // mTopicName is empty, so this is an external intent
            Uri contactUri = intent.getData();
            if (contactUri != null) {
                Cursor cursor = getContentResolver().query(contactUri,
                        new String[]{Utils.DATA_PID}, null, null, null);
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        name = cursor.getString(cursor.getColumnIndex(Utils.DATA_PID));
                    }
                    cursor.close();
                }
            }
        }

        return name;
    }

    @Override
    public void onPause() {
        super.onPause();

        Cache.getTinode().removeListener(mTinodeListener);

        topicDetach();

        // Stop handling read messages
        mNoteReadHandler.removeMessages(0);
    }

    private void topicAttach(boolean interactive) {
        setRefreshing(true);

        Tinode tinode = Cache.getTinode();
        if (!tinode.isAuthenticated()) {
            // If connection is not ready, wait for completion. This method will be called again
            // from the onLogin callback;
            Cache.getTinode().reconnectNow(interactive, false, false);
            return;
        }

        Topic.MetaGetBuilder builder = mTopic.getMetaGetBuilder()
                .withDesc()
                .withSub()
                .withLaterData(MESSAGES_TO_LOAD)
                .withDel();

        if (mTopic.isOwner()) {
            builder = builder.withTags();
        }

        mTopic.subscribe(null, builder.build())
                .thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                    @Override
                    public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                        UiUtils.setupToolbar(MessageActivity.this, mTopic.getPub(),
                                mTopicName, mTopic.getOnline(), mTopic.getLastSeen());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                FragmentManager fm = getSupportFragmentManager();
                                Fragment visible = UiUtils.getVisibleFragment(fm);
                                if (visible instanceof InvalidTopicFragment) {
                                    // Replace InvalidTopicFragment with default FRAGMENT_MESSAGES.
                                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                                    showFragment(FRAGMENT_MESSAGES, null, false);
                                } else {
                                    MessagesFragment fragmsg = (MessagesFragment) fm.findFragmentByTag(FRAGMENT_MESSAGES);
                                    if (fragmsg != null) {
                                        fragmsg.topicSubscribed();
                                    }
                                }
                            }
                        });
                        // Resume message sender and submit pending messages for processing:
                        // publish queued, delete marked for deletion.
                        mMessageSender.resume();
                        syncAllMessages(true);
                        return null;
                    }
                }).thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                    @Override
                    public PromisedReply<ServerMessage> onFailure(Exception err) {
                        if (!(err instanceof NotConnectedException)) {
                            Log.w(TAG, "Subscribe failed", err);
                            if (err instanceof ServerResponseException) {
                                int code = ((ServerResponseException) err).getCode();
                                if (code == 404) {
                                    showFragment(FRAGMENT_INVALID, null, false);
                                }
                            }
                        }
                        return null;
                    }
                }).thenFinally(new PromisedReply.FinalListener() {
                    @Override
                    public void onFinally() {
                        setRefreshing(false);
                    }
                });
    }

    // Clean up everything related to the topic being replaced of removed.
    private void topicDetach() {
        mMessageSender.pause();
        if (mTypingAnimationTimer != null) {
            mTypingAnimationTimer.cancel();
            mTypingAnimationTimer = null;
        }

        if (mTopic != null) {
            mTopic.setListener(null);

            // Deactivate current topic
            if (mTopic.isAttached()) {
                mTopic.leave();
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mMessageSender.shutdownNow();
        unregisterReceiver(onDownloadComplete);
        unregisterReceiver(onNotificationClick);
    }


    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        UiUtils.setVisibleTopic(hasFocus ? mTopicName : null);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (mTopic == null || !mTopic.isValid()) {
            return false;
        }

        int id = item.getItemId();
        switch (id) {
            case R.id.action_view_contact:
                showFragment(FRAGMENT_INFO, null, true);
                return true;

            case R.id.action_view_payments:
                showFragment(FRAGMENT_payments, null, true);
                return true;

            case R.id.action_my_payments:
                showFragment(FRAGMENT_mypayments, null, true);
                return true;

            case R.id.action_archive:
                if (mTopic != null) {
                    mTopic.updateArchived(true);
                }
                return true;

            case R.id.action_unarchive:
                if (mTopic != null) {
                    mTopic.updateArchived(false);
                }
                return true;

            default:
                return false;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Try to send all pending messages.
    public void syncAllMessages(final boolean runLoader) {
        syncMessages(-1, runLoader);
    }

    // Try to send the specified message.
    public void syncMessages(final long msgId, final boolean runLoader) {
        mMessageSender.submit(new Runnable() {
            @Override
            public void run() {
                PromisedReply<ServerMessage> promise;
                if (msgId > 0) {
                    promise = mTopic.syncOne(msgId);
                } else {
                    promise = mTopic.syncAll();
                }
                if (runLoader) {
                    promise.thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                        @Override
                        public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                            runMessagesLoader();
                            return null;
                        }
                    });
                }
                promise.thenCatch(new PromisedReply.FailureListener<ServerMessage>() {
                    @Override
                    public PromisedReply<ServerMessage> onFailure(Exception err) {
                        Log.w(TAG, "Sync failed", err);
                        return null;
                    }
                });
            }
        });
    }

    private boolean isFragmentVisible(String tag) {
        Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
        return fragment != null && fragment.isVisible();
    }

    void showFragment(String tag, Bundle args, boolean addToBackstack) {
        if (isFinishing() || isDestroyed()) {
            return;
        }

        FragmentManager fm = getSupportFragmentManager();

        Fragment fragment = fm.findFragmentByTag(tag);
        if (fragment == null) {
            switch (tag) {
                case FRAGMENT_MESSAGES:
                    fragment = new MessagesFragment();
                    break;
                case FRAGMENT_INFO:
                    fragment = new TopicInfoFragment();
                    break;
                case FRAGMENT_mypayments:
                    fragment = new MyPaymentsFragment();
                    break;
                case FRAGMENT_payments:
                    fragment = new PaymentsFragment();
                    break;
                case FRAGMENT_PERMISSIONS:
                    fragment = new TopicPermissionsFragment();
                    break;
                case FRAGMENT_EDIT_MEMBERS:
                    fragment = new EditMembersFragment();
                    break;
                case FRAGMENT_VIEW_IMAGE:
                    fragment = new ImageViewFragment();
                    break;
                case FRAGMENT_FILE_PREVIEW:
                    fragment = new FilePreviewFragment();
                    break;
                case FRAGMENT_INVALID:
                    fragment = new InvalidTopicFragment();
                    break;
            }
        } else if (args == null) {
            // Retain old arguments.
            args = fragment.getArguments();
        }
        if (fragment == null) {
            throw new NullPointerException();
        }

        args = args != null ? args : new Bundle();
        args.putString("topic", mTopicName);
        args.putString(MessagesFragment.MESSAGE_TO_SEND, mMessageText);
        if (fragment.getArguments() != null) {
            fragment.getArguments().putAll(args);
        } else {
            fragment.setArguments(args);
        }

        FragmentTransaction trx = fm.beginTransaction();
        if (!fragment.isAdded()) {
            trx = trx.replace(R.id.contentFragment, fragment, tag)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        } else if (!fragment.isVisible()) {
            trx = trx.show(fragment);
        } else {
            addToBackstack = false;
        }

        if (FRAGMENT_MESSAGES.equals(tag)) {
            trx.setPrimaryNavigationFragment(fragment);
        }

        if (addToBackstack) {
            trx.addToBackStack(tag);
        }
        if (!trx.isEmpty()) {
            trx.commit();
        }
    }

    boolean sendMessage(Drafty content) {
        if (mTopic != null) {
            PromisedReply<ServerMessage> reply = mTopic.publish(content);
            BaseDb.getInstance().getStore().msgPruneFailed(mTopic);
            runMessagesLoader(); // Refreshes the messages: hides removed, shows pending.
            reply
                    .thenApply(new PromisedReply.SuccessListener<ServerMessage>() {
                        @Override
                        public PromisedReply<ServerMessage> onSuccess(ServerMessage result) {
                            if (mTopic.isArchived()) {
                                mTopic.updateArchived(false);
                            }
                            return null;
                        }
                    })
                    .thenCatch(new UiUtils.ToastFailureListener(this))
                    .thenFinally(new PromisedReply.FinalListener() {
                        @Override
                        public void onFinally() {
                            // Updates message list with "delivered" or "failed" icon.
                            runMessagesLoader();
                        }
                    });
            return true;
        }
        return false;
    }

    public void sendKeyPress() {
        if (mTopic != null && mSendTypingNotifications) {
            mTopic.noteKeyPress();
        }
    }

    void runMessagesLoader() {
        final MessagesFragment fragment = (MessagesFragment) getSupportFragmentManager().
                findFragmentByTag(FRAGMENT_MESSAGES);
        if (fragment != null && fragment.isVisible()) {
            fragment.runMessagesLoader(mTopicName);
        }
    }

    public void submitForExecution(Runnable runnable) {
        mMessageSender.submit(runnable);
    }

    /**
     * Show progress indicator based on current status
     *
     * @param active should be true to show progress indicator
     */
    public void setRefreshing(final boolean active) {
        if (isFinishing() || isDestroyed()) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                MessagesFragment fragMsg = (MessagesFragment) getSupportFragmentManager()
                        .findFragmentByTag(FRAGMENT_MESSAGES);
                if (fragMsg != null) {
                    fragMsg.setRefreshing(active);
                }
            }
        });
    }

    // Schedule a delayed {note what="read"} notification.
    void sendNoteRead(int seq) {
        if (mSendReadReceipts) {
            Message msg = mNoteReadHandler.obtainMessage(0, seq, 0, mTopicName);
            mNoteReadHandler.sendMessageDelayed(msg, READ_DELAY);
        }
    }

    // Handler which sends "read" notifications for received messages.
    private static class NoteHandler extends Handler {
        WeakReference<MessageActivity> ref;
        NoteHandler(MessageActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            MessageActivity activity = ref.get();
            if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
                return;
            }

            if (activity.mTopic == null) {
                return;
            }

            // If messages fragment is not visible don't send the notification.
            if (!activity.isFragmentVisible(FRAGMENT_MESSAGES)) {
                return;
            }

            // Don't send a notification if more notifications are pending. This avoids the case of acking
            // every {data} message in a large batch.
            // It may pose a problem if a later message is acked first (msg[1].seq > msg[2].seq), but that
            // should not happen.
            if (hasMessages(0)) {
                return;
            }

            String topicName = (String) msg.obj;
            if (topicName.equals(activity.mTopic.getName())) {
                activity.mTopic.noteRead(msg.arg1);
            }
        }
    }

    /**
     * Utility class to send messages queued while offline.
     * The execution is paused while the activity is in background and unpaused
     * when the topic subscription is live.
     */
    private static class PausableSingleThreadExecutor extends ThreadPoolExecutor {
        private boolean isPaused;
        private ReentrantLock pauseLock = new ReentrantLock();
        private Condition unpaused = pauseLock.newCondition();

        PausableSingleThreadExecutor() {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        }

        @Override
        protected void beforeExecute(Thread t, Runnable r) {
            super.beforeExecute(t, r);
            pauseLock.lock();
            try {
                while (isPaused) unpaused.await();
            } catch (InterruptedException ie) {
                t.interrupt();
            } finally {
                pauseLock.unlock();
            }
        }

        void pause() {
            pauseLock.lock();
            try {
                isPaused = true;
            } finally {
                pauseLock.unlock();
            }
        }

        void resume() {
            pauseLock.lock();
            try {
                isPaused = false;
                unpaused.signalAll();
            } finally {
                pauseLock.unlock();
            }
        }
    }

    private class TListener extends ComTopic.ComListener<VxCard> {

        TListener() {
        }

        @Override
        public void onSubscribe(int code, String text) {
            // Topic name may change after subscription, i.e. new -> grpXXX
            mTopicName = mTopic.getName();
        }

        @Override
        public void onData(MsgServerData data) {
            // Don't send a notification for own messages. They are read by default.
            if (data != null && !Cache.getTinode().isMe(data.from)) {
                sendNoteRead(data.seq);
            }

            runMessagesLoader();
        }

        @Override
        public void onPres(MsgServerPres pres) {
            // noinspection SwitchStatementWithTooFewBranches
            switch (pres.what) {
                case "acs":
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Fragment fragment = UiUtils.getVisibleFragment(getSupportFragmentManager());
                            if (fragment != null) {
                                if (fragment instanceof TopicInfoFragment) {
                                    ((TopicInfoFragment) fragment).notifyDataSetChanged();
                                } else if (fragment instanceof TopicPermissionsFragment) {
                                    ((TopicPermissionsFragment) fragment).notifyDataSetChanged();
                                } else if (fragment instanceof MessagesFragment) {
                                    ((MessagesFragment) fragment).notifyDataSetChanged(true);
                                }
                            }
                        }
                    });
                    break;
                default:
                    Log.d(TAG, "Topic '" + mTopicName + "' onPres what='" + pres.what + "' (unhandled)");
            }

        }

        @Override
        public void onInfo(MsgServerInfo info) {
            switch (info.what) {
                case "read":
                case "recv":
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MessagesFragment fragment = (MessagesFragment) getSupportFragmentManager().
                                    findFragmentByTag(FRAGMENT_MESSAGES);
                            if (fragment != null && fragment.isVisible()) {
                                fragment.notifyDataSetChanged(false);
                            }
                        }
                    });
                    break;
                case "kp":
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            // Show typing indicator as animation over avatar in toolbar
                            mTypingAnimationTimer = UiUtils.toolbarTypingIndicator(MessageActivity.this,
                                    mTypingAnimationTimer, TYPING_INDICATOR_DURATION);
                        }
                    });
                    break;
                default:
                    break;
            }
        }

        @Override
        public void onSubsUpdated() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Fragment fragment = UiUtils.getVisibleFragment(getSupportFragmentManager());
                    if (fragment != null) {
                        if (fragment instanceof TopicInfoFragment) {
                            ((TopicInfoFragment) fragment).notifyDataSetChanged();
                        } else if (fragment instanceof MessagesFragment) {
                            ((MessagesFragment) fragment).notifyDataSetChanged(true);
                        }
                    }
                }
            });
        }

        @Override
        public void onMetaDesc(final Description<VxCard, PrivateType> desc) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UiUtils.setupToolbar(MessageActivity.this, mTopic.getPub(), mTopic.getName(),
                            mTopic.getOnline(), mTopic.getLastSeen());
                    Fragment fragment = UiUtils.getVisibleFragment(getSupportFragmentManager());
                    if (fragment != null) {
                        if (fragment instanceof TopicInfoFragment) {
                            ((TopicInfoFragment) fragment).notifyDataSetChanged();
                        } else if (fragment instanceof MessagesFragment) {
                            ((MessagesFragment) fragment).notifyDataSetChanged(true);
                        }
                    }
                }
            });
        }

        @Override
        public void onContUpdate(final Subscription<VxCard, PrivateType> sub) {
            onMetaDesc(null);
        }

        @Override
        public void onMetaTags(String[] tags) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Fragment fragment = UiUtils.getVisibleFragment(getSupportFragmentManager());
                    if (fragment instanceof TopicInfoFragment) {
                        ((TopicInfoFragment) fragment).notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onOnline(final boolean online) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    UiUtils.toolbarSetOnline(MessageActivity.this,
                            mTopic.getOnline(), mTopic.getLastSeen());
                }
            });

        }
    }

    private class MessageEventListener extends UiUtils.EventListener {
        MessageEventListener(boolean online) {
            super(MessageActivity.this, online);
        }

        @Override
        public void onLogin(int code, String txt) {
            super.onLogin(code, txt);

            UiUtils.attachMeTopic(MessageActivity.this, null);
            topicAttach(false);
        }
    }
}