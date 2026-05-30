package btm.m.edgeflow.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import btm.m.edgeflow.data.entity.SelectedApp
import kotlinx.coroutines.flow.Flow

@Dao
interface SelectedAppDao {

    @Query("SELECT * FROM selected_apps ORDER BY sortOrder ASC")
    fun getAll(): Flow<List<SelectedApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(app: SelectedApp)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(apps: List<SelectedApp>)

    @Delete
    suspend fun delete(app: SelectedApp)

    @Query("DELETE FROM selected_apps WHERE packageName = :packageName")
    suspend fun deleteByPackageName(packageName: String)

    @Query("SELECT COUNT(*) FROM selected_apps")
    suspend fun count(): Int
}