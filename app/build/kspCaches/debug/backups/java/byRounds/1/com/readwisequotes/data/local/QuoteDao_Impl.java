package com.readwisequotes.data.local;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.readwisequotes.data.model.Quote;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class QuoteDao_Impl implements QuoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Quote> __insertionAdapterOfQuote;

  private final Converters __converters = new Converters();

  private final SharedSQLiteStatement __preparedStmtOfDeleteAll;

  public QuoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfQuote = new EntityInsertionAdapter<Quote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `quotes` (`id`,`text`,`title`,`author`,`bookCover`,`tags`,`isFavorite`,`updatedAt`,`sourceType`,`bookId`) VALUES (?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Quote entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getText());
        if (entity.getTitle() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getTitle());
        }
        if (entity.getAuthor() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getAuthor());
        }
        if (entity.getBookCover() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getBookCover());
        }
        final String _tmp = __converters.fromStringList(entity.getTags());
        statement.bindString(6, _tmp);
        final int _tmp_1 = entity.isFavorite() ? 1 : 0;
        statement.bindLong(7, _tmp_1);
        statement.bindString(8, entity.getUpdatedAt());
        if (entity.getSourceType() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getSourceType());
        }
        if (entity.getBookId() == null) {
          statement.bindNull(10);
        } else {
          statement.bindLong(10, entity.getBookId());
        }
      }
    };
    this.__preparedStmtOfDeleteAll = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM quotes";
        return _query;
      }
    };
  }

  @Override
  public Object insertAll(final List<Quote> quotes, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfQuote.insert(quotes);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteAll(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteAll.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteAll.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Quote>> getAllQuotes() {
    final String _sql = "SELECT * FROM quotes ORDER BY RANDOM()";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quotes"}, new Callable<List<Quote>>() {
      @Override
      @NonNull
      public List<Quote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfBookCover = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCover");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSourceType = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceType");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final List<Quote> _result = new ArrayList<Quote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Quote _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final String _tmpBookCover;
            if (_cursor.isNull(_cursorIndexOfBookCover)) {
              _tmpBookCover = null;
            } else {
              _tmpBookCover = _cursor.getString(_cursorIndexOfBookCover);
            }
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toStringList(_tmp);
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpSourceType;
            if (_cursor.isNull(_cursorIndexOfSourceType)) {
              _tmpSourceType = null;
            } else {
              _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
            }
            final Long _tmpBookId;
            if (_cursor.isNull(_cursorIndexOfBookId)) {
              _tmpBookId = null;
            } else {
              _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            }
            _item = new Quote(_tmpId,_tmpText,_tmpTitle,_tmpAuthor,_tmpBookCover,_tmpTags,_tmpIsFavorite,_tmpUpdatedAt,_tmpSourceType,_tmpBookId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Quote>> getFavoriteQuotes() {
    final String _sql = "SELECT * FROM quotes WHERE isFavorite = 1 ORDER BY RANDOM()";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quotes"}, new Callable<List<Quote>>() {
      @Override
      @NonNull
      public List<Quote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfBookCover = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCover");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSourceType = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceType");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final List<Quote> _result = new ArrayList<Quote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Quote _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final String _tmpBookCover;
            if (_cursor.isNull(_cursorIndexOfBookCover)) {
              _tmpBookCover = null;
            } else {
              _tmpBookCover = _cursor.getString(_cursorIndexOfBookCover);
            }
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toStringList(_tmp);
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpSourceType;
            if (_cursor.isNull(_cursorIndexOfSourceType)) {
              _tmpSourceType = null;
            } else {
              _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
            }
            final Long _tmpBookId;
            if (_cursor.isNull(_cursorIndexOfBookId)) {
              _tmpBookId = null;
            } else {
              _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            }
            _item = new Quote(_tmpId,_tmpText,_tmpTitle,_tmpAuthor,_tmpBookCover,_tmpTags,_tmpIsFavorite,_tmpUpdatedAt,_tmpSourceType,_tmpBookId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Quote>> getQuotesByTag(final String tag) {
    final String _sql = "SELECT * FROM quotes WHERE tags LIKE '%' || ? || '%' ORDER BY RANDOM()";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, tag);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quotes"}, new Callable<List<Quote>>() {
      @Override
      @NonNull
      public List<Quote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfBookCover = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCover");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSourceType = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceType");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final List<Quote> _result = new ArrayList<Quote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Quote _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final String _tmpBookCover;
            if (_cursor.isNull(_cursorIndexOfBookCover)) {
              _tmpBookCover = null;
            } else {
              _tmpBookCover = _cursor.getString(_cursorIndexOfBookCover);
            }
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toStringList(_tmp);
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpSourceType;
            if (_cursor.isNull(_cursorIndexOfSourceType)) {
              _tmpSourceType = null;
            } else {
              _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
            }
            final Long _tmpBookId;
            if (_cursor.isNull(_cursorIndexOfBookId)) {
              _tmpBookId = null;
            } else {
              _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            }
            _item = new Quote(_tmpId,_tmpText,_tmpTitle,_tmpAuthor,_tmpBookCover,_tmpTags,_tmpIsFavorite,_tmpUpdatedAt,_tmpSourceType,_tmpBookId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<Quote>> getRecentQuotes(final String since) {
    final String _sql = "SELECT * FROM quotes WHERE updatedAt >= ? ORDER BY RANDOM()";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, since);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"quotes"}, new Callable<List<Quote>>() {
      @Override
      @NonNull
      public List<Quote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfText = CursorUtil.getColumnIndexOrThrow(_cursor, "text");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAuthor = CursorUtil.getColumnIndexOrThrow(_cursor, "author");
          final int _cursorIndexOfBookCover = CursorUtil.getColumnIndexOrThrow(_cursor, "bookCover");
          final int _cursorIndexOfTags = CursorUtil.getColumnIndexOrThrow(_cursor, "tags");
          final int _cursorIndexOfIsFavorite = CursorUtil.getColumnIndexOrThrow(_cursor, "isFavorite");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfSourceType = CursorUtil.getColumnIndexOrThrow(_cursor, "sourceType");
          final int _cursorIndexOfBookId = CursorUtil.getColumnIndexOrThrow(_cursor, "bookId");
          final List<Quote> _result = new ArrayList<Quote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Quote _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpText;
            _tmpText = _cursor.getString(_cursorIndexOfText);
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final String _tmpAuthor;
            if (_cursor.isNull(_cursorIndexOfAuthor)) {
              _tmpAuthor = null;
            } else {
              _tmpAuthor = _cursor.getString(_cursorIndexOfAuthor);
            }
            final String _tmpBookCover;
            if (_cursor.isNull(_cursorIndexOfBookCover)) {
              _tmpBookCover = null;
            } else {
              _tmpBookCover = _cursor.getString(_cursorIndexOfBookCover);
            }
            final List<String> _tmpTags;
            final String _tmp;
            _tmp = _cursor.getString(_cursorIndexOfTags);
            _tmpTags = __converters.toStringList(_tmp);
            final boolean _tmpIsFavorite;
            final int _tmp_1;
            _tmp_1 = _cursor.getInt(_cursorIndexOfIsFavorite);
            _tmpIsFavorite = _tmp_1 != 0;
            final String _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getString(_cursorIndexOfUpdatedAt);
            final String _tmpSourceType;
            if (_cursor.isNull(_cursorIndexOfSourceType)) {
              _tmpSourceType = null;
            } else {
              _tmpSourceType = _cursor.getString(_cursorIndexOfSourceType);
            }
            final Long _tmpBookId;
            if (_cursor.isNull(_cursorIndexOfBookId)) {
              _tmpBookId = null;
            } else {
              _tmpBookId = _cursor.getLong(_cursorIndexOfBookId);
            }
            _item = new Quote(_tmpId,_tmpText,_tmpTitle,_tmpAuthor,_tmpBookCover,_tmpTags,_tmpIsFavorite,_tmpUpdatedAt,_tmpSourceType,_tmpBookId);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getAllTags(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT DISTINCT tags FROM quotes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getCount(final Continuation<? super Integer> $completion) {
    final String _sql = "SELECT COUNT(*) FROM quotes";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
