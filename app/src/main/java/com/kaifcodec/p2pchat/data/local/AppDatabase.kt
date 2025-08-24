package com.kaifcodec.p2pchat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.kaifcodec.p2pchat.data.local.dao.MessageDao
import com.kaifcodec.p2pchat.data.local.entities.Message
import com.kaifcodec.p2pchat.utils.Constants
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory
import java.security.SecureRandom

@Database(
    entities = [Message::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .openHelperFactory(getSupportFactory(context))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private fun getSupportFactory(context: Context): SupportFactory {
            val passphrase = getOrCreatePassphrase(context)
            return SupportFactory(SQLiteDatabase.getBytes(passphrase.toCharArray()))
        }

        private fun getOrCreatePassphrase(context: Context): String {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val sharedPreferences = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            return sharedPreferences.getString(Constants.DATABASE_PASSPHRASE_KEY, null)
                ?: generateAndStorePassphrase(sharedPreferences)
        }

        private fun generateAndStorePassphrase(sharedPreferences: android.content.SharedPreferences): String {
            val random = SecureRandom()
            val passphrase = ByteArray(32)
            random.nextBytes(passphrase)
            val passphraseString = android.util.Base64.encodeToString(passphrase, android.util.Base64.NO_WRAP)

            sharedPreferences.edit()
                .putString(Constants.DATABASE_PASSPHRASE_KEY, passphraseString)
                .apply()

            return passphraseString
        }
    }
}
