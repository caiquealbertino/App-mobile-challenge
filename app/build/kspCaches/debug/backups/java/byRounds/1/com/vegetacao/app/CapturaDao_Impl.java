package com.vegetacao.app;

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
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
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
public final class CapturaDao_Impl implements CapturaDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Captura> __insertionAdapterOfCaptura;

  private final SharedSQLiteStatement __preparedStmtOfMarcarEnviada;

  public CapturaDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCaptura = new EntityInsertionAdapter<Captura>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `capturas` (`id`,`corrida`,`caminho`,`lat`,`lng`,`timestamp`,`enviada`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Captura entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getCorrida());
        statement.bindString(3, entity.getCaminho());
        statement.bindDouble(4, entity.getLat());
        statement.bindDouble(5, entity.getLng());
        statement.bindLong(6, entity.getTimestamp());
        final int _tmp = entity.getEnviada() ? 1 : 0;
        statement.bindLong(7, _tmp);
      }
    };
    this.__preparedStmtOfMarcarEnviada = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE capturas SET enviada = 1 WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object inserir(final Captura c, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCaptura.insert(c);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object marcarEnviada(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfMarcarEnviada.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfMarcarEnviada.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object pendentes(final int n, final Continuation<? super List<Captura>> $completion) {
    final String _sql = "SELECT * FROM capturas WHERE enviada = 0 ORDER BY id LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, n);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Captura>>() {
      @Override
      @NonNull
      public List<Captura> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfCorrida = CursorUtil.getColumnIndexOrThrow(_cursor, "corrida");
          final int _cursorIndexOfCaminho = CursorUtil.getColumnIndexOrThrow(_cursor, "caminho");
          final int _cursorIndexOfLat = CursorUtil.getColumnIndexOrThrow(_cursor, "lat");
          final int _cursorIndexOfLng = CursorUtil.getColumnIndexOrThrow(_cursor, "lng");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfEnviada = CursorUtil.getColumnIndexOrThrow(_cursor, "enviada");
          final List<Captura> _result = new ArrayList<Captura>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Captura _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpCorrida;
            _tmpCorrida = _cursor.getString(_cursorIndexOfCorrida);
            final String _tmpCaminho;
            _tmpCaminho = _cursor.getString(_cursorIndexOfCaminho);
            final double _tmpLat;
            _tmpLat = _cursor.getDouble(_cursorIndexOfLat);
            final double _tmpLng;
            _tmpLng = _cursor.getDouble(_cursorIndexOfLng);
            final long _tmpTimestamp;
            _tmpTimestamp = _cursor.getLong(_cursorIndexOfTimestamp);
            final boolean _tmpEnviada;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfEnviada);
            _tmpEnviada = _tmp != 0;
            _item = new Captura(_tmpId,_tmpCorrida,_tmpCaminho,_tmpLat,_tmpLng,_tmpTimestamp,_tmpEnviada);
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
  public Flow<Integer> contarPendentes() {
    final String _sql = "SELECT COUNT(*) FROM capturas WHERE enviada = 0";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"capturas"}, new Callable<Integer>() {
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
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
