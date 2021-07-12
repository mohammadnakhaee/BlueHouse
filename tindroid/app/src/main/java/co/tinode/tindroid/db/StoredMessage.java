package co.tinode.tindroid.db;

import android.database.Cursor;

import java.util.Date;
import java.util.Map;

import co.tinode.tinodesdk.Storage;
import co.tinode.tinodesdk.model.Drafty;
import co.tinode.tinodesdk.model.MsgRange;
import co.tinode.tinodesdk.model.MsgServerData;

/**
 * StoredMessage fetched from the database
 */
public class StoredMessage extends MsgServerData implements Storage.Message {
    public long id;
    public long topicId;
    public long userId;
    public BaseDb.Status status;
    public int delId;
    public int high;


    StoredMessage() {
    }

    StoredMessage(MsgServerData m) {
        topic = m.topic;
        head = m.head;
        from = m.from;
        ts = m.ts;
        seq = m.seq;
        content = m.content;
    }

    public StoredMessage(MsgServerData m, BaseDb.Status status) {
        this(m);
        this.status = status;
    }

    public static StoredMessage readMessage(Cursor c) {
        StoredMessage msg = new StoredMessage();

        msg.id = c.getLong(MessageDb.COLUMN_IDX_ID);
        msg.topicId = c.getLong(MessageDb.COLUMN_IDX_TOPIC_ID);
        msg.userId = c.getLong(MessageDb.COLUMN_IDX_USER_ID);
        msg.status = BaseDb.Status.fromInt(c.getInt(MessageDb.COLUMN_IDX_STATUS));
        msg.from = c.getString(MessageDb.COLUMN_IDX_SENDER);
        msg.ts = new Date(c.getLong(MessageDb.COLUMN_IDX_TS));
        msg.seq = c.getInt(MessageDb.COLUMN_IDX_SEQ);
        msg.high = c.isNull(MessageDb.COLUMN_IDX_HIGH) ? 0 : c.getInt(MessageDb.COLUMN_IDX_HIGH);
        msg.delId = c.isNull(MessageDb.COLUMN_IDX_DEL_ID) ? 0 : c.getInt(MessageDb.COLUMN_IDX_DEL_ID);
        msg.head = BaseDb.deserialize(c.getString(MessageDb.COLUMN_IDX_HEAD));
        msg.content = BaseDb.deserialize(c.getString(MessageDb.COLUMN_IDX_CONTENT));

        return msg;
    }

    static MsgRange readDelRange(Cursor c) {
        // 0: delId, 1: seq, 2: high
        return new MsgRange(c.getInt(1), c.getInt(2));
    }

    public boolean isMine() {
        return BaseDb.isMe(from);
    }

    @Override
    public Drafty getContent() {
        return content;
    }

    @Override
    public Map<String, Object> getHead() { return head; }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public int getSeqId() {
        return seq;
    }

    @Override
    public boolean isDraft() {
        return status == BaseDb.Status.DRAFT;
    }

    @Override
    public boolean isReady() {
        return status == BaseDb.Status.QUEUED;
    }

    @Override
    public boolean isDeleted() {
        return status == BaseDb.Status.DELETED_SOFT || status == BaseDb.Status.DELETED_HARD;
    }

    @Override
    public boolean isDeleted(boolean hard) {
        return hard ? status == BaseDb.Status.DELETED_HARD : status == BaseDb.Status.DELETED_SOFT;
    }

    @Override
    public boolean isSynced() {
        return status == BaseDb.Status.SYNCED;
    }
}
