package btm.m.edgeflow.data

import androidx.room.Database
import androidx.room.RoomDatabase
import btm.m.edgeflow.data.dao.CustomLinkDao
import btm.m.edgeflow.data.dao.SelectedAppDao
import btm.m.edgeflow.data.entity.CustomLink
import btm.m.edgeflow.data.entity.SelectedApp

@Database(entities = [SelectedApp::class, CustomLink::class], version = 4, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun selectedAppDao(): SelectedAppDao
    abstract fun customLinkDao(): CustomLinkDao
}
