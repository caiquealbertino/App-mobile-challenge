package com.vegetacao.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "capturas")
data class Captura(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val corrida: String,
    val caminho: String,
    val lat: Double,
    val lng: Double,
    val timestamp: Long,
    val enviada: Boolean = false
)

@Dao
interface CapturaDao {
    @Insert suspend fun inserir(c: Captura)
    @Query("SELECT * FROM capturas WHERE enviada = 0 ORDER BY id LIMIT :n") suspend fun pendentes(n: Int): List<Captura>
    @Query("UPDATE capturas SET enviada = 1 WHERE id = :id") suspend fun marcarEnviada(id: Long)
    @Query("SELECT COUNT(*) FROM capturas WHERE enviada = 0") fun contarPendentes(): Flow<Int>
}

@Database(entities = [Captura::class], version = 1, exportSchema = false)
abstract class Banco : RoomDatabase() {
    abstract fun dao(): CapturaDao

    companion object {
        @Volatile private var inst: Banco? = null
        fun get(c: Context): Banco = inst ?: synchronized(this) {
            inst ?: Room.databaseBuilder(c.applicationContext, Banco::class.java, "vegetacao.db").build().also { inst = it }
        }
    }
}
